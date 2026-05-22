package gestiones_tunnel;

import com.quictunnel.core.TunnelConnection;

import java.util.HashMap;
import java.util.Map;

public class PacketDispatcher {

    private final Map<Byte, PacketHandler> handlers =
            new HashMap<>();

    public void register(byte type, PacketHandler handler) {
        handlers.put(type, handler);
    }

    public void handle(
            TunnelConnection conn,
            byte[] payload
    ) {

        PacketReader reader =
                new PacketReader(payload);

        byte type = reader.readByte();

        PacketHandler handler =
                handlers.get(type);

        if (handler != null) {
            handler.handle(conn, reader);
        }
    }
}