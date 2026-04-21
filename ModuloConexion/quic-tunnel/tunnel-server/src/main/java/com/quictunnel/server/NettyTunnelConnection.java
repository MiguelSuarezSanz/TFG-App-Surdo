package com.quictunnel.server;

import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementación concreta de TunnelConnection para el lado del servidor.
 * Representa la conexión con un móvil concreto.
 *
 * Internamente usa un Channel de Netty para enviar y recibir datos,
 * pero quien usa el túnel (el juego) nunca ve el Channel directamente,
 * solo ve TunnelConnection.
 */
public class NettyTunnelConnection implements TunnelConnection {

    private static final Logger log = LoggerFactory.getLogger(NettyTunnelConnection.class);

    /**
     * Versión actual del protocolo del túnel.
     * Se incluye en la cabecera de cada datagrama.
     */
    private static final byte PROTOCOL_VERSION = 0x01;

    /**
     * Canal de Netty que representa la conexión física con el móvil.
     * Es el objeto interno de Netty que gestiona el envío y recepción de datos.
     */
    private final Channel channel;

    /**
     * Identificador único de esta conexión.
     * Se genera a partir del identificador del canal de Netty.
     */
    private final String id;

    /**
     * Estado actual de la conexión.
     * AtomicReference porque el estado puede cambiar desde múltiples hilos
     * (hilo de red de Netty, hilo del juego...).
     */
    private final AtomicReference<State> state = new AtomicReference<>(State.CONNECTED);

    /**
     * @param channel El canal Netty de esta conexión.
     * @param id      Identificador único generado por QuicConnectionHandler.
     */
    public NettyTunnelConnection(Channel channel, String id) {
        this.channel = channel;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Envía un payload al móvil correspondiente a esta conexión.
     *
     * El túnel añade automáticamente la cabecera antes del payload:
     *   [version: 1 byte][length: 2 bytes][payload: N bytes]
     *
     * Es thread-safe: puede llamarse desde cualquier hilo.
     *
     * @param payload Los bytes a enviar. No puede ser null ni vacío.
     * @throws TunnelException si la conexión no está activa o el payload es inválido.
     */
    @Override
    public void send(byte[] payload) throws TunnelException {
        if (payload == null || payload.length == 0) {
            throw new TunnelException(
                    TunnelError.Type.INVALID_DATAGRAM,
                    "El payload no puede ser null ni vacío"
            );
        }

        if (state.get() != State.CONNECTED) {
            throw new TunnelException(
                    TunnelError.Type.CONNECTION_LOST,
                    "No se puede enviar: la conexión no está activa (estado: " + state.get() + ")"
            );
        }

        // Construimos el datagrama: [version|length|payload]
        // version: 1 byte
        // length:  2 bytes (short sin signo, máximo 65535 bytes de payload)
        // payload: N bytes
        ByteBuf buffer = Unpooled.buffer(3 + payload.length);
        buffer.writeByte(PROTOCOL_VERSION);
        buffer.writeShort(payload.length);
        buffer.writeBytes(payload);

        // writeAndFlush envía el buffer por el canal de Netty
        // Es asíncrono: no bloquea el hilo que llama a send()
        ChannelFuture future = channel.writeAndFlush(buffer);
        future.addListener(f -> {
            if (!f.isSuccess()) {
                log.error("Error al enviar datagrama a {}: {}", id, f.cause().getMessage());
            }
        });
    }

    /**
     * Cierra esta conexión de forma ordenada.
     * Cambia el estado a DISCONNECTED y cierra el canal Netty.
     */
    @Override
    public void close() {
        if (state.compareAndSet(State.CONNECTED, State.DISCONNECTED)) {
            log.info("Cerrando conexión: {}", id);
            channel.close();
        }
    }

    @Override
    public State getState() {
        return state.get();
    }

    /**
     * Cambia el estado de la conexión.
     * Llamado internamente por los handlers de Netty cuando
     * detectan cambios en la conexión.
     *
     * @param newState El nuevo estado.
     */
    void setState(State newState) {
        State previous = state.getAndSet(newState);
        log.debug("Conexión {}: {} → {}", id, previous, newState);
    }
}