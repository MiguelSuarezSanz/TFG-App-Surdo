package com.example.mandosapp_surdo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelException;
import com.quictunnel.core.TunnelListener;
import com.quictunnel.client.QuicTunnelClient;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private QuicTunnelClient tunnel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Estos son los colores que se le pasaran al segundo mando
        String[] colores = new String[] {"Rojo", "Amarillo", "Verde", "Azul"};

        final Button mando1 = findViewById(R.id.mando1);
        final Button mando2 = findViewById(R.id.mando2);
        final Button mando3 = findViewById(R.id.mando3);

        mando1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent myIntent = new Intent(MainActivity.this, VentanaMinijuegosActivity.class);
                startActivity(myIntent);
            }
        });

        /* Cuando se quiera entrar en el segundo mando, el programa elegira de forma aleatoria uno
        de los colores guardados, y se lo pasara a su actividad como un extra */
        mando2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String color = colores[new Random().nextInt(colores.length)];

                final Intent myIntent = new Intent(MainActivity.this, ConexionMandoActivity.class);
                myIntent.putExtra("color", color);
                startActivity(myIntent);
            }
        });

        try {
            TunnelConfig config = TunnelConfig.builder()
                    .host("192.168.137.1")
                    .port(4242)
                    .serverName("AppSurdo-Server")
                    .caCert(copyAssetToFile("ca.crt"))
                    .cert(copyAssetToFile("client.crt"))
                    .key(copyAssetToFile("client.key"))
                    .callbackExecutor(new Handler(Looper.getMainLooper())::post)
                    .build();

            tunnel = new QuicTunnelClient(config);
            tunnel.setListener(new TunnelListener() {
                @Override
                public void onConnected(TunnelConnection c) {
                    Log.i("Tunnel", "Conectado!");
                }

                @Override
                public void onDataReceived(TunnelConnection c, byte[] payload) {
                    Log.i("Tunnel", "Datos: " + new String(payload));
                }

                @Override
                public void onDisconnected(TunnelConnection c) {
                    Log.w("Tunnel", "Desconectado");
                }

                @Override
                public void onError(TunnelConnection c, TunnelError error) {
                    Log.e("Tunnel", "Error: " + error.getType());
                }
            });
            tunnel.connect();

        } catch (TunnelException | java.io.IOException e) {
            Log.e("Tunnel", "Error al iniciar: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tunnel != null) tunnel.stop();
    }

    private String copyAssetToFile(String assetName) throws java.io.IOException {
        java.io.File outFile = new java.io.File(getFilesDir(), assetName);
        try (java.io.InputStream in = getAssets().open(assetName);
             java.io.FileOutputStream out = new java.io.FileOutputStream(outFile)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
        return outFile.getAbsolutePath();
    }


}