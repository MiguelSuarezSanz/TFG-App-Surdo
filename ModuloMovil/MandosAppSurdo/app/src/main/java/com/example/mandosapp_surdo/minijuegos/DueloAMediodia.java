package com.example.mandosapp_surdo.minijuegos;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mandosapp_surdo.NsDondePonerte.Minijuego;
import com.example.mandosapp_surdo.NsDondePonerte.ResultadoCallback;

public class DueloAMediodia implements Minijuego {

    private static final long TIEMPO_LIMITE_MS   = 5000;
    private static final long TIEMPO_MANTENER_MS = 1500;
    private ResultadoCallback callback;
    private CountDownTimer timerGlobal;
    private long tiempoEnPosicion = 0;
    private boolean enPosicion = false;
    private boolean activo = false;
    private LinearLayout contenedor;
    private ImageView imagen;
    private TextView textoGiro;
    private TextView textoTiempo;

    public DueloAMediodia(LinearLayout contenedor, ImageView imagen, TextView textoGiro, TextView textoTiempo) {
        this.contenedor = contenedor;
        this.imagen = imagen;
        this.textoGiro = textoGiro;
        this.textoTiempo = textoTiempo;
    }

    @Override public String getTitulo() { return "Duelo a Mediodia"; }

    @Override
    public String getExplicacion() {
        return "Lo que todo hombre deseo una vez, un duelo a muerte de vaqueros." +
                "\n\n¡Desenfunde rapido forastero!";
    }

    @Override
    public void iniciar(ResultadoCallback cb) {
        this.callback         = cb;
        this.activo           = true;
        this.tiempoEnPosicion = 0;
        this.enPosicion       = false;

        contenedor.setVisibility(View.VISIBLE);
        imagen.setVisibility(View.INVISIBLE);
        textoGiro.setText("Inclina el móvil sobre su lateral...");

        timerGlobal = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
            @Override public void onTick(long ms) {
                textoTiempo.setText("⏱ " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                if (activo) { activo = false; callback.onPerdio(); }
            }
        }.start();
    }

    public void onDatosGiro(float pitch, float roll) {
        if (!activo) return;
        boolean ok = (Math.abs(roll) > 70f) && (Math.abs(pitch) < 20f);
        if (ok) {
            if (!enPosicion) { enPosicion = true; tiempoEnPosicion = System.currentTimeMillis(); }
            textoGiro.setText("✅ ¡Bien! Mantén...");
            imagen.setVisibility(View.VISIBLE);
            if (System.currentTimeMillis() - tiempoEnPosicion >= TIEMPO_MANTENER_MS) {
                activo = false;
                timerGlobal.cancel();
                callback.onGano();
            }
        } else {
            enPosicion = false;
            imagen.setVisibility(View.INVISIBLE);
            textoGiro.setText("Inclina el móvil sobre su lateral...");
        }
    }

    @Override
    public void detener() {
        activo = false;
        if (timerGlobal != null) timerGlobal.cancel();
        imagen.setVisibility(View.INVISIBLE);
    }
}