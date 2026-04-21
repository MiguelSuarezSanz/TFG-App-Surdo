package com.quictunnel.server;

import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

/**
 * Handler de Netty que procesa los datagramas entrantes de un móvil.
 *
 * Netty llama a channelRead0() cada vez que llega un datagrama.
 * Este handler:
 *   1. Lee la cabecera [version | length]
 *   2. Verifica que length coincide con los bytes recibidos
 *   3. Extrae el payload y lo entrega al listener del juego
 *
 * El payload se entrega intacto, el túnel no lo interpreta.
 */
public class QuicDatagramHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(QuicDatagramHandler.class);

    /**
     * Tamaño mínimo de un datagrama válido.
     * 1 byte version + 2 bytes length = 3 bytes de cabecera mínima.
     */
    private static final int HEADER_SIZE = 3;

    private final NettyTunnelConnection connection;
    private final TunnelListener listener;
    private final Executor callbackExecutor;

    /**
     * @param connection       La conexión asociada a este handler.
     * @param listener         El listener del juego al que entregar los datos.
     * @param callbackExecutor El executor en el que ejecutar los callbacks del listener.
     */
    public QuicDatagramHandler(
            NettyTunnelConnection connection,
            TunnelListener listener,
            Executor callbackExecutor) {
        this.connection = connection;
        this.listener = listener;
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * Llamado por Netty cada vez que llega un datagrama completo.
     *
     * @param ctx El contexto del canal Netty.
     * @param msg El datagrama recibido como ByteBuf.
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        // Verificamos que hay al menos los 3 bytes de cabecera
        if (msg.readableBytes() < HEADER_SIZE) {
            log.warn("Datagrama demasiado corto ({} bytes) de {}. Descartando.",
                    msg.readableBytes(), connection.getId());
            notifyError(TunnelError.Type.INVALID_DATAGRAM,
                    "Datagrama demasiado corto: " + msg.readableBytes() + " bytes");
            return;
        }

        // Leemos la cabecera
        // version: 1 byte — lo leemos pero no lo validamos, el juego decide qué hacer
        byte version = msg.readByte();

        // length: 2 bytes — tamaño esperado del payload
        int declaredLength = msg.readUnsignedShort();

        // Verificamos que length coincide con los bytes reales del payload
        int actualLength = msg.readableBytes();
        if (declaredLength != actualLength) {
            log.warn("Datagrama de {}: length declarado ({}) != bytes reales ({}). Descartando.",
                    connection.getId(), declaredLength, actualLength);
            notifyError(TunnelError.Type.INVALID_DATAGRAM,
                    "Length incorrecto: declarado=" + declaredLength + " real=" + actualLength);
            return;
        }

        // Extraemos el payload como array de bytes
        byte[] payload = new byte[actualLength];
        msg.readBytes(payload);

        log.debug("Datagrama recibido de {}: version={}, {} bytes de payload",
                connection.getId(), version, payload.length);

        // Entregamos el payload al listener en el executor configurado
        // Así no bloqueamos el hilo de red de Netty
        callbackExecutor.execute(() ->
                listener.onDataReceived(connection, payload)
        );
    }

    /**
     * Llamado por Netty cuando la conexión se cierra.
     * Notifica al listener y actualiza el estado de la conexión.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Canal inactivo para conexión: {}", connection.getId());
        connection.setState(TunnelConnection.State.DISCONNECTED);
        callbackExecutor.execute(() ->
                listener.onDisconnected(connection)
        );
    }

    /**
     * Llamado por Netty cuando ocurre una excepción en el pipeline.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Excepción en canal de {}: {}", connection.getId(), cause.getMessage());
        notifyError(TunnelError.Type.CONNECTION_LOST, cause.getMessage(), cause);
        ctx.close();
    }

    /**
     * Notifica un error al listener en el executor configurado.
     */
    private void notifyError(TunnelError.Type type, String message) {
        notifyError(type, message, null);
    }

    private void notifyError(TunnelError.Type type, String message, Throwable cause) {
        TunnelError error = new TunnelError(type, message, cause);
        callbackExecutor.execute(() ->
                listener.onError(connection, error)
        );
    }
}