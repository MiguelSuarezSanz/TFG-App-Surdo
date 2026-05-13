package com.example.mandosapp_surdo.minijuegos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.example.mandosapp_surdo.Minijuego;
import com.example.mandosapp_surdo.ResultadoCallback;
import com.example.mandosapp_surdo.VentanaMinijuegosActivity;
import com.example.mandosapp_surdo.enume.Figura;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Handler;

public class DibujaLaFigura implements Minijuego {
    private static final int  NUM_FIGURAS = 3;
    private static final long  TIEMPO_FIGURA_MS = 5000;

    // Un cambio de dirección > este ángulo (grados) cuenta como esquina
    private static final float UMBRAL_ESQUINA   = 55f;
    private final List<float[]> trayectoria = new ArrayList<>();
    private ResultadoCallback callback;
    private boolean activo      = false;
    private int figuraActualIdx = 0;
    private List<Figura> secuencia;
    private float lastYaw = Float.NaN, lastPitch = Float.NaN;
    private CountDownTimer timerFigura;
    private FrameLayout contenedor;
    private FiguraView figuraView;
    private TextView figura;
    private TextView estado;
    private TextView resultado;
    private Handler handler;
    private TextView tiempo;

    public DibujaLaFigura(FrameLayout contenedor, TextView figura, TextView estado, TextView resultado, TextView tiempo, Handler handler) {
        this.contenedor = contenedor;
        this.figura = figura;
        this.estado = estado;
        this.resultado = resultado;
        this.tiempo = tiempo;
        this.handler = handler;
        this.figuraView = new FiguraView(this.contenedor.getContext());
        this.contenedor.addView(this.figuraView, 0);
    }

    @Override public String getTitulo() { return "✏️ Dibuja en el aire"; }

    @Override
    public String getExplicacion() {
        return "¡Usa el móvil como un pincel en el aire!\n\n" +
                "Se mostrarán 3 figuras en secuencia. Tienes 5 segundos para dibujar cada una.\n\n" +
                "Figuras posibles:\n○ Círculo  □ Cuadrado  △ Triángulo  ★ Estrella  ⬡ Hexágono";
    }

    @Override
    public void iniciar(ResultadoCallback cb) {
        this.callback      = cb;
        this.activo        = true;
        this.figuraActualIdx = 0;

        // Elegir 3 figuras distintas al azar
        List<Figura> pool = new ArrayList<>(List.of(Figura.values()));
        Collections.shuffle(pool);
        secuencia = pool.subList(0, NUM_FIGURAS);
        contenedor.setVisibility(View.VISIBLE);
        iniciarFigura();
    }

    private void iniciarFigura() {
        Figura f = secuencia.get(figuraActualIdx);
        trayectoria.clear();
        lastYaw   = Float.NaN;
        lastPitch = Float.NaN;
        figuraView.limpiar();
        figura.setText("Dibuja: " + f.nombre);
        estado.setText("Figura " + (figuraActualIdx + 1) + " de " + NUM_FIGURAS + "\n" + f.pista);
        resultado.setText("");

        if (timerFigura != null) timerFigura.cancel();
        timerFigura = new CountDownTimer(TIEMPO_FIGURA_MS, 50) {
            @Override public void onTick(long ms) {
                tiempo.setText("⏱ " + (ms / 1000 + 1) + "s");
            }
            @Override public void onFinish() {
                if (activo) evaluarFigura(secuencia.get(figuraActualIdx));
            }
        }.start();
    }

    // Recibe pitch y yaw en grados desde onSensorChanged
    public void onDatosGiro(float pitch, float yaw) {
        if (!activo) return;
        if (Float.isNaN(lastYaw)) { lastYaw = yaw; lastPitch = pitch; return; }

        float dYaw   = yaw   - lastYaw;
        float dPitch = pitch - lastPitch;

        if (Math.abs(dYaw) > 1.5f || Math.abs(dPitch) > 1.5f) {
            trayectoria.add(new float[]{yaw, pitch});
            figuraView.agregarPunto(yaw, pitch);
            lastYaw   = yaw;
            lastPitch = pitch;
        }
    }

    private void evaluarFigura(Figura esperada) {
        if (trayectoria.size() < 25) {
            falloFigura("❌ ¡Dibuja algo!");
            return;
        }

        int esquinas = contarEsquinas();
        boolean cerrada = esTrayectoriaCerrada();
        float varianza = calcularVarianzaAngular();
        float ratio = calcularRatioAspecto();
        boolean correcto = false;

        switch (esperada) {
            case CIRCULO:
                // Pocos cambios bruscos, cerrada, varianza alta (curva suave y continua)
                correcto = esquinas <= 2 && cerrada && varianza > 8f;
                break;
            case CUADRADO:
                // 3–5 esquinas, cerrada, ratio aspecto ≈ 1 (no demasiado alargado)
                correcto = esquinas >= 3 && esquinas <= 5 && cerrada
                        && ratio > 0.5f && ratio < 2.0f;
                break;
            case TRIANGULO:
                // 2–4 esquinas bruscas, cerrada
                correcto = esquinas >= 2 && esquinas <= 4 && cerrada;
                break;
            case ESTRELLA:
                // Muchos cambios de dirección alternando (≥5), no necesariamente cerrada
                correcto = esquinas >= 5;
                break;
            case HEXAGONO:
                // 5–7 esquinas, cerrada
                correcto = esquinas >= 5 && esquinas <= 7 && cerrada;
                break;
        }

        if (correcto) exitoFigura();
        else          falloFigura("❌ Figura incorrecta (" + esquinas + " esquinas)");
    }

    private void exitoFigura() {
        resultado.setText("✅ ¡Correcto!");
        figuraActualIdx++;
        if (figuraActualIdx >= NUM_FIGURAS) {
            handler.postDelayed(() -> { if (activo) { activo = false; callback.onGano(); } }, 700);
        } else {
            handler.postDelayed(() -> { if (activo) iniciarFigura(); }, 1200);
        }
    }

    private void falloFigura(String msg) {
        activo = false;
        if (timerFigura != null) timerFigura.cancel();
        resultado.setText(msg);
        handler.postDelayed(callback::onPerdio, 800);
    }

    // ---- Análisis de la trayectoria ----

    /** Cuenta cambios bruscos de dirección (esquinas) */
    private int contarEsquinas() {
        if (trayectoria.size() < 3) return 0;
        int esquinas = 0;
        for (int i = 1; i < trayectoria.size() - 1; i++) {
            float[] p = trayectoria.get(i - 1);
            float[] c = trayectoria.get(i);
            float[] n = trayectoria.get(i + 1);
            float ang1 = (float) Math.toDegrees(Math.atan2(c[1] - p[1], c[0] - p[0]));
            float ang2 = (float) Math.toDegrees(Math.atan2(n[1] - c[1], n[0] - c[0]));
            float diff = Math.abs(ang2 - ang1);
            if (diff > 180) diff = 360 - diff;
            if (diff > UMBRAL_ESQUINA) esquinas++;
        }
        return esquinas;
    }

    /** La trayectoria es cerrada si el último punto está cerca del primero
     *  en relación al rango total del dibujo */
    private boolean esTrayectoriaCerrada() {
        if (trayectoria.size() < 5) return false;
        float[] ini = trayectoria.get(0);
        float[] fin = trayectoria.get(trayectoria.size() - 1);
        float rango = rangoTotal();
        float dist  = (float) Math.sqrt(
                Math.pow(fin[0] - ini[0], 2) + Math.pow(fin[1] - ini[1], 2));
        return dist < rango * 0.4f;
    }

    /** Varianza del ángulo entre segmentos → qué tan curva y uniforme es la trayectoria */
    private float calcularVarianzaAngular() {
        if (trayectoria.size() < 3) return 0;
        List<Float> angulos = new ArrayList<>();
        for (int i = 1; i < trayectoria.size(); i++) {
            float[] p = trayectoria.get(i - 1);
            float[] c = trayectoria.get(i);
            angulos.add((float) Math.toDegrees(Math.atan2(c[1] - p[1], c[0] - p[0])));
        }
        float media = 0;
        for (float a : angulos) media += a;
        media /= angulos.size();
        float varianza = 0;
        for (float a : angulos) varianza += (a - media) * (a - media);
        return varianza / angulos.size();
    }

    /** Ratio ancho/alto de la bounding box */
    private float calcularRatioAspecto() {
        if (trayectoria.isEmpty()) return 1f;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] p : trayectoria) {
            if (p[0] < minX) minX = p[0]; if (p[0] > maxX) maxX = p[0];
            if (p[1] < minY) minY = p[1]; if (p[1] > maxY) maxY = p[1];
        }
        float h = maxY - minY;
        return h == 0 ? 1f : (maxX - minX) / h;
    }

    /** Diagonal de la bounding box (magnitud del dibujo) */
    private float rangoTotal() {
        if (trayectoria.isEmpty()) return 1f;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] p : trayectoria) {
            if (p[0] < minX) minX = p[0]; if (p[0] > maxX) maxX = p[0];
            if (p[1] < minY) minY = p[1]; if (p[1] > maxY) maxY = p[1];
        }
        return (float) Math.sqrt(Math.pow(maxX - minX, 2) + Math.pow(maxY - minY, 2));
    }

    @Override
    public void detener() {
        activo = false;
        if (timerFigura != null) timerFigura.cancel();
        trayectoria.clear();
        figuraView.limpiar();
    }

    class FiguraView extends View {

        private final Paint paintLinea = new Paint();
        private final Paint paintDot = new Paint();
        private final Path path = new Path();

        private final List<float[]> puntos = new ArrayList<>();
        private float minX, maxX, minY, maxY;

        FiguraView(Context ctx) {
            super(ctx);
            limpiar();

            paintLinea.setColor(Color.parseColor("#6C63FF"));
            paintLinea.setStyle(Paint.Style.STROKE);
            paintLinea.setStrokeWidth(7f);
            paintLinea.setStrokeCap(Paint.Cap.ROUND);
            paintLinea.setStrokeJoin(Paint.Join.ROUND);
            paintLinea.setAntiAlias(true);

            paintDot.setStyle(Paint.Style.FILL);
            paintDot.setAntiAlias(true);
        }

        void agregarPunto(float yaw, float pitch) {
            puntos.add(new float[]{yaw, pitch});
            if (yaw   < minX) minX = yaw;   if (yaw   > maxX) maxX = yaw;
            if (pitch < minY) minY = pitch;  if (pitch > maxY) maxY = pitch;
            postInvalidate();
        }

        void limpiar() {
            puntos.clear();
            path.reset();
            minX = Float.MAX_VALUE;  maxX = -Float.MAX_VALUE;
            minY = Float.MAX_VALUE;  maxY = -Float.MAX_VALUE;
            postInvalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (puntos.size() < 2) return;

            int   w = getWidth();
            int   h = getHeight();
            float margen = 40f;
            float rangoX = (maxX == minX) ? 1 : maxX - minX;
            float rangoY = (maxY == minY) ? 1 : maxY - minY;

            path.reset();
            for (int i = 0; i < puntos.size(); i++) {
                float px = margen + ((puntos.get(i)[0] - minX) / rangoX) * (w - 2 * margen);
                float py = margen + ((puntos.get(i)[1] - minY) / rangoY) * (h - 2 * margen);
                if (i == 0) path.moveTo(px, py);
                else        path.lineTo(px, py);
            }
            canvas.drawPath(path, paintLinea);

            // Punto de inicio (blanco) y punto final (verde)
            float[] ini = puntos.get(0);
            float[] fin = puntos.get(puntos.size() - 1);
            float pxi = margen + ((ini[0] - minX) / rangoX) * (w - 2 * margen);
            float pyi = margen + ((ini[1] - minY) / rangoY) * (h - 2 * margen);
            float pxf = margen + ((fin[0] - minX) / rangoX) * (w - 2 * margen);
            float pyf = margen + ((fin[1] - minY) / rangoY) * (h - 2 * margen);

            paintDot.setColor(Color.WHITE);
            canvas.drawCircle(pxi, pyi, 12f, paintDot);
            paintDot.setColor(Color.parseColor("#43A047"));
            canvas.drawCircle(pxf, pyf, 12f, paintDot);
        }
    }
}