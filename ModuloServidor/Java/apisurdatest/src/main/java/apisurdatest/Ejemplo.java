package apisurdatest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.quictunnel.core.*;
import com.quictunnel.server.*;

public class Ejemplo {

	public static void main(String[] args) throws TunnelException {
		// TODO Auto-generated method stub
        try {
            InetAddress localHost = InetAddress.getLocalHost();

            //System.out.println("Nombre del equipo: " + localHost.getHostName());
            //System.out.println("IP local: " + localHost.getHostAddress());

        } catch (UnknownHostException e) {
            System.err.println("No se pudo obtener la IP del equipo.");
            e.printStackTrace();
        }
        
        TunnelConfig config = TunnelConfig.builder()
        	    .port(4242)
                .caCert("certs/ca.crt")
                .cert("certs/server.key")
                .key("certs/server.crt")
        	    .build();

        	QuicTunnelServer server = new QuicTunnelServer(config);
        	server.setListener(new TunnelListener() {
        	    @Override
        	    public void onConnected(TunnelConnection conn) {
        	        System.out.println("Móvil conectado: " + conn.getId());
        	    }
        	    @Override
        	    public void onDataReceived(TunnelConnection conn, byte[] payload) {
        	        System.out.println("Datos: " + new String(payload));
        	    }
        	    @Override
        	    public void onDisconnected(TunnelConnection conn) {
        	        System.out.println("Móvil desconectado: " + conn.getId());
        	    }
        	    @Override
        	    public void onError(TunnelConnection conn, TunnelError error) {
        	        System.err.println("Error: " + error.getMessage());
        	    }
        	});
        	server.start();
        	System.out.println("Servidor escuchando en puerto 4242...");

	}

}
