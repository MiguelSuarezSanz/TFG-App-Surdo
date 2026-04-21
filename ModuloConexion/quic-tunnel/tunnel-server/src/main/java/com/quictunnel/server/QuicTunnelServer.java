package com.quictunnel.server;

import com.quictunnel.core.QuicTunnel;
import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelException;
import com.quictunnel.core.TunnelListener;
import io.netty.incubator.codec.quic.QuicSslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementación del túnel para el lado del servidor.
 * Es el único punto de entrada que el juego necesita conocer
 * de este módulo.
 *
 * Uso:
 *   TunnelConfig config = TunnelConfig.builder()
 *       .port(4242)
 *       .caCert("ca.crt")
 *       .cert("server.crt")
 *       .key("server.key")
 *       .build();
 *
 *   QuicTunnel tunnel = new QuicTunnelServer(config);
 *   tunnel.setListener(listener);
 *   tunnel.start();
 */
public class QuicTunnelServer implements QuicTunnel {

    private static final Logger log = LoggerFactory.getLogger(QuicTunnelServer.class);

    private final TunnelConfig config;
    private final ServerConnectionManager connectionManager;
    private final AtomicReference<TunnelConnection.State> state;

    /**
     * El listener que el juego registra para recibir eventos.
     * Se guarda como volatile para que sea visible desde cualquier hilo.
     */
    private volatile TunnelListener listener;

    /**
     * El bootstrap que gestiona Netty internamente.
     * El juego nunca lo ve.
     */
    private NettyServerBootstrap bootstrap;

    public QuicTunnelServer(TunnelConfig config) {
        this.config = config;
        this.connectionManager = new ServerConnectionManager();
        this.state = new AtomicReference<>(TunnelConnection.State.IDLE);
    }
    private QuicSslContext testSslContext = null;

    /**
     * Constructor para pruebas con SSL context pre-construido.
     */
    public QuicTunnelServer(TunnelConfig config, QuicSslContext testSslContext) {
        this(config);
        this.testSslContext = testSslContext;
    }

    @Override
    public void setListener(TunnelListener listener) {
        this.listener = listener;
    }

    /**
     * Arranca el servidor y empieza a aceptar conexiones.
     * Debe llamarse después de setListener().
     *
     * @throws TunnelException si el listener no está registrado,
     *                         si el servidor ya está corriendo,
     *                         o si falla el arranque de Netty.
     */
    @Override
    public void start() throws TunnelException {
        if (listener == null) {
            throw new TunnelException(
                    TunnelError.Type.CONFIGURATION_ERROR,
                    "Debes registrar un listener antes de llamar a start()"
            );
        }

        if (!state.compareAndSet(
                TunnelConnection.State.IDLE,
                TunnelConnection.State.CONNECTING)) {
            throw new TunnelException(
                    TunnelError.Type.CONFIGURATION_ERROR,
                    "El servidor ya está arrancado o en proceso de arranque (estado: " + state.get() + ")"
            );
        }

        try {
            bootstrap = testSslContext != null
                    ? new NettyServerBootstrap(testSslContext, config, connectionManager, listener, config.getCallbackExecutor())
                    : new NettyServerBootstrap(config, connectionManager, listener, config.getCallbackExecutor());

            bootstrap.start();
            state.set(TunnelConnection.State.CONNECTED);
            log.info("QuicTunnelServer arrancado correctamente en puerto {}", config.getPort());

        } catch (Exception e) {
            state.set(TunnelConnection.State.ERROR);
            throw new TunnelException(
                    TunnelError.Type.CONNECTION_FAILED,
                    "Error al arrancar el servidor: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * No tiene sentido en el servidor, que escucha conexiones entrantes.
     * El servidor usa start() en su lugar.
     */
    @Override
    public void connect() throws TunnelException {
        throw new TunnelException(
                TunnelError.Type.CONFIGURATION_ERROR,
                "El servidor no usa connect(). Usa start() en su lugar."
        );
    }

    /**
     * Detiene el servidor de forma ordenada.
     * Cierra todas las conexiones activas y libera los recursos de Netty.
     */
    @Override
    public void stop() {
        log.info("Deteniendo QuicTunnelServer...");

        connectionManager.closeAll();

        if (bootstrap != null) {
            bootstrap.stop();
        }

        state.set(TunnelConnection.State.DISCONNECTED);
        log.info("QuicTunnelServer detenido.");
    }

    @Override
    public TunnelConnection.State getState() {
        return state.get();
    }

    /**
     * Devuelve el gestor de conexiones activas.
     * El juego puede usarlo para enviar datos a jugadores concretos
     * o hacer broadcast a todos.
     *
     * Ejemplo:
     *   server.getConnectionManager().getAll().forEach(conn -> {
     *       conn.send(data);
     *   });
     */
    public ServerConnectionManager getConnectionManager() {
        return connectionManager;
    }
}