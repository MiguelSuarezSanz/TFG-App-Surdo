package com.example.mandosapp_surdo.minijuegos;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mandosapp_surdo.NsDondePonerte.Minijuego;
import com.example.mandosapp_surdo.NsDondePonerte.ResultadoCallback;

import java.util.Random;

public class SaludoCatalan implements Minijuego {

    private static final long  TIEMPO_LIMITE_MS = 5000;
    private static final float MOVE_THRESHOLD = 12.0f;
    private int cuchilladas;
    private Random random = new Random();
    private ResultadoCallback callback;
    private CountDownTimer timer;
    private boolean activo = false;
    private int cambios = 0;
    private float ultimoEje = 0;
    private LinearLayout contenedor;
    private TextView emoji;
    private TextView texto;
    private TextView tiempo;

    public SaludoCatalan(LinearLayout contenedor, TextView emoji, TextView texto, TextView tiempo) {
        this.contenedor = contenedor;
        this.emoji = emoji;
        this.texto = texto;
        this.tiempo = tiempo;
    }

    @Override public String getTitulo() {
        return "Saludo Tradicional Catalan";
    }

    @Override
    public String getExplicacion() {
        return "Hombre pero mira quien esta ahi, Giuseppe, tu amigo catalan de la uni." +
                "\n\n¿Porque no vas a saludarlo al estilo de L'Hospitalet?";
    }

    @Override
    public void iniciar(ResultadoCallback cb) {
        this.callback  = cb;
        this.activo  = true;
        this.cambios = 0;
        this.ultimoEje = 0;
        cuchilladas = (int)(5 + ((Math.random() * (20 - 5))));

        contenedor.setVisibility(View.VISIBLE);
        emoji.setText("↔️");
        texto.setText("Movimientos: 0 / " + cuchilladas);

        timer = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
            @Override public void onTick(long ms) {
                tiempo.setText("⏱ " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                if (activo) { activo = false; callback.onPerdio(); }
            }
        }.start();
    }

    public void onDatosAcel(float ejeY) {
        if (!activo) return;
        if ((ultimoEje > MOVE_THRESHOLD  && ejeY < -MOVE_THRESHOLD) ||
                (ultimoEje < -MOVE_THRESHOLD && ejeY > MOVE_THRESHOLD)) {
            cambios++;
            ultimoEje = ejeY;
            texto.setText("Movimientos: " + cambios + " / " + cuchilladas);
            if (cambios >= cuchilladas) {
                activo = false;
                timer.cancel();

                callback.onGano();
            }
        } else if (Math.abs(ejeY) > MOVE_THRESHOLD) {
            ultimoEje = ejeY;
        }
    }

    @Override
    public void detener() {
        activo = false;
        if (timer != null) timer.cancel();
    }
}