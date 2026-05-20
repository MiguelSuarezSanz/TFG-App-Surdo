package com.example.mandosapp_surdo.minijuegos;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mandosapp_surdo.interfaces.Minijuego;
import com.example.mandosapp_surdo.interfaces.ResultadoCallback;
import java.util.Random;

public class CurtKobainSimulator implements Minijuego {

    // ---- Umbrales ----
    private static final float SHAKE_THRESHOLD = 25.0f; // magnitud para cargar
    private static final float PITCH_OBJETIVO = -45f; // parte superior apuntando a la cara ~45° sobre el suelo
    private static final float PITCH_MARGEN = 10f; // ±5°
    private static final float ROLL_OBJETIVO = 90f; // móvil de canto sobre su lateral
    private static final float ROLL_MARGEN  = 10f; // ±5°

    private final Random random = new Random();

    // ---- Vistas ----
    private final LinearLayout contenedor;
    private final ImageView imagen;
    private final TextView estado;
    private final TextView tiempo;

    // ---- Estado ----
    private ResultadoCallback callback;
    private boolean activo = false;
    private boolean cargado = false;
    private int numeroCamara = 0;
    private CountDownTimer timerDisparo;

    public CurtKobainSimulator(LinearLayout contenedor, ImageView imagen, TextView estado, TextView tiempo) {
        this.contenedor = contenedor;
        this.imagen = imagen;
        this.estado = estado;
        this.tiempo = tiempo;
    }

    @Override public String getTitulo() { return "Kurt Cobain Simulator"; }

    @Override
    public String getExplicacion() {
        return "¿Que vueltas da la vida no? Hace unos años estabas en la cima, y mirate ahora, apunto de volarte los sesos" +
                "\n\n¿Que tal si dejas el destino de tu vida a la suerte?";
    }

    @Override
    public void iniciar(ResultadoCallback cb) {

        this.callback = cb;
        this.activo   = true;
        this.cargado  = false;
        contenedor.setVisibility(View.VISIBLE);
        imagen.setVisibility(View.INVISIBLE);
        tiempo.setText("");
        estado.setText("Sacude para cargar el revolver");
    }

    // Llamado desde VentanaMinijuegosActivity con la magnitud de aceleración lineal
    public void onDatosAcel(float magnitud) {
        if (!activo || cargado) return;

        if (magnitud > SHAKE_THRESHOLD) {
            cargado      = true;
            numeroCamara = random.nextInt(6) + 1;  // 1–6
            estado.setText("Vamos no seas gallina, dispara >:)");
            imagen.setVisibility(View.VISIBLE);

            // 10 segundos para posicionar el móvil; si se acaba, pierde
            timerDisparo = new CountDownTimer(10000, 100) {
                @Override public void onTick(long ms) {
                    tiempo.setText("⏱ " + (ms / 1000 + 1) + "s");
                }
                @Override public void onFinish() {
                    if (activo) {
                        activo = false;
                        callback.onPerdio();
                    }
                }
            }.start();
        }
    }

    // Llamado desde VentanaMinijuegosActivity con pitch y roll del giroscopio
    public void onDatosGiro(float pitch, float roll) {
        if (!activo || !cargado) return;

        // Posición de disparo: de canto (|roll| ≈ 90° ± 5°) y parte superior apuntando a la cara (pitch ≈ -45° ± 5°)
        boolean cantoCorrect  = Math.abs(Math.abs(roll) - ROLL_OBJETIVO) < ROLL_MARGEN;
        boolean pitchCorrecto = pitch >= (PITCH_OBJETIVO - PITCH_MARGEN)
                && pitch <= (PITCH_OBJETIVO + PITCH_MARGEN);

        if (cantoCorrect && pitchCorrecto) {
            activo = false;
            if (timerDisparo != null) timerDisparo.cancel();
            int numeroDisparo = random.nextInt(6) + 1;  // 1–6
            tiempo.setText("");

            if (numeroDisparo == numeroCamara) {
                callback.onPerdio();
            } else {
                callback.onGano();
            }
        }
    }

    @Override
    public void detener() {
        activo = false;
        if (timerDisparo != null) timerDisparo.cancel();
        imagen.setVisibility(View.INVISIBLE);
        tiempo.setText("");
    }
}