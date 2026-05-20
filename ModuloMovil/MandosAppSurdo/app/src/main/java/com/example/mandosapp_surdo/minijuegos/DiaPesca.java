package com.example.mandosapp_surdo.minijuegos;

import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mandosapp_surdo.interfaces.Minijuego;
import com.example.mandosapp_surdo.interfaces.ResultadoCallback;
import com.example.mandosapp_surdo.enume.Fase;

import java.util.Random;

public class DiaPesca implements Minijuego {
    private final float SHAKE_THRESHOLD  = 50.0f;
    private final long  TIEMPO_LANZAR_MS = 5000;
    private final long  TIEMPO_ESPERA_MS = 15000;
    private final long  TIEMPO_SACAR_MS  = 3000;
    private final Random random = new Random();
    private ResultadoCallback callback;
    private Fase fase = Fase.INACTIVO;
    private CountDownTimer timer;
    private boolean activo = false;
    private LinearLayout contenedor;
    private TextView emoji;
    private TextView texto;
    private TextView tiempo;
    private Handler handler;

    public DiaPesca(LinearLayout contenedor, TextView emoji, TextView texto, TextView tiempo, Handler handler) {
        this.contenedor = contenedor;
        this.emoji = emoji;
        this.texto = texto;
        this.tiempo = tiempo;
        this.handler = handler;
    }

    @Override public String getTitulo() {
        return "Un Dia de Pesca";
    }

    @Override
    public String getExplicacion() {
        return "Ahh, el mar, un sitio magnifico ¿verdad?, porque no te relajas," +
                "\n\nCojes esa caña de ahi y te pones a pescar";
    }

    @Override
    public void iniciar(ResultadoCallback cb) {
        this.callback = cb;
        this.activo = true;
        contenedor.setVisibility(View.VISIBLE);
        iniciarFaseLanzar();
    }

    // ------ FASE 1: Lanzar ------
    private void iniciarFaseLanzar() {
        fase = Fase.LANZAR;
        emoji.setText("🎣");
        texto.setText("¡Sacude para lanzar el cebo!");

        timer = new CountDownTimer(TIEMPO_LANZAR_MS, 100) {
            @Override public void onTick(long ms) {
                tiempo.setText("⏱ Lanzar: " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                if (activo) {
                    activo = false;
                    callback.onPerdio();
                }
            }
        }.start();
    }

    // ------ FASE 2: Esperar al pez ------
    private void iniciarFaseEspera() {
        fase = Fase.ESPERAR;
        timer.cancel();
        emoji.setText("🌊");
        texto.setText("El cebo está en el agua... ¡espera!");
        tiempo.setText("");

        // El pez aparece en un instante aleatorio entre 2s y el final de los 15s
        long tiempoPez = 2000 + (long)(random.nextFloat() * (TIEMPO_ESPERA_MS - 2000));

        // Timer global de 15s: si pasa entero sin que pesque → pierde
        timer = new CountDownTimer(TIEMPO_ESPERA_MS, 100) {
            @Override public void onTick(long ms) { /* sin countdown visible */ }
            @Override public void onFinish() {
                if (activo && fase == Fase.ESPERAR) { activo = false; callback.onPerdio(); }
            }
        }.start();

        // Programar la aparición del pez
        handler.postDelayed(() -> {
            if (activo && fase == Fase.ESPERAR) iniciarFasePez();
        }, tiempoPez);
    }

    // ------ FASE 3: ¡Pez! ------
    private void iniciarFasePez() {
        fase = Fase.PEZ_ACTIVO;
        timer.cancel();
        emoji.setText("🐟");
        texto.setText("¡¡A PICADO, AGITA!!");

        timer = new CountDownTimer(TIEMPO_SACAR_MS, 100) {
            @Override public void onTick(long ms) {
                tiempo.setText("⏱ " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                if (activo) { activo = false; callback.onPerdio(); }
            }
        }.start();
    }

    // Llamado desde onSensorChanged con la magnitud de aceleración lineal
    public void onDatosAcel(float magnitud) {
        if (!activo) return;
        boolean sacudida = magnitud > SHAKE_THRESHOLD;

        switch (fase) {
            case LANZAR:
                if (sacudida) {
                    emoji.setText("🎯");
                    texto.setText("¡Lanzado! Ahora espera...");
                    iniciarFaseEspera();
                }
                break;

            case ESPERAR:
                if (sacudida) {
                    // Sacudida prematura: asusta al pez
                    activo = false;
                    timer.cancel();
                    handler.removeCallbacksAndMessages(null);
                    emoji.setText("💦");
                    texto.setText("¡Asustaste al pez!");
                    callback.onPerdio();
                }
                break;

            case PEZ_ACTIVO:
                if (sacudida) {
                    activo = false;
                    timer.cancel();
                    emoji.setText("🏆");
                    texto.setText("¡Pescado!");
                    callback.onGano();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void detener() {
        activo = false;
        fase = Fase.INACTIVO;
        if (timer != null) timer.cancel();
        handler.removeCallbacksAndMessages(null);
    }
}