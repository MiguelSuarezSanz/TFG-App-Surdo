package com.example.mandosapp_surdo.minijuegos;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mandosapp_surdo.Minijuego;
import com.example.mandosapp_surdo.ResultadoCallback;
import com.google.android.material.button.MaterialButton;
import android.content.res.ColorStateList;
import java.util.Random;

public class SimpleBoton implements Minijuego {

    // ---- Colores disponibles ----
    private static final String[] NOMBRES = {"Rojo", "Amarillo", "Verde", "Azul"};
    private static final int[] COLORES = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE};
    private static final long TIEMPO_LIMITE_MS = 10000;
    private final LinearLayout contenedor;
    private final MaterialButton boton;
    private final TextView texto;
    private final TextView tiempo;
    private final Random random = new Random();

    // ---- Estado ----
    private ResultadoCallback callback;
    private boolean activo = false;
    private boolean debesPulsar = false; // true = el texto pide pulsar
    private boolean coloresCoinc = false; // true = color botón == color pedido
    private CountDownTimer timer;

    public SimpleBoton(LinearLayout contenedor, MaterialButton boton, TextView texto, TextView tiempo) {
        this.contenedor = contenedor;
        this.boton = boton;
        this.texto = texto;
        this.tiempo = tiempo;
    }

    @Override
    public String getTitulo() {
        return "Un simple Boton";
    }

    @Override
    public String getExplicacion() {
        return "No te preocupes, solo te vas a encontrar un boton, nada raro va a pasar, " +
                "\n\n Solo sigue las instrucciones y no fallaras el minijuego";
    }

    @Override
    public void iniciar(ResultadoCallback callback) {

        this.callback = callback;
        this.activo = true;
        contenedor.setVisibility(View.VISIBLE);

        // Elegir color del botón al azar
        int idxBoton  = random.nextInt(NOMBRES.length);
        int colorBoton = COLORES[idxBoton];

        // Elegir color mencionado en el texto al azar (puede coincidir o no)
        int idxTexto  = random.nextInt(NOMBRES.length);

        // Elegir si el texto pide pulsar o no pulsar
        debesPulsar  = random.nextBoolean();

        // ¿Coinciden los colores?
        coloresCoinc = (idxBoton == idxTexto);

        // Aplicar color al botón
        boton.setBackgroundTintList(ColorStateList.valueOf(colorBoton));
        boton.setText("");

        // Construir el texto de instrucción
        String accion = debesPulsar ? "¡Pulsa el botón" : "¡No pulses el botón";
        texto.setText(accion + " " + NOMBRES[idxTexto] + "!");

        // Listener del botón
        boton.setOnClickListener(v -> { if (activo) { evaluar(true); } });

        // Timer: al agotarse cuenta como "no pulsó"
        timer = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
            @Override public void onTick(long ms) {
                tiempo.setText("⏱ " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                if (activo) {
                    evaluar(false); // tiempo agotado = no pulsó
                }
            }
        }.start();
    }

    private void evaluar(boolean pulso) {
        activo = false;
        timer.cancel();
        boton.setOnClickListener(null);
        boolean debioPulsar = (debesPulsar && coloresCoinc) || (!debesPulsar && !coloresCoinc);
        boolean gano = (pulso == debioPulsar);

        if (gano) {
            callback.onGano();
        } else {
            callback.onPerdio();
        }
    }

    @Override
    public void detener() {
        activo = false;
        if (timer != null) {
            timer.cancel();
        }
        boton.setOnClickListener(null);
        contenedor.setVisibility(View.GONE);
    }
}