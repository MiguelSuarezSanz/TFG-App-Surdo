package com.example.mandosapp_surdo.Conexión.Packets;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PacketWriter {

    private final ByteArrayOutputStream baos;
    private final DataOutputStream out;

    public PacketWriter() {
        baos = new ByteArrayOutputStream();
        out = new DataOutputStream(baos);
    }

    public PacketWriter writeByte(int value) {

        try {
            out.writeByte(value);
        } catch (IOException ignored) {}

        return this;
    }

    public PacketWriter writeInt(int value) {

        try {
            out.writeInt(value);
        } catch (IOException ignored) {}

        return this;
    }

    public PacketWriter writeString(String value) {

        try {

            byte[] bytes =
                    value.getBytes(StandardCharsets.UTF_8);

            out.writeInt(bytes.length);

            out.write(bytes);

        } catch (IOException ignored) {}

        return this;
    }

    public byte[] toArray() {
        return baos.toByteArray();
    }
}