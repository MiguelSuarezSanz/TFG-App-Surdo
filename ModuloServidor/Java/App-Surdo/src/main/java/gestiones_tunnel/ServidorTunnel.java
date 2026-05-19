package gestiones_tunnel;

import java.util.ArrayList;

import com.quictunnel.core.*;
import com.quictunnel.server.*;

import general.PuenteJava;

public class ServidorTunnel {

    private static QuicTunnelServer server;

    private static final PacketDispatcher dispatcher =
            new PacketDispatcher();
    
    private static final ArrayList<String> participantes =
			    new ArrayList<>();
    
    public static String iniciar() {

        try {

            String codigo =
                    obtenerCodigoConexion();

            TunnelConfig config = TunnelConfig.builder()
                    .port(4242)
                    .caCert("certs/ca.crt")
                    .cert("certs/server.crt")
                    .key("certs/server.key")
                    .keepaliveInterval(15000)
                    .build();

            server = new QuicTunnelServer(config);

            registrarPackets();

            server.setListener(new TunnelListener() {

                @Override
                public void onConnected(TunnelConnection conn) {

                    TunnelManager.setConnection(conn);
                }

                @Override
                public void onDataReceived(
                        TunnelConnection conn,
                        byte[] payload
                ) {

                    dispatcher.handle(payload);
                }

                @Override
                public void onDisconnected(
                        TunnelConnection conn
                ) {

                }

                @Override
                public void onError(
                        TunnelConnection conn,
                        TunnelError error
                ) {

                }
            });

            server.start();

            return codigo;

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    private static void registrarPackets() {

    	dispatcher.register(
    	        Protocol.MSG_SET_NAME,
    	        reader -> {

    	            String nombre =
    	                    reader.readString();

    	            PuenteJava.getInstancia()
    	                    .lamarJavascript(
    	                            "annadirParticipante",
    	                            "'" + nombre + "'"
    	                    );
    	        }
    	);
    }
    
    private static String obtenerCodigoConexion() {

        try {

            java.net.InetAddress localHost =
                    java.net.InetAddress.getLocalHost();

            String ip =
                    localHost.getHostAddress();

            return IpEncryptor.encrypt(ip);

        } catch (java.net.UnknownHostException e) {

            e.printStackTrace();

            return null;
        }
    }
}