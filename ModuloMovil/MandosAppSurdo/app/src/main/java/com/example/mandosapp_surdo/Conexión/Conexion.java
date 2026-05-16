package com.example.mandosapp_surdo.Conexión;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mandosapp_surdo.Conexión.Packets.PacketDispatcher;
import com.example.mandosapp_surdo.NsDondePonerte.IpEncryptor;
import com.example.mandosapp_surdo.R;
import com.quictunnel.client.QuicTunnelClient;
import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelException;
import com.quictunnel.core.TunnelListener;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Conexion extends AppCompatActivity {

    private List<EditText> fields;
    private Button btnConectar;
    private QuicTunnelClient tunnel;

    private final PacketDispatcher dispatcher =
            new PacketDispatcher();

    // Caracteres permitidos: letras A-Z y números 0-9
    private final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9]");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion);

        btnConectar = findViewById(R.id.btnConectar);

        fields = Arrays.asList(
                findViewById(R.id.char1),
                findViewById(R.id.char2),
                findViewById(R.id.char3),
                findViewById(R.id.char4),
                findViewById(R.id.char5),
                findViewById(R.id.char6),
                findViewById(R.id.char7),
                findViewById(R.id.char8)
        );

        for (int i = 0; i < fields.size(); i++) {
            setupField(fields.get(i), i);
        }

        dispatcher.register(
                Protocol.MSG_SET_NAME,
                reader -> {

                    String nombre = reader.readString();

                    Log.i(
                            "Tunnel",
                            "Nombre recibido: " + nombre
                    );
                }
        );

        btnConectar.setOnClickListener(v -> {
            StringBuilder codigo = new StringBuilder();

            for (EditText field : fields) {
                codigo.append(field.getText().toString());
            }

            connect(codigo.toString());
        });

        // Abre el teclado en el primer campo automáticamente
        fields.get(0).requestFocus();
    }

    private void setupField(EditText editText, int index) {

        // Filtro de caracteres permitidos
        InputFilter allowedFilter = (source, start, end, dest, dstart, dend) -> {
            StringBuilder filtered = new StringBuilder();

            for (int i = start; i < end; i++) {
                char c = source.charAt(i);

                if (ALLOWED.matcher(String.valueOf(c)).matches()) {
                    filtered.append(Character.toUpperCase(c));
                }
            }

            return filtered.toString();
        };

        editText.setFilters(new InputFilter[]{
                allowedFilter,
                new InputFilter.LengthFilter(1)
        });

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {

                if (s != null && s.length() == 1) {

                    // Avanza al siguiente campo
                    if (index < fields.size() - 1) {
                        fields.get(index + 1).requestFocus();
                    } else {
                        // Último campo: cierra el teclado
                        hideKeyboard(editText);
                    }
                }

                // Activa el botón solo si todos tienen 1 carácter
                boolean allFilled = true;

                for (EditText field : fields) {
                    if (field.getText().length() != 1) {
                        allFilled = false;
                        break;
                    }
                }

                btnConectar.setEnabled(allFilled);
            }
        });

        // Retroceso: si el campo está vacío, vuelve al anterior
        editText.setOnKeyListener((v, keyCode, event) -> {

            if (keyCode == KeyEvent.KEYCODE_DEL
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && editText.getText().toString().isEmpty()
                    && index > 0) {

                EditText previous = fields.get(index - 1);

                previous.requestFocus();
                previous.getText().clear();

                return true;
            }

            return false;
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void connect(String codigo) {
        try {
            TunnelConfig config = TunnelConfig.builder()
                    .host(IpEncryptor.decrypt(codigo))
                    .port(4242)
                    .serverName("AppSurdo-Server")
                    .caCert(copyAssetToFile("ca.crt"))
                    .cert(copyAssetToFile("client.crt"))
                    .key(copyAssetToFile("client.key"))
                    .callbackExecutor(new Handler(Looper.getMainLooper())::post)
                    .build();

            tunnel = new QuicTunnelClient(config);

            TunnelManager.setTunnel(tunnel);

            tunnel.setListener(new TunnelListener() {

                @Override
                public void onConnected(TunnelConnection c) {

                    Log.e("Tunnel", "ON CONNECTED");

                    Intent intent =
                            new Intent(
                                    Conexion.this,
                                    EligeNombre.class
                            );

                    startActivity(intent);
                }

                @Override
                public void onDataReceived(
                        TunnelConnection c,
                        byte[] payload
                ) {
                    dispatcher.handle(payload);
                }

                @Override
                public void onDisconnected(TunnelConnection c) {
                    Log.w("Tunnel", "Desconectado");
                }

                @Override
                public void onError(
                        TunnelConnection c,
                        TunnelError error
                ) {

                    Log.e(
                            "Tunnel",
                            "ERROR -> " +
                                    error.getType() +
                                    " | " +
                                    error.getMessage()
                    );
                }
            });
            Log.e("Tunnel", "Intentando conectar...");

            new Thread(() -> {

                try {

                    Log.e("Tunnel", "ANTES CONNECT");

                    tunnel.connect();

                    Log.e("Tunnel", "DESPUES CONNECT");

                } catch (Exception e) {

                    Log.e(
                            "Tunnel",
                            "EXCEPCION -> " + e.getMessage()
                    );
                }

            }).start();

        } catch (TunnelException | java.io.IOException e) {
            Log.e("Tunnel", "Error al iniciar: " + e.getMessage());
            //Mensaje de error al usuario
        }
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