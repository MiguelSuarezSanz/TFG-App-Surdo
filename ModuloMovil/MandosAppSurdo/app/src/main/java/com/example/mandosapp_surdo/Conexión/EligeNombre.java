package com.example.mandosapp_surdo.Conexión;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mandosapp_surdo.Conexión.TunnelManager;
import com.example.mandosapp_surdo.R;
import com.quictunnel.client.QuicTunnelClient;
import com.quictunnel.core.TunnelException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class EligeNombre extends AppCompatActivity {

    private Button btnEnviar;
    private EditText inputMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elige_nombre);

        inputMensaje = findViewById(R.id.inputMensaje);

        btnEnviar = findViewById(R.id.btnEnviar);

        btnEnviar.setOnClickListener(v -> {

            QuicTunnelClient tunnel = TunnelManager.getTunnel();

            if (tunnel == null) return;

            try {
                String nombre = inputMensaje.getText().toString();

                byte[] nombreBytes = nombre.getBytes(StandardCharsets.UTF_8);

                ByteBuffer buffer =
                        ByteBuffer.allocate(1 + 4 + nombreBytes.length);

                buffer.put((byte)0x01); // tipo

                buffer.putInt(nombreBytes.length); // longitud

                buffer.put(nombreBytes); // datos

                tunnel.send(buffer.array());
            } catch (TunnelException e) {
                e.printStackTrace();
            }
        });
    }
}