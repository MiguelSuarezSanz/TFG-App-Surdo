package com.quictunnel.client;

import com.quictunnel.core.QuicTunnel;
import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelListener;

/**
 * Cliente de prueba mínimo.
 * Usa MockQuicheWrapper para simular quiche sin binarios nativos.
 *
 * Prueba tres escenarios:
 *   1. Conexión exitosa y envío de datos
 *   2. Fallo de conexión inicial
 *   3. Reconexión automática tras caída
 */
public class TunnelClientTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Prueba 1: Conexión exitosa ===");
        testConexionExitosa();

        System.out.println("\n=== Prueba 2: Fallo de conexión ===");
        testFalloConexion();

        System.out.println("\n=== Prueba 3: Reconexión automática ===");
        testReconexion();

        System.out.println("\n=== Todas las pruebas completadas ===");
    }

    // ─────────────────────────────────────────────
    // Prueba 1: conexión exitosa y envío de datos
    // ─────────────────────────────────────────────

    static void testConexionExitosa() throws Exception {
        MockQuicheWrapper mock = new MockQuicheWrapper();

        QuicTunnelClient client = crearClienteConMock(mock);

        client.setListener(new TunnelListener() {
            @Override
            public void onConnected(TunnelConnection connection) {
                System.out.println("[OK] onConnected llamado: " + connection.getId());
            }
            @Override
            public void onDataReceived(TunnelConnection connection, byte[] payload) {
                System.out.println("[OK] onDataReceived: " + new String(payload));
            }
            @Override
            public void onDisconnected(TunnelConnection connection) {
                System.out.println("[OK] onDisconnected: " + connection.getId());
            }
            @Override
            public void onError(TunnelConnection connection, TunnelError error) {
                System.out.println("[ERROR] " + error.getType() + ": " + error.getMessage());
            }
        });

        client.connect();
        System.out.println("[OK] Estado tras connect(): " + client.getState());

        // Enviamos un dato
        client.send("Hola servidor".getBytes());
        System.out.println("[OK] Datos enviados. sendCount=" + mock.sendCount);
        System.out.println("[OK] Último payload enviado (sin cabecera): "
                + extraerPayload(mock.lastSentData));

        Thread.sleep(500);
        client.stop();
        System.out.println("[OK] Estado tras stop(): " + client.getState());
    }

    // ─────────────────────────────────────────────
    // Prueba 2: fallo de conexión inicial
    // ─────────────────────────────────────────────

    static void testFalloConexion() throws Exception {
        MockQuicheWrapper mock = new MockQuicheWrapper();
        mock.shouldConnectFail = true;

        QuicTunnelClient client = crearClienteConMock(mock);

        client.setListener(new TunnelListener() {
            @Override public void onConnected(TunnelConnection c) {}
            @Override public void onDataReceived(TunnelConnection c, byte[] p) {}
            @Override public void onDisconnected(TunnelConnection c) {}
            @Override
            public void onError(TunnelConnection connection, TunnelError error) {
                System.out.println("[OK] Error esperado: " + error.getType()
                        + " - " + error.getMessage());
            }
        });

        try {
            client.connect();
            System.out.println("[FALLO] Debería haber lanzado TunnelException");
        } catch (Exception e) {
            System.out.println("[OK] TunnelException lanzada correctamente: "
                    + e.getMessage());
        }

        System.out.println("[OK] Estado: " + client.getState());
    }

    // ─────────────────────────────────────────────
    // Prueba 3: reconexión automática
    // ─────────────────────────────────────────────

    static void testReconexion() throws Exception {
        MockQuicheWrapper mock = new MockQuicheWrapper();

        QuicTunnelClient client = crearClienteConMock(mock);

        client.setListener(new TunnelListener() {
            @Override
            public void onConnected(TunnelConnection connection) {
                System.out.println("[OK] Conectado: " + connection.getId());
            }
            @Override public void onDataReceived(TunnelConnection c, byte[] p) {}
            @Override
            public void onDisconnected(TunnelConnection connection) {
                System.out.println("[OK] Desconectado, esperando reconexión...");
            }
            @Override
            public void onError(TunnelConnection connection, TunnelError error) {
                System.out.println("[OK] Error: " + error.getType()
                        + " - " + error.getMessage());
            }
        });

        client.connect();
        System.out.println("[OK] Conectado inicialmente.");

        // Simulamos caída de conexión
        Thread.sleep(500);
        mock.connected = false;
        System.out.println("[OK] Conexión caída simulada.");

        // Esperamos a que detecte la caída y reconecte
        Thread.sleep(3000);
        System.out.println("[OK] Estado tras reconexión: " + client.getState());

        client.stop();
    }

    // ─────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────

    /**
     * Crea un QuicTunnelClient pero reemplaza QuicheWrapper con el mock.
     * Usa una subclase anónima para inyectar el mock.
     */
    static QuicTunnelClient crearClienteConMock(MockQuicheWrapper mock) throws Exception {
        TunnelConfig config = TunnelConfig.builder()
                .host("localhost")
                .port(4242)
                .caCert("dummy-ca.crt")
                .cert("dummy-client.crt")
                .key("dummy-client.key")
                .reconnectMaxAttempts(3)
                .reconnectBackoff(500, 2000)
                .build();

        return new QuicTunnelClient(config, mock);
    }

    /**
     * Extrae el payload de un datagrama quitando los 3 bytes de cabecera.
     */
    static String extraerPayload(byte[] datagram) {
        if (datagram == null || datagram.length < 3) return "(vacío)";
        byte[] payload = new byte[datagram.length - 3];
        System.arraycopy(datagram, 3, payload, 0, payload.length);
        return new String(payload);
    }
}