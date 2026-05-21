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
    private final float SHAKE_THRESHOLD  = 28.0f;
    private final long  TIEMPO_LANZAR_MS = 5000;
    private final long  TIEMPO_ESPERA_MS = 15000;
    private final long  TIEMPO_SACAR_MS  = 3000;
    private static final long ESPERA_TRAS_LANZAR_MS = 800;
    private long tiempoLanzamiento = 0;
    private final Random random = new Random();
    private ResultadoCallback callback;
    private Fase fase = Fase.INACTIVO;
    private CountDownTimer timer;
    private boolean activo = false;
    private LinearLayout contenedor;
    private TextView emoji;
    private TextView texto;
    private Handler handler;

    public DiaPesca(LinearLayout contenedor, TextView emoji, TextView texto, Handler handler) {
        this.contenedor = contenedor;
        this.emoji = emoji;
        this.texto = texto;
        this.handler = handler;
    }

    @Override
    public String getTitulo() {
        return "Un Dia de Pesca";
    }

    @Override
    public String getIntroduccion() {
        return "Ahh, el mar, un sitio magnifico ¿verdad?, porque no te relajas," +
                "\n\nCojes esa caña de ahi y te pones a pescar";
    }

    @Override
    public String getExplicacion() {
        return "Primero agita el movil para lanzar la caña, espera a que el pez pique y cuando lo " +
                "haga, vuelve a agitar el movil para pescarlo, si lo pescas a tiempo ganas, si lo " +
                "ahuyentas, pierdes";
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
            @Override public void onTick(long ms) { }
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
        tiempoLanzamiento = System.currentTimeMillis(); // ← añadir esto
        fase = Fase.ESPERAR;
        timer.cancel();
        emoji.setText("🌊");
        texto.setText("El cebo está en el agua... ¡espera!");

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
            @Override public void onTick(long ms) { }
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
                if (sacudida) iniciarFaseEspera();
                break;

            case ESPERAR:
                if (sacudida) {
                    if (System.currentTimeMillis() - tiempoLanzamiento < ESPERA_TRAS_LANZAR_MS) return;
                    activo = false;
                    timer.cancel();
                    handler.removeCallbacksAndMessages(null);
                    callback.onPerdio();
                }
                break;

            case PEZ_ACTIVO:
                if (sacudida) {
                    activo = false;
                    timer.cancel();
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