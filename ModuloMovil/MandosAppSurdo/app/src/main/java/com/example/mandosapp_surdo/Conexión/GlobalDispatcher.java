package com.example.mandosapp_surdo.Conexión;

import com.example.mandosapp_surdo.Conexión.Packets.PacketDispatcher;

public class GlobalDispatcher {

    private static final PacketDispatcher dispatcher =
            new PacketDispatcher();

    public static PacketDispatcher get() {
        return dispatcher;
    }
}