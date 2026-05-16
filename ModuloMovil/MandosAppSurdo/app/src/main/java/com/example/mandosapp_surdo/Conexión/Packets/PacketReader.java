package com.example.mandosapp_surdo.Conexión.Packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PacketReader {

    private final ByteBuffer buffer;

    public PacketReader(byte[] data) {
        buffer = ByteBuffer.wrap(data);
    }

    public byte readByte() {
        return buffer.get();
    }

    public int readInt() {
        return buffer.getInt();
    }

    public String readString() {

        int length = readInt();

        byte[] bytes = new byte[length];

        buffer.get(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }
}