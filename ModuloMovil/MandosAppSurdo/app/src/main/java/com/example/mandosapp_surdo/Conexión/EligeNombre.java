package com.example.mandosapp_surdo.Conexión;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

            if (!TunnelManager.isConnected()) {

                Toast.makeText(
                        this,
                        "No conectado al servidor",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            try {
                String nombre = inputMensaje.getText().toString();

                byte[] packet = new PacketWriter()
                        .writeByte(Protocol.MSG_SET_NAME)
                        .writeString(nombre)
                        .toArray();

                tunnel.send(packet);
            } catch (TunnelException e) {
                e.printStackTrace();
            }
        });
    }
}