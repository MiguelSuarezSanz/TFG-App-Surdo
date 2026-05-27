package gestiones_tunnel;

import com.quictunnel.core.TunnelConnection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Participantes {

    private static final Map<TunnelConnection, String>
            participantes = new HashMap<>();
    
    private static final Map<TunnelConnection, Integer>
    		puntuaciones = new HashMap<>();

    public static boolean existe(String nombre) {

        return participantes.containsValue(nombre);
    }

    public static void annadir(
            TunnelConnection conn,
            String nombre
    ) {

        participantes.put(conn, nombre);
        puntuaciones.put(conn, 0);
    }

    public static String eliminar(
            TunnelConnection conn
    ) {

        return participantes.remove(conn);
    }

    public static String obtenerNombre(
            TunnelConnection conn
    ) {

        return participantes.get(conn);
    }

    public static Collection<TunnelConnection>
    getConexiones() {

        return participantes.keySet();
    }
    
    public static void establecerPuntuacion(
            TunnelConnection conn,
            int puntuacion
    ) {
        puntuaciones.put(conn, puntuacion);
    }

    public static int obtenerPuntuacion(
            TunnelConnection conn
    ) {
        return puntuaciones.getOrDefault(conn, 0);
    }
    
    public static Map<TunnelConnection, Integer>
    	obtenerPuntuaciones() {

        return puntuaciones;
    }
}