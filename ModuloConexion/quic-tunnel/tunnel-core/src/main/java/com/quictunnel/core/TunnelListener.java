package com.quictunnel.core;

/**
 * Interfaz que debe implementar quien use el túnel (el juego).
 * Define los callbacks que el túnel llamará cuando ocurra algo relevante.
 *
 * El hilo en el que se ejecutan estos callbacks depende del Executor
 * configurado en TunnelConfig. Por defecto no se garantiza ningún hilo
 * concreto, es responsabilidad de quien implemente esta interfaz
 * gestionarlo si lo necesita (por ejemplo, Android no puede tocar
 * la UI desde un hilo que no sea el principal).
 */
public interface TunnelListener {

    /**
     * Se llama cuando un extremo se conecta exitosamente.
     * En el servidor: cuando un cliente Android completa el handshake mTLS.
     * En el cliente: cuando se conecta al servidor.
     *
     * @param connection La conexión recién establecida.
     */
    void onConnected(TunnelConnection connection);

    /**
     * Se llama cuando llega un datagrama con datos.
     * El payload es exactamente lo que el otro extremo envió con send(),
     * el túnel no lo modifica ni lo interpreta.
     *
     * @param connection La conexión desde la que llegaron los datos.
     * @param payload Los bytes recibidos, tal cual los envió el otro extremo.
     */
    void onDataReceived(TunnelConnection connection, byte[] payload);

    /**
     * Se llama cuando una conexión se cierra.
     * Puede ser por desconexión voluntaria o por pérdida de conexión.
     * Si el túnel está configurado para reconectar automáticamente,
     * este callback se llama igualmente antes de intentar reconectar.
     *
     * @param connection La conexión que se ha cerrado.
     */
    void onDisconnected(TunnelConnection connection);

    /**
     * Se llama cuando ocurre un error en el túnel.
     * El tipo de error está detallado en TunnelError para que
     * quien implemente esta interfaz pueda reaccionar de forma
     * diferente según el tipo.
     *
     * @param connection La conexión en la que ocurrió el error.
     *                   Puede ser null si el error ocurrió antes
     *                   de establecer la conexión.
     * @param error El error que ocurrió.
     */
    void onError(TunnelConnection connection, TunnelError error);
}