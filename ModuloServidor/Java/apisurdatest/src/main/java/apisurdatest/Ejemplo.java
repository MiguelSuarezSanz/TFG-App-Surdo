package apisurdatest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.quictunnel.core.*;
import com.quictunnel.server.*;

public class Ejemplo {
    public static void main(String[] args) throws TunnelException {
        // Info del equipo
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Nombre del equipo: " + localHost.getHostName());
            System.out.println("IP local: " + localHost.getHostAddress());
        } catch (UnknownHostException e) {
            System.err.println("No se pudo obtener la IP del equipo.");
            e.printStackTrace();
        }

        System.out.println("Cargando certificados...");
        TunnelConfig config = TunnelConfig.builder()
            .port(4242)
            .caCert("certs/ca.crt")
            .cert("certs/server.crt")
            .key("certs/server.key")
            .build();
        System.out.println("Certificados OK");

        QuicTunnelServer server = new QuicTunnelServer(config);
        server.setListener(new TunnelListener() {
            @Override
            public void onConnected(TunnelConnection conn) {
                System.out.println("[CONECTADO] Móvil: " + conn.getId());
            }
            @Override
            public void onDataReceived(TunnelConnection conn, byte[] payload) {
                System.out.println("[DATOS] de " + conn.getId() + ": " + new String(payload));
            }
            @Override
            public void onDisconnected(TunnelConnection conn) {
                System.out.println("[DESCONECTADO] Móvil: " + conn.getId());
            }
            @Override
            public void onError(TunnelConnection conn, TunnelError error) {
                System.err.println("[ERROR] " + error.getMessage());
            }
        });

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) 
        	    org.slf4j.LoggerFactory.getLogger("ROOT");
        	root.setLevel(ch.qos.logback.classic.Level.DEBUG);
        
        server.start();
        System.out.println("Servidor escuchando en puerto 4242...");
        System.out.println("Esperando conexiones (Ctrl+C para parar)");

        // Mantener vivo el hilo principal
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Servidor detenido.");
            server.stop();
        }
    }
}
