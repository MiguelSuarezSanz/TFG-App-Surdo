package com.example.mandosapp_surdo.minijuegos;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mandosapp_surdo.interfaces.Minijuego;
import com.example.mandosapp_surdo.interfaces.ResultadoCallback;
import java.util.Random;

public class DueloIrlandes implements Minijuego {

    // ---- Límite de tiempo total ----
    private static final long TIEMPO_LIMITE_MS = 15000;

    // ---- Umbrales de ángulo (pitch negativo = inclinado hacia delante) ----
    private static final float ANGULO_LENTO_MIN = 0f;
    private static final float ANGULO_LENTO_MAX = 15f;
    private static final float ANGULO_MEDIO_MAX = 40f;
    private static final float ANGULO_NORMAL_MAX = 45f;
    private static final float ANGULO_RAPIDO_MAX = 60f;
    // Más de 60 → pierde automáticamente

    // ---- Velocidades de vaciado (% por tick de 100ms) ----
    // Normal: 10% por segundo = 1% por tick de 100ms
    private static final float VEL_LENTO   = 0.25f; // 10% cada 4s  → 0.25% por tick
    private static final float VEL_MEDIO   = 0.50f; // 10% cada 2s  → 0.50% por tick
    private static final float VEL_NORMAL  = 1.00f; // 10% cada 1s  → 1.00% por tick
    private static final float VEL_RAPIDO  = 1.50f; // 15% cada 1s  → 1.50% por tick

    // ---- Probabilidad de derrame en zona rápida (30% por segundo = 3% por tick) ----
    private static final float PROB_DERRAME_POR_TICK = 0.03f;

    // ---- Vistas recibidas por constructor ----
    private final LinearLayout contenedor;
    private final View         vistaLiquido;   // bloque ámbar que se reduce
    private final TextView     txtPorcentaje;  // "🍺 75%"

    // ---- Estado ----
    private ResultadoCallback callback;
    private boolean activo = false;
    private float porcentaje = 100f;  // 0..100
    private float pitchActual = 0f;
    private CountDownTimer timerGlobal;
    private CountDownTimer timerTick;
    private final Random random = new Random();
    private int alturaMaxPx = 0;

    public DueloIrlandes(LinearLayout contenedor, View vistaLiquido, TextView txtPorcentaje) {
        this.contenedor = contenedor;
        this.vistaLiquido = vistaLiquido;
        this.txtPorcentaje = txtPorcentaje;
    }

    @Override
    public String getTitulo() { return "Duelo a la Irlandesa"; }

    @Override
    public String getIntroduccion() {
        return "No hay ninguna fiesta igual que el dia de San Patricio," +
                "¿Como que se celebra el 27 de marzo y no el 27 de mayo?";
    }

    @Override
    public String getExplicacion() {
        return "¡Inclina el móvil hacia delante como si bebieras de una jarra!" +
                "Vacía la cerveza antes de que se acabe el tiempo." +
                "Cuidado con inclinarlo demasiado, o derramarás la bebida.";
    }

    @Override
    public void iniciar(ResultadoCallback cb) {
        this.callback    = cb;
        this.activo      = true;
        this.porcentaje  = 100f;  // ← reinicia el porcentaje
        this.pitchActual = 0f;

        contenedor.setVisibility(View.VISIBLE);

        // Si ya tenemos la altura medida de una partida anterior, actualizamos directamente
        // Si no, esperamos a que la vista se mida por primera vez
        if (alturaMaxPx > 0) {
            actualizarVistaLiquido(); // ← fuerza el vaso lleno visualmente
        } else {
            vistaLiquido.post(() -> {
                alturaMaxPx = vistaLiquido.getHeight();
                actualizarVistaLiquido();
            });
        }

        // Esperar a que la vista esté medida para obtener su altura real
        vistaLiquido.post(() -> {
            alturaMaxPx = vistaLiquido.getHeight();
            actualizarVistaLiquido();
        });

        txtPorcentaje.setText("🍺 100%");

        // Timer global de 10 segundos
        timerGlobal = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
            @Override public void onTick(long ms) { }
            @Override public void onFinish() {
                if (activo) perder("⏰ ¡Se acabó el tiempo!");
            }
        }.start();

        // Tick cada 100ms para actualizar el vaciado
        timerTick = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
            @Override public void onTick(long ms) {
                if (activo) procesarTick();
            }
            @Override public void onFinish() {}
        }.start();
    }

    // Llamado desde onSensorChanged de la actividad con el pitch en grados
    public void onDatosGiro(float pitch) {
        if (!activo) return;
        // Usamos el valor absoluto del pitch negativo (inclinación hacia delante)
        pitchActual = pitch;
    }

    private void procesarTick() {
        // El pitch negativo indica inclinación hacia delante (beber)
        float angulo = pitchActual; // positivo cuando inclina hacia delante

        // ---- Zona prohibida: más de 60° → pierde ----
        if (angulo > ANGULO_RAPIDO_MAX) {
            perder("💦 ¡Lo has derramado todo!");
            return;
        }

        // ---- Zona sin beber: ángulo negativo o 0 (móvil hacia atrás o recto) ----
        if (angulo <= ANGULO_LENTO_MIN) {;
            return;
        }

        // ---- Determinar velocidad y posible derrame según zona ----
        float velocidad;

        if (angulo <= ANGULO_LENTO_MAX) {
            // 0–15°: velocidad lenta
            velocidad = VEL_LENTO;
            txtPorcentaje.setText("!!!BEBE, BEBE, BEBE!!!");
        } else if (angulo <= ANGULO_MEDIO_MAX) {
            // 15–40°: velocidad media
            velocidad = VEL_MEDIO;
            txtPorcentaje.setText("!!!BEBE, BEBE, BEBE!!!");
        } else if (angulo <= ANGULO_NORMAL_MAX) {
            // 40–45°: velocidad normal
            velocidad = VEL_NORMAL;
            txtPorcentaje.setText("!!!BEBE, BEBE, BEBE!!!");
        } else {
            // 45–60°: velocidad rápida con riesgo de derrame
            velocidad = VEL_RAPIDO;
            txtPorcentaje.setText("CUIDADO, VAS MUY RAPIDO");

            // 30% de probabilidad por segundo = 3% por tick de 100ms
            if (random.nextFloat() < PROB_DERRAME_POR_TICK) {
                perder("💦 ¡Se ha derramado la cerveza!");
                return;
            }
        }

        // ---- Reducir el porcentaje ----
        porcentaje -= velocidad;

        if (porcentaje <= 0f) {
            porcentaje = 0f;
            actualizarVistaLiquido();
            ganar();
            return;
        }

        actualizarVistaLiquido();
    }

    private void actualizarVistaLiquido() {
        if (alturaMaxPx == 0) return;

        // Altura proporcional al porcentaje restante
        int nuevaAltura = (int) (alturaMaxPx * (porcentaje / 100f));

        ViewGroup.LayoutParams params = vistaLiquido.getLayoutParams();
        params.height = Math.max(nuevaAltura, 0);
        vistaLiquido.setLayoutParams(params);

        txtPorcentaje.setText("🍺 " + (int) porcentaje + "%");
    }

    private void ganar() {
        activo = false;
        cancelarTimers();
        callback.onGano();
    }

    private void perder(String mensaje) {
        activo = false;
        cancelarTimers();
        callback.onPerdio();
    }

    private void cancelarTimers() {
        if (timerGlobal != null) timerGlobal.cancel();
        if (timerTick   != null) timerTick.cancel();
    }

    @Override
    public void detener() {
        activo = false;
        cancelarTimers();
        porcentaje = 100f;

        // Restaurar visualmente el vaso lleno para la próxima vez
        if (alturaMaxPx > 0) {
            ViewGroup.LayoutParams params = vistaLiquido.getLayoutParams();
            params.height = alturaMaxPx;
            vistaLiquido.setLayoutParams(params);
        }

        contenedor.setVisibility(View.GONE);
    }
}