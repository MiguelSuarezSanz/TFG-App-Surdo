package com.quictunnel.client;

import com.quictunnel.client.jni.QuicheWrapper;
import com.quictunnel.core.QuicTunnel;
import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelException;
import com.quictunnel.core.TunnelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementación del túnel para el lado del cliente Android.
 * Es el único punto de entrada que la app Android necesita conocer
 * de este módulo.
 *
 * Uso:
 *   TunnelConfig config = TunnelConfig.builder()
 *       .host("192.168.1.10")
 *       .port(4242)
 *       .caCert("ca.crt")
 *       .cert("client.crt")
 *       .key("client.key")
 *       .build();
 *
 *   QuicTunnel tunnel = new QuicTunnelClient(config);
 *   tunnel.setListener(listener);
 *   tunnel.connect();
 *
 *   tunnel.send(new byte[]{ ... });
 */
public class QuicTunnelClient implements QuicTunnel {

    private static final Logger log = LoggerFactory.getLogger(QuicTunnelClient.class);

    private final TunnelConfig config;
    private final AtomicReference<TunnelConnection.State> state;

    /**
     * El listener que la app Android registra para recibir eventos.
     * volatile para que sea visible desde cualquier hilo.
     */
    private volatile TunnelListener listener;

    /**
     * Gestiona la conexión, recepción y reconexión internamente.
     * La app Android nunca lo ve.
     */
    private QuicClientConnection clientConnection;

    /**
     * El wrapper JNI de quiche.
     * Se instancia aquí y se pasa a QuicClientConnection.
     */
    private final QuicheWrapper quiche;

    public QuicTunnelClient(TunnelConfig config) {
        this.config = config;
        this.state = new AtomicReference<>(TunnelConnection.State.IDLE);
        this.quiche = new QuicheWrapper();
        QuicheWrapper.loadNativeLibrary(); // carga el .so solo en producción
    }

    /**
     * Constructor para pruebas. Permite inyectar un QuicheWrapper
     * personalizado (por ejemplo un mock) en lugar del real.
     */
    QuicTunnelClient(TunnelConfig config, QuicheWrapper quiche) {
        this.config = config;
        this.state = new AtomicReference<>(TunnelConnection.State.IDLE);
        this.quiche = quiche;
    }

    @Override
    public void setListener(TunnelListener listener) {
        this.listener = listener;
    }

    /**
     * Conecta al servidor.
     * Debe llamarse después de setListener().
     *
     * La conexión es explícita: el túnel no conecta automáticamente
     * al instanciarse. Esto permite obtener la IP del servidor
     * por cualquier medio (QR, manual...) antes de conectar.
     *
     * @throws TunnelException si el listener no está registrado,
     *                         si ya está conectado,
     *                         o si la conexión inicial falla.
     */
    @Override
    public void connect() throws TunnelException {
        if (listener == null) {
            throw new TunnelException(
                    TunnelError.Type.CONFIGURATION_ERROR,
                    "Debes registrar un listener antes de llamar a connect()"
            );
        }

        if (!state.compareAndSet(
                TunnelConnection.State.IDLE,
                TunnelConnection.State.CONNECTING)) {
            throw new TunnelException(
                    TunnelError.Type.CONFIGURATION_ERROR,
                    "El cliente ya está conectado o conectando (estado: " + state.get() + ")"
            );
        }

        try {
            // Envolvemos el listener para actualizar el estado del túnel
            // cuando ocurren eventos de conexión/desconexión
            TunnelListener wrappedListener = wrapListener(listener);

            clientConnection = new QuicClientConnection(
                    config,
                    wrappedListener,
                    quiche
            );

            clientConnection.start();
            state.set(TunnelConnection.State.CONNECTED);
            log.info("QuicTunnelClient conectado a {}:{}", config.getHost(), config.getPort());

        } catch (TunnelException e) {
            state.set(TunnelConnection.State.ERROR);
            throw e;
        }
    }

    /**
     * No tiene sentido en el cliente, que inicia conexiones salientes.
     * El cliente usa connect() en su lugar.
     */
    @Override
    public void start() throws TunnelException {
        throw new TunnelException(
                TunnelError.Type.CONFIGURATION_ERROR,
                "El cliente no usa start(). Usa connect() en su lugar."
        );
    }

    /**
     * Envía un payload al servidor.
     * Thread-safe: puede llamarse desde cualquier hilo.
     *
     * @param payload Los bytes a enviar.
     * @throws TunnelException si no hay conexión activa o el envío falla.
     */
    public void send(byte[] payload) throws TunnelException {
        if (clientConnection == null) {
            throw new TunnelException(
                    TunnelError.Type.CONNECTION_LOST,
                    "No hay conexión activa. Llama a connect() primero."
            );
        }
        clientConnection.send(payload);
    }

    /**
     * Detiene el cliente de forma ordenada.
     * Cancela reconexiones pendientes y cierra la conexión.
     */
    @Override
    public void stop() {
        log.info("Deteniendo QuicTunnelClient...");

        if (clientConnection != null) {
            clientConnection.stop();
        }

        state.set(TunnelConnection.State.DISCONNECTED);
        log.info("QuicTunnelClient detenido.");
    }

    @Override
    public TunnelConnection.State getState() {
        return state.get();
    }

    /**
     * Envuelve el listener del juego para que el túnel pueda
     * actualizar su estado interno cuando ocurren eventos.
     *
     * Por ejemplo cuando onDisconnected se llama, el túnel
     * actualiza su estado a DISCONNECTED antes de notificar al juego.
     *
     * @param original El listener original del juego.
     * @return Un listener que actualiza el estado y luego delega al original.
     */
    private TunnelListener wrapListener(TunnelListener original) {
        return new TunnelListener() {

            @Override
            public void onConnected(TunnelConnection connection) {
                state.set(TunnelConnection.State.CONNECTED);
                original.onConnected(connection);
            }

            @Override
            public void onDataReceived(TunnelConnection connection, byte[] payload) {
                original.onDataReceived(connection, payload);
            }

            @Override
            public void onDisconnected(TunnelConnection connection) {
                state.set(TunnelConnection.State.DISCONNECTED);
                original.onDisconnected(connection);
            }

            @Override
            public void onError(TunnelConnection connection, TunnelError error) {
                if (error.getType() == TunnelError.Type.MAX_RECONNECT_ATTEMPTS_REACHED) {
                    state.set(TunnelConnection.State.ERROR);
                }
                original.onError(connection, error);
            }
        };
    }
}