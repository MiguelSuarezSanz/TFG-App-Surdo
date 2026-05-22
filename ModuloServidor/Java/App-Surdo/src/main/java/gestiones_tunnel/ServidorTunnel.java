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

                	dispatcher.handle(conn, payload);
                }

                @Override
                public void onDisconnected(
                        TunnelConnection conn
                ) {

                    System.out.println("DESCONECTADO");

                    String nombre =
                            Participantes.eliminar(conn);

                    System.out.println("ELIMINADO -> " + nombre);

                    if (nombre != null) {

                        PuenteJava.getInstancia()
                                .lamarJavascript(
                                        "removerParticipante",
                                        "'" + nombre + "'"
                                );
                    }
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
    	        (conn, reader) -> {
    	        	String nombre =
    	        	        reader.readString();

    	        	if (Participantes.existe(nombre)) {

    	        	    PacketWriter writer =
    	        	            new PacketWriter();

    	        	    writer.writeByte(Protocol.MSG_NAME_ERROR);
    	        	    writer.writeString("Nombre ya en uso");

    	        	    try {
							TunnelManager
							        .getConnection()
							        .send(writer.toArray());
						} catch (TunnelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

    	        	    return;
    	        	}

    	        	Participantes.annadir(conn, nombre);

    	        	PuenteJava.getInstancia()
    	        	        .lamarJavascript(
    	        	                "annadirParticipante",
    	        	                "'" + nombre + "'"
    	        	        );
    	        	
    	        	PacketWriter writer =
    	        	        new PacketWriter();

    	        	writer.writeByte(Protocol.MSG_NAME_OK);

    	        	try {
						TunnelManager
						        .getConnection()
						        .send(writer.toArray());
					} catch (TunnelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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