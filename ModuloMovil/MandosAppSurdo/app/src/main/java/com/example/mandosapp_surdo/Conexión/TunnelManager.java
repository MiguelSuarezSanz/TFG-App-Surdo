package com.example.mandosapp_surdo.Conexión;

import com.quictunnel.client.QuicTunnelClient;
import com.quictunnel.core.TunnelConnection;

public class TunnelManager {

    private static QuicTunnelClient tunnel;

    public static void setTunnel(QuicTunnelClient t) {
        tunnel = t;
    }

    public static QuicTunnelClient getTunnel() {
        return tunnel;
    }

    public static boolean isConnected() {
        return tunnel != null &&
                tunnel.getState() == TunnelConnection.State.CONNECTED;
    }

    public static void disconnect() {
        if (tunnel != null) {
            tunnel.stop();
            tunnel = null;
        }
    }
}