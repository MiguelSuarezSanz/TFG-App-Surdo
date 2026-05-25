package gestiones_tunnel;

import com.quictunnel.core.TunnelConnection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Participantes {

    private static final Map<TunnelConnection, String>
            participantes = new HashMap<>();

    public static boolean existe(String nombre) {

        return participantes.containsValue(nombre);
    }

    public static void annadir(
            TunnelConnection conn,
            String nombre
    ) {

        participantes.put(conn, nombre);
    }

    public static String eliminar(
            TunnelConnection conn
    ) {

        return participantes.remove(conn);
    }
    
    public static Collection<TunnelConnection>
    getConexiones() {

        return participantes.keySet();
    }
}