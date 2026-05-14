package com.quictunnel.server;

import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Configura y arranca el servidor Netty QUIC.
 *
 * Esta clase es interna del túnel. Quien usa el túnel (el juego)
 * nunca la ve ni la toca. Toda la complejidad de Netty vive aquí.
 *
 * El flujo de arranque es:
 *   1. Cargar los certificados para mTLS
 *   2. Crear el contexto SSL con los certificados
 *   3. Configurar el codec QUIC con ese contexto SSL
 *   4. Arrancar el servidor UDP en el puerto configurado
 *   5. Empezar a aceptar conexiones entrantes
 */
public class NettyServerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(NettyServerBootstrap.class);

    private final TunnelConfig config;
    private final ServerConnectionManager connectionManager;
    private final TunnelListener listener;
    private final Executor callbackExecutor;

    /**
     * El grupo de hilos de Netty que gestiona los eventos de red.
     * NioEventLoopGroup usa Java NIO (non-blocking I/O).
     * Se guarda para poder cerrarlo cuando el servidor se detenga.
     */
    private NioEventLoopGroup group;

    /**
     * El canal raíz del servidor UDP.
     * Se guarda para poder cerrarlo cuando el servidor se detenga.
     */
    private Channel serverChannel;
    private QuicSslContext sslContext;

    /**
     * Constructor alternativo para pruebas.
     * Acepta un QuicSslContext ya construido en lugar de cargar certificados
     * desde ficheros, útil cuando los certificados se generan en memoria.
     */
    public NettyServerBootstrap(
            QuicSslContext sslContext,
            TunnelConfig config,
            ServerConnectionManager connectionManager,
            TunnelListener listener,
            Executor callbackExecutor) {
        this.sslContext = sslContext;
        this.config = config;
        this.connectionManager = connectionManager;
        this.listener = listener;
        this.callbackExecutor = callbackExecutor;
    }

    public NettyServerBootstrap(
            TunnelConfig config,
            ServerConnectionManager connectionManager,
            TunnelListener listener,
            Executor callbackExecutor) {
        this.sslContext = null;
        this.config = config;
        this.connectionManager = connectionManager;
        this.listener = listener;
        this.callbackExecutor = callbackExecutor;
    }
    /**
     * Arranca el servidor QUIC.
     * Carga los certificados, configura Netty y empieza a escuchar.
     *
     * @throws Exception si falla la carga de certificados o el arranque.
     */
    public void start() throws Exception {
        log.info("Arrancando servidor QUIC en puerto {}...", config.getPort());

        // Si no se inyectó un sslContext (caso de pruebas),
        // lo construimos desde los ficheros de configuración
        if (this.sslContext == null) {
            this.sslContext = QuicSslContextBuilder
                    .forServer(
                            new File(config.getKeyPath()),
                            null,
                            new File(config.getCertPath())
                    )
                    .clientAuth(io.netty.handler.ssl.ClientAuth.REQUIRE)
                    .trustManager(new File(config.getCaCertPath()))
                    .applicationProtocols("quic-tunnel")
                    .build();
        }

        QuicChannelInitializer quicChannelInitializer = new QuicChannelInitializer(
                connectionManager,
                listener,
                callbackExecutor
        );

        io.netty.channel.ChannelHandler quicCodec = new QuicServerCodecBuilder()
                .sslContext(this.sslContext)
                .maxIdleTimeout(config.getKeepaliveIntervalMs() * 2L, TimeUnit.MILLISECONDS)
                .maxRecvUdpPayloadSize(65535)
                .maxSendUdpPayloadSize(65535)
                .datagram(65535, 65535)
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                .handler(quicChannelInitializer)
                .build();

        group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .handler(quicCodec);

        serverChannel = bootstrap
                .bind(new InetSocketAddress(config.getPort()))
                .sync()
                .channel();

        log.info("Servidor QUIC escuchando en puerto {}", config.getPort());
    }

    /**
     * Detiene el servidor de forma ordenada.
     * Cierra el canal UDP y libera los hilos de Netty.
     */
    public void stop() {
        log.info("Deteniendo servidor QUIC...");

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (group != null) {
            // shutdownGracefully espera a que terminen las operaciones
            // en curso antes de cerrar los hilos
            group.shutdownGracefully();
        }

        log.info("Servidor QUIC detenido.");
    }
}