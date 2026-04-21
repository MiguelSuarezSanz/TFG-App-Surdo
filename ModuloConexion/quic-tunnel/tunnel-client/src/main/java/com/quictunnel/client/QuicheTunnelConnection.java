package com.quictunnel.client;

import com.quictunnel.client.jni.QuicheWrapper;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementación concreta de TunnelConnection para el lado del cliente.
 * Representa la conexión del móvil con el servidor.
 *
 * Internamente usa QuicheWrapper para hablar con la librería nativa quiche,
 * pero quien usa el túnel (la app Android) nunca ve ni QuicheWrapper ni quiche,
 * solo ve TunnelConnection.
 *
 * A diferencia de NettyTunnelConnection en el servidor, aquí solo hay
 * una conexión activa a la vez (la del servidor).
 */
public class QuicheTunnelConnection implements TunnelConnection {

    private static final Logger log = LoggerFactory.getLogger(QuicheTunnelConnection.class);

    /**
     * Versión actual del protocolo del túnel.
     * Debe coincidir con la del servidor.
     */
    private static final byte PROTOCOL_VERSION = 0x01;

    /**
     * Tamaño máximo del buffer para recibir datagramas.
     * 65535 bytes es el máximo de un datagrama QUIC.
     */
    private static final int RECEIVE_BUFFER_SIZE = 65535;

    private final QuicheWrapper quiche;

    /**
     * Handle nativo de la conexión devuelto por quiche.
     * Es un puntero al objeto de conexión en memoria nativa.
     * -1 significa que no hay conexión activa.
     */
    private final long connHandle;

    private final String id;
    private final AtomicReference<State> state;

    /**
     * @param quiche     El wrapper JNI de quiche.
     * @param connHandle El handle nativo de la conexión ya establecida.
     * @param id         Identificador único de esta conexión.
     */
    public QuicheTunnelConnection(QuicheWrapper quiche, long connHandle, String id) {
        this.quiche = quiche;
        this.connHandle = connHandle;
        this.id = id;
        this.state = new AtomicReference<>(State.CONNECTED);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Envía un payload al servidor.
     *
     * Construye el datagrama añadiendo la cabecera:
     *   [version: 1 byte][length: 2 bytes][payload: N bytes]
     *
     * Es thread-safe: puede llamarse desde cualquier hilo.
     *
     * @param payload Los bytes a enviar. No puede ser null ni vacío.
     * @throws TunnelException si la conexión no está activa o el envío falla.
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
                    "No se puede enviar: conexión no activa (estado: " + state.get() + ")"
            );
        }

        // Construimos el datagrama con cabecera
        // version: 1 byte, length: 2 bytes, payload: N bytes
        byte[] datagram = new byte[3 + payload.length];
        datagram[0] = PROTOCOL_VERSION;
        datagram[1] = (byte) ((payload.length >> 8) & 0xFF); // byte alto de length
        datagram[2] = (byte) (payload.length & 0xFF);         // byte bajo de length
        System.arraycopy(payload, 0, datagram, 3, payload.length);

        int bytesSent = quiche.sendDatagram(connHandle, datagram);

        if (bytesSent < 0) {
            String error = quiche.getLastError(connHandle);
            throw new TunnelException(
                    TunnelError.Type.CONNECTION_LOST,
                    "Error al enviar datagrama: " + error
            );
        }

        log.debug("Datagrama enviado: {} bytes de payload", payload.length);
    }

    /**
     * Intenta recibir un datagrama del servidor.
     *
     * Lee los bytes del buffer, verifica la cabecera y devuelve
     * solo el payload al caller (QuicClientConnection).
     *
     * @param timeoutMs Tiempo máximo de espera en milisegundos.
     * @return El payload recibido, o null si timeout o conexión cerrada.
     * @throws TunnelException si el datagrama está malformado.
     */
    public byte[] receive(int timeoutMs) throws TunnelException {
        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        int bytesRead = quiche.receiveDatagram(connHandle, buffer, timeoutMs);

        // Timeout: no llegó nada en el tiempo dado
        if (bytesRead == 0) {
            return null;
        }

        // Error de conexión
        if (bytesRead < 0) {
            if (state.get() == State.CONNECTED) {
                setState(State.DISCONNECTED);
            }
            return null;
        }

        // Verificamos cabecera mínima: al menos 3 bytes
        if (bytesRead < 3) {
            throw new TunnelException(
                    TunnelError.Type.INVALID_DATAGRAM,
                    "Datagrama demasiado corto: " + bytesRead + " bytes"
            );
        }

        // Leemos la cabecera
        // version: lo leemos pero no validamos, el juego decide
        @SuppressWarnings("unused")
        byte version = buffer[0];

        // length: 2 bytes big-endian
        int declaredLength = ((buffer[1] & 0xFF) << 8) | (buffer[2] & 0xFF);
        int actualLength = bytesRead - 3;

        if (declaredLength != actualLength) {
            throw new TunnelException(
                    TunnelError.Type.INVALID_DATAGRAM,
                    "Length incorrecto: declarado=" + declaredLength + " real=" + actualLength
            );
        }

        // Extraemos y devolvemos solo el payload
        byte[] payload = new byte[actualLength];
        System.arraycopy(buffer, 3, payload, 0, actualLength);
        return payload;
    }

    @Override
    public void close() {
        if (state.compareAndSet(State.CONNECTED, State.DISCONNECTED)) {
            log.info("Cerrando conexión: {}", id);
            quiche.close(connHandle);
        }
    }

    @Override
    public State getState() {
        return state.get();
    }

    /**
     * Cambia el estado de la conexión.
     * Llamado internamente por QuicClientConnection.
     */
    void setState(State newState) {
        State previous = state.getAndSet(newState);
        log.debug("Conexión {}: {} → {}", id, previous, newState);
    }

    /**
     * Devuelve el handle nativo de la conexión.
     * Usado por QuicClientConnection para verificar si sigue activa.
     */
    long getConnHandle() {
        return connHandle;
    }
}