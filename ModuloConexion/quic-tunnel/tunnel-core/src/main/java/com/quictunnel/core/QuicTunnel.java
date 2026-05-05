package com.quictunnel.core;

/**
 * Interfaz principal del túnel.
 * Es el punto de entrada tanto para el servidor como para el cliente.
 *
 * El servidor implementa esta interfaz con QuicTunnelServer.
 * El cliente la implementa con QuicTunnelClient.
 *
 * El juego solo necesita conocer esta interfaz para usar el túnel,
 * sin saber nada de Netty, quiche o QUIC internamente.
 *
 * Ejemplo servidor:
 *   QuicTunnel tunnel = new QuicTunnelServer(config);
 *   tunnel.setListener(listener);
 *   tunnel.start();
 *
 * Ejemplo cliente:
 *   QuicTunnel tunnel = new QuicTunnelClient(config);
 *   tunnel.setListener(listener);
 *   tunnel.connect();
 */
public interface QuicTunnel {

    /**
     * Registra el listener que recibirá los eventos del túnel.
     * Debe llamarse antes de start() o connect().
     *
     * @param listener El listener a registrar. No puede ser null.
     */
    void setListener(TunnelListener listener);

    /**
     * Arranca el servidor y comienza a escuchar conexiones entrantes.
     * Solo tiene sentido en el servidor (QuicTunnelServer).
     * El cliente usa connect() en su lugar.
     *
     * @throws TunnelException Si el servidor no puede arrancar.
     */
    void start() throws TunnelException;

    /**
     * Conecta al servidor. Solo tiene sentido en el cliente (QuicTunnelClient).
     * El servidor usa start() en su lugar.
     *
     * La conexión es explícita, el túnel no conecta automáticamente
     * al instanciarse. Esto permite obtener la IP del servidor
     * por cualquier medio (QR, manual...) antes de conectar.
     *
     * @throws TunnelException Si la configuración es incorrecta.
     */
    void connect() throws TunnelException;

    /**
     * Detiene el túnel de forma ordenada.
     * En el servidor: cierra todas las conexiones activas y deja de escuchar.
     * En el cliente: cierra la conexión con el servidor y cancela reconexiones.
     *
     * Después de stop() el túnel no puede reutilizarse,
     * hay que crear una nueva instancia.
     */
    void stop();

    /**
     * Devuelve el estado actual del túnel.
     *
     * @return El estado actual.
     */
    TunnelConnection.State getState();
}