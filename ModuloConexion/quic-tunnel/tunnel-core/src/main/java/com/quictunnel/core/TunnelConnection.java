package com.quictunnel.core;

/**
 * Representa una conexión activa entre dos extremos del túnel.
 *
 * En el servidor: hay una TunnelConnection por cada móvil conectado.
 * En el cliente: hay una única TunnelConnection con el servidor.
 *
 * El juego recibe estas conexiones a través de TunnelListener
 * y las usa para enviar datos al extremo correspondiente.
 */
public interface TunnelConnection {

    /**
     * Identificador único de esta conexión.
     * El servidor lo usa para distinguir entre varios móviles conectados.
     * El juego puede usarlo para asociar una conexión a un jugador concreto.
     *
     * @return Identificador único de la conexión.
     */
    String getId();

    /**
     * Envía un payload al otro extremo de esta conexión.
     * Es thread-safe, puede llamarse desde cualquier hilo.
     *
     * El túnel añade la cabecera (version + length) automáticamente.
     * El juego solo proporciona el payload, no necesita construir la cabecera.
     *
     * @param payload Los bytes a enviar. No puede ser null ni vacío.
     * @throws TunnelException Si la conexión no está activa o el payload es inválido.
     */
    void send(byte[] payload) throws TunnelException;

    /**
     * Cierra esta conexión de forma ordenada.
     * Dispara onDisconnected() en el listener.
     * En el cliente, si hay reconexión automática configurada,
     * close() la desactiva antes de cerrar.
     */
    void close();

    /**
     * Devuelve el estado actual de esta conexión.
     *
     * @return El estado actual.
     */
    State getState();

    /**
     * Estados posibles de una conexión.
     */
    enum State {
        IDLE,         // Conexión no iniciada
        CONNECTING,   // Handshake mTLS en curso
        CONNECTED,    // Conexión activa, puede enviar y recibir
        DISCONNECTED, // Conexión perdida, puede estar reconectando
        RECONNECTING, // Intentando reconectar
        ERROR         // Error irrecuperable, no se intentará reconectar
    }
}