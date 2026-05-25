package gestiones_tunnel;

import java.util.ArrayList;
import java.util.Random;

import org.cef.browser.CefBrowser;

import com.quictunnel.core.*;
import com.quictunnel.server.*;

import general.PuenteJava;

public class ServidorTunnel {

    private static QuicTunnelServer server;

    private static final PacketDispatcher dispatcher =
            new PacketDispatcher();
    
    private static final ArrayList<String> participantes =
			    new ArrayList<>();
    
    private static final Random random =
            new Random();

    
    public static String iniciar(PuenteJava pj) {
    	
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

            registrarPackets(pj);

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

                        pj.lamarJavascript(
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

    private static void registrarPackets(PuenteJava pj) {

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

    	        	pj.lamarJavascript(
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
    	
    	dispatcher.register(
    	        Protocol.MSG_MINIGAME_COUNT,
    	        (conn, reader) -> {

    	            pj.cantidadMinijuegos =
    	                    reader.readInt();
    	        }
    	);
    	
    	dispatcher.register(
    	        Protocol.MSG_REQUEST_MINIGAME_STATEMENT,
    	        (conn, reader) -> {

    	            String nombre =
    	                    reader.readString();

    	            String descripcion =
    	                    reader.readString();
    	            System.out.println(nombre + "   " + descripcion);
    	            pj.titulo = nombre;
    	            pj.enunciado = descripcion;
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
    
    public static void siguienteMinijuego(PuenteJava pj) {
        if (pj.cantidadMinijuegos <= 0) {
            return;
        }
        
        int numero;
        
        do {
	        numero = random.nextInt(pj.cantidadMinijuegos);
        } while(numero == pj.ultimoMinijuego);
        
        pj.ultimoMinijuego = numero;     

        prepararMinijuego(numero);
        
        pj.lamarJavascript(
                "cambiarPagina",
                "'../minigame_room/minagame_room.html'"
        );
        
        System.out.println("2"+pj.titulo + "   " + pj.enunciado);
        String[] minijuego = {pj.titulo,pj.enunciado};
        
        
        new java.util.Timer().schedule(
        	    new java.util.TimerTask() {
        	        @Override
        	        public void run() {
        	            pj.lamarJavascript(
        	                "establecerMinijuego",
        	                "'" + pj.titulo + "@" + pj.enunciado + "'"
        	            );
        	        }
        	    },
        	    1000 // 1 segundo en milisegundos
        	);
        
    }
    
    public static void prepararMinijuego(int numero) {
        PacketWriter writer =
                new PacketWriter();

        writer.writeByte(
                Protocol.MSG_PREP_MINIGAME
        );

        writer.writeInt(numero);

        try {

            for (TunnelConnection conn
                    : Participantes.getConexiones()) {

                conn.send(writer.toArray());
            }

        } catch (TunnelException e) {

            e.printStackTrace();
        }
    }
    
    public static void iniciarMinijuego() {

        PacketWriter writer =
                new PacketWriter();

        writer.writeByte(
                Protocol.MSG_PLAY_MINIGAME
        );
        
        try {

            for (TunnelConnection conn
                    : Participantes.getConexiones()) {

                conn.send(writer.toArray());
            }

        } catch (TunnelException e) {

            e.printStackTrace();
        }
    }
    
}