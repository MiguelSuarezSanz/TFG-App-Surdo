package gestiones_tunnel;

import com.quictunnel.core.*;
import com.quictunnel.server.*;

public class TunnelManager {

    private static TunnelConnection connection;

    public static void setConnection(TunnelConnection conn) {
        connection = conn;
    }

    public static TunnelConnection getConnection() {
        return connection;
    }
}