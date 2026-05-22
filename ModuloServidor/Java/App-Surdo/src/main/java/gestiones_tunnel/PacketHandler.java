package gestiones_tunnel;

import com.quictunnel.core.TunnelConnection;

public interface PacketHandler {

    void handle(
            TunnelConnection conn,
            PacketReader reader
    );
}