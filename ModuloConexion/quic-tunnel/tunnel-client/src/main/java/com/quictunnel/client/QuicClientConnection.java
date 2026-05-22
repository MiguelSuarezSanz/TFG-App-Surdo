package com.quictunnel.client;

import com.quictunnel.client.jni.QuicheWrapper;
import com.quictunnel.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gestiona la conexión QUIC del cliente con el servidor.
 *
 * Responsabilidades:
 *   - Establecer la conexión inicial con quiche
 *   - Mantener un hilo de recepción que escucha datagramas entrantes
 *   - Detectar caídas de conexión y disparar la reconexión automática
 *   - Notificar al listener del juego en el executor configurado
 *
 * Es el equivalente del lado cliente a NettyServerBootstrap +
 * QuicConnectionHandler + QuicDatagramHandler juntos.
 */
public class QuicClientConnection {

    private static final Logger log = LoggerFactory.getLogger(QuicClientConnection.class);

    /**
     * Timeout de recepción en milisegundos.
     * El hilo de recepción espera este tiempo antes de comprobar
     * si debe seguir corriendo o si la conexión se cayó.
     */
    private static final int RECEIVE_TIMEOUT_MS = 1000;

    private final TunnelConfig config;
    private final TunnelListener listener;
    private final QuicheWrapper quiche;
    private final ClientReconnectPolicy reconnectPolicy;
    private final AtomicReference<QuicheTunnelConnection> activeConnection;

    /**
     * Hilo dedicado a recibir datagramas del servidor.
     * Corre en bucle mientras la conexión está activa.
     */
    private ExecutorService receiveExecutor;

    /**
     * Hilo dedicado a gestionar la reconexión automática.
     */
    private ExecutorService reconnectExecutor;

    /**
     * Indica si el cliente está corriendo.
     * Se pone a false cuando se llama a stop().
     */
    private volatile boolean running;

    public QuicClientConnection(
            TunnelConfig config,
            TunnelListener listener,
            QuicheWrapper quiche) {
        this.config = config;
        this.listener = listener;
        this.quiche = quiche;
        this.reconnectPolicy = new ClientReconnectPolicy(config);
        this.activeConnection = new AtomicReference<>(null);
        this.running = false;
    }

    /**
     * Inicia la conexión con el servidor.
     * Arranca el hilo de recepción y el de reconexión.
     *
     * @throws TunnelException si la conexión inicial falla.
     */
    public void start() throws TunnelException {
        running = true;
        receiveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "quic-receive");
            t.setDaemon(true);
            return t;
        });
        reconnectExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "quic-reconnect");
            t.setDaemon(true);
            return t;
        });

        // Intento de conexión inicial
        QuicheTunnelConnection connection = attemptConnect();
        activeConnection.set(connection);
        reconnectPolicy.reset();

        config.getCallbackExecutor().execute(() ->
                listener.onConnected(connection)
        );

        // Arrancamos el hilo de recepción
        receiveExecutor.execute(this::receiveLoop);

        log.info("QuicClientConnection iniciada.");
    }

    /**
     * Detiene la conexión y cancela cualquier reconexión pendiente.
     */
    public void stop() {
        log.info("Deteniendo QuicClientConnection...");

        running = false;
        reconnectPolicy.disable();

        QuicheTunnelConnection connection =
                activeConnection.getAndSet(null);

        if (connection != null) {

            connection.setState(
                    TunnelConnection.State.DISCONNECTED
            );

            connection.close();

            config.getCallbackExecutor().execute(() ->
                    listener.onDisconnected(connection)
            );
        }

        if (receiveExecutor != null) {
            receiveExecutor.shutdownNow();
        }

        if (reconnectExecutor != null) {
            reconnectExecutor.shutdownNow();
        }

        log.info("QuicClientConnection detenida.");
    }

    /**
     * Envía un payload al servidor a través de la conexión activa.
     *
     * @param payload Los bytes a enviar.
     * @throws TunnelException si no hay conexión activa o el envío falla.
     */
    public void send(byte[] payload) throws TunnelException {
        QuicheTunnelConnection connection = activeConnection.get();
        if (connection == null) {
            throw new TunnelException(
                    TunnelError.Type.CONNECTION_LOST,
                    "No hay conexión activa con el servidor"
            );
        }
        connection.send(payload);
    }

    /**
     * Devuelve el estado actual de la conexión activa,
     * o IDLE si no hay conexión.
     */
    public com.quictunnel.core.TunnelConnection.State getState() {
        QuicheTunnelConnection connection = activeConnection.get();
        if (connection == null) {
            return com.quictunnel.core.TunnelConnection.State.IDLE;
        }
        return connection.getState();
    }

    // ─────────────────────────────────────────────
    // Internos
    // ─────────────────────────────────────────────

    /**
     * Intenta establecer una conexión con el servidor.
     * Usa los certificados configurados en TunnelConfig para mTLS.
     *
     * @return La conexión establecida.
     * @throws TunnelException si quiche no puede conectar.
     */
    private QuicheTunnelConnection attemptConnect() throws TunnelException {
        log.info("Conectando a {}:{}...", config.getHost(), config.getPort());

        long handle = quiche.connect(
                config.getHost(),
                config.getPort(),
                config.getServerName(),
                config.getCaCertPath(),
                config.getCertPath(),
                config.getKeyPath()
        );

        if (handle == -1) {
            String error = quiche.getLastError(-1);
            throw new TunnelException(
                    TunnelError.Type.CONNECTION_FAILED,
                    "quiche no pudo conectar: " + error
            );
        }

        String id = UUID.randomUUID().toString();
        return new QuicheTunnelConnection(quiche, handle, id);
    }

    /**
     * Bucle de recepción que corre en su propio hilo.
     * Escucha datagramas del servidor y los entrega al listener.
     * Cuando detecta una caída, dispara la reconexión automática.
     */
    private void receiveLoop() {
        log.debug("Hilo de recepción iniciado.");

        while (running) {
            QuicheTunnelConnection connection = activeConnection.get();
            if (connection == null) {
                continue;
            }

            try {
                byte[] payload = connection.receive(RECEIVE_TIMEOUT_MS);

                // Timeout: no llegó nada, seguimos esperando
                if (payload == null) {
                    // Verificamos si la conexión sigue activa según quiche
                    if (!quiche.isConnected(connection.getConnHandle())) {
                        handleConnectionLost(connection);
                    }
                    continue;
                }

                // Entregamos el payload al listener en el executor del juego
                final byte[] finalPayload = payload;
                config.getCallbackExecutor().execute(() ->
                        listener.onDataReceived(connection, finalPayload)
                );

            } catch (TunnelException e) {
                log.warn("Error recibiendo datagrama: {}", e.getMessage());
                TunnelError error = new TunnelError(e.getErrorType(), e.getMessage(), e);
                config.getCallbackExecutor().execute(() ->
                        listener.onError(connection, error)
                );

                // Si el datagrama era inválido seguimos, no cerramos la conexión
                if (e.getErrorType() != TunnelError.Type.INVALID_DATAGRAM) {
                    handleConnectionLost(connection);
                }
            }
        }

        log.debug("Hilo de recepción terminado.");
    }

    /**
     * Gestiona la pérdida de conexión.
     * Notifica al listener y dispara la reconexión en su propio hilo.
     *
     * @param connection La conexión que se cayó.
     */
    private void handleConnectionLost(QuicheTunnelConnection connection) {
        if (connection.getState() == com.quictunnel.core.TunnelConnection.State.DISCONNECTED) {
            return; // ya se está gestionando
        }

        log.warn("Conexión perdida: {}", connection.getId());
        connection.setState(com.quictunnel.core.TunnelConnection.State.DISCONNECTED);
        activeConnection.set(null);

        config.getCallbackExecutor().execute(() ->
                listener.onDisconnected(connection)
        );

        // Disparamos la reconexión en su propio hilo para no bloquear
        // el hilo de recepción
        reconnectExecutor.execute(() -> reconnectLoop(connection));
    }

    /**
     * Bucle de reconexión con backoff exponencial.
     * Intenta reconectar hasta que lo consigue o se agotan los intentos.
     *
     * @param previousConnection La conexión que se cayó, para notificar errores.
     */
    private void reconnectLoop(QuicheTunnelConnection previousConnection) {
        while (running && reconnectPolicy.shouldReconnect()) {
            try {
                reconnectPolicy.waitBeforeNextAttempt();

                if (!running) break;

                log.info("Intentando reconectar... (intento {})",
                        reconnectPolicy.getAttempts());

                QuicheTunnelConnection newConnection = attemptConnect();
                activeConnection.set(newConnection);
                reconnectPolicy.reset();

                log.info("Reconexión exitosa: {}", newConnection.getId());

                config.getCallbackExecutor().execute(() ->
                        listener.onConnected(newConnection)
                );

                return; // reconexión exitosa, salimos del bucle

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (TunnelException e) {
                log.warn("Intento de reconexión fallido: {}", e.getMessage());
            }
        }

        // Si llegamos aquí es porque se agotaron los intentos o se llamó stop()
        if (running) {
            log.error("Se agotaron los intentos de reconexión.");
            TunnelError error = new TunnelError(
                    TunnelError.Type.MAX_RECONNECT_ATTEMPTS_REACHED,
                    "No se pudo reconectar tras " + reconnectPolicy.getAttempts() + " intentos"
            );
            config.getCallbackExecutor().execute(() ->
                    listener.onError(previousConnection, error)
            );
        }
    }
}