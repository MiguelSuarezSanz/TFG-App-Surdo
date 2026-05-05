package com.example.mandosapp_surdo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelException;
import com.quictunnel.core.TunnelListener;
import com.quictunnel.client.QuicTunnelClient;

public class ConexionMandoActivity extends AppCompatActivity {

    private QuicTunnelClient tunnel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion_mando);

        try {
            TunnelConfig config = TunnelConfig.builder()
                    .host("192.168.3.16")
                    .port(4242)
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
        if (!outFile.exists()) {
            try (java.io.InputStream in = getAssets().open(assetName);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(outFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
        }
        return outFile.getAbsolutePath();
    }
}