
public class TunnelManager {

    private static TunnelConnection connection;

    public static void setConnection(TunnelConnection conn) {
        connection = conn;
    }

    public static TunnelConnection getConnection() {
        return connection;
    }
}