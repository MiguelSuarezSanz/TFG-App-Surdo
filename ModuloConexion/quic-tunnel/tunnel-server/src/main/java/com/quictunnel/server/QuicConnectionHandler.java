package com.quictunnel.server;

import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Handler de Netty que gestiona el ciclo de vida de una conexión.
 * Se ejecuta cuando un móvil nuevo completa el handshake mTLS
 * y cuando se desconecta.
 *
 * Por cada móvil que se conecta, Netty crea una instancia nueva
 * de este handler, por lo que cada instancia gestiona exactamente
 * una conexión.
 */
public class QuicConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(QuicConnectionHandler.class);

    private final ServerConnectionManager connectionManager;
    private final TunnelListener listener;
    private final Executor callbackExecutor;

    /**
     * La conexión asociada a este handler.
     * Se crea cuando el canal se activa (handshake completado).
     */
    private NettyTunnelConnection connection;

    /**
     * @param connectionManager El gestor de conexiones activas.
     * @param listener          El listener del juego.
     * @param callbackExecutor  El executor para los callbacks.
     */
    public QuicConnectionHandler(
            ServerConnectionManager connectionManager,
            TunnelListener listener,
            Executor callbackExecutor) {
        this.connectionManager = connectionManager;
        this.listener = listener;
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * Llamado por Netty cuando el canal se activa.
     * Esto ocurre justo después de que el handshake mTLS se completa
     * y la conexión queda establecida.
     *
     * Aquí creamos la NettyTunnelConnection, la registramos en el
     * manager y notificamos al listener del juego.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();

        // Generamos un ID único para esta conexión
        // UUID garantiza que no habrá colisiones aunque haya muchos jugadores
        String connectionId = UUID.randomUUID().toString();

        // Creamos la conexión y la registramos
        connection = new NettyTunnelConnection(channel, connectionId);
        connectionManager.add(connection);

        log.info("Nueva conexión establecida: {}", connectionId);

        // Añadimos el handler de datagramas al pipeline de Netty
        // A partir de aquí, los datagramas de este canal los procesará
        // QuicDatagramHandler
        ctx.pipeline().addLast(new QuicDatagramHandler(
                connection,
                listener,
                callbackExecutor
        ));

        // Notificamos al juego en el executor configurado
        callbackExecutor.execute(() ->
                listener.onConnected(connection)
        );
    }

    /**
     * Llamado por Netty cuando el canal se desactiva.
     * Esto ocurre cuando el móvil se desconecta.
     *
     * Eliminamos la conexión del manager y notificamos al listener.
     * Nota: QuicDatagramHandler también tiene channelInactive, pero
     * este handler se ejecuta primero al estar antes en el pipeline.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (connection != null) {
            connectionManager.remove(connection.getId());
            log.info("Conexión cerrada: {}", connection.getId());
        }
    }

    /**
     * Llamado por Netty cuando ocurre una excepción antes de que
     * el canal esté completamente activo, por ejemplo durante el
     * handshake mTLS.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Excepción en handshake o conexión: {}", cause.getMessage());

        TunnelError error = new TunnelError(
                TunnelError.Type.HANDSHAKE_FAILED,
                "Error durante el handshake mTLS: " + cause.getMessage(),
                cause
        );

        // connection puede ser null si el error ocurre antes de channelActive
        // TunnelListener.onError acepta connection null para este caso
        callbackExecutor.execute(() ->
                listener.onError(connection, error)
        );

        ctx.close();
    }
}