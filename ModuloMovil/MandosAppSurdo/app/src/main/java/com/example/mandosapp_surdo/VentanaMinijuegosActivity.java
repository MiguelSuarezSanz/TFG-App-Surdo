package com.example.mandosapp_surdo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class VentanaMinijuegosActivity extends AppCompatActivity implements SensorEventListener {

    // =========================================================
    // INTERFAZ BASE — Cada minijuego la implementa
    // Para añadir un nuevo minijuego: crea una clase interna que
    // implemente Minijuego y añádela a crearListaMinijuegos()
    // =========================================================
    interface Minijuego {
        String getTitulo();
        String getExplicacion();
        void iniciar(ResultadoCallback callback);
        void detener();
    }

    interface ResultadoCallback {
        void onGano();
        void onPerdio();
    }

    // =========================================================
    // ESTADOS DE LA ACTIVIDAD
    // =========================================================
    private enum Estado { MENU, EXPLICACION, JUEGO, GAME_OVER }
    private Estado estadoActual = Estado.MENU;

    // =========================================================
    // VISTAS — Pantallas
    // =========================================================
    private View pantallaMenu;
    private View pantallaExplicacion;
    private View pantallaJuego;
    private View pantallaGameOver;

    // Menú
    private TextView txtMaxPuntuacion;

    // Explicación
    private TextView txtTituloMinijuego;
    private TextView txtExplicacion;
    private TextView txtPuntuacionActual;

    // Juego
    private TextView txtTemporizador;

    // Botones (minijuego botones)
    private LinearLayout contenedorBotones;
    private MaterialButton btn1, btn2, btn3;

    // Giroscopio
    private LinearLayout contenedorGiroscopio;
    private ImageView imagen;
    private TextView txtEstadoGiro;

    // Acelerómetro
    private LinearLayout contenedorAcelerometro;
    private TextView txtEmojiAccel;
    private TextView txtEstadoAccel;

    // Game Over
    private TextView txtPuntuacionFinal;
    private TextView txtMensajeRecord;

    // =========================================================
    // SENSORES
    // =========================================================
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometerSensor;

    // =========================================================
    // ESTADO DEL JUEGO
    // =========================================================
    private int puntuacion = 0;
    private int maxPuntuacion = 0;
    private static final String PREFS_NAME = "minijuegos_prefs";
    private static final String KEY_MAX = "max_puntuacion";

    private List<Minijuego> listaMinijuegos;
    private Minijuego minijuegoActual;
    private final Random random = new Random();
    private final Handler handler = new Handler();

    // =========================================================
    // MEDIA
    // =========================================================
    private MediaPlayer cancionGiroscopio;
    private MediaPlayer cancionSacudida;
    private MediaPlayer cancionMovimiento;

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ventana_minijuegos);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vincularVistas();
        inicializarSensores();
        inicializarMedia();
        cargarMaxPuntuacion();
        crearListaMinijuegos();
        mostrarPantalla(Estado.MENU);

        findViewById(R.id.btnJugar).setOnClickListener(v -> iniciarPartida());
        findViewById(R.id.btnListo).setOnClickListener(v -> iniciarMinijuegoActual());
    }

    @Override
    protected void onResume() {
        super.onResume();
        registrarSensoresSiNecesario();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (minijuegoActual != null) minijuegoActual.detener();
        pausarTodosLosMediaPlayers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liberarMediaPlayers();
        handler.removeCallbacksAndMessages(null);
    }

    // =========================================================
    // INICIALIZACIÓN
    // =========================================================

    private void vincularVistas() {
        pantallaMenu        = findViewById(R.id.pantallaMenu);
        pantallaExplicacion = findViewById(R.id.pantallaExplicacion);
        pantallaJuego       = findViewById(R.id.pantallaJuego);
        pantallaGameOver    = findViewById(R.id.pantallaGameOver);

        txtMaxPuntuacion    = findViewById(R.id.txtMaxPuntuacion);
        txtTituloMinijuego  = findViewById(R.id.txtTituloMinijuego);
        txtExplicacion      = findViewById(R.id.txtExplicacion);
        txtPuntuacionActual = findViewById(R.id.txtPuntuacionActual);
        txtTemporizador     = findViewById(R.id.txtTemporizador);

        contenedorBotones      = findViewById(R.id.contenedorBotones);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);

        contenedorGiroscopio = findViewById(R.id.contenedorGiroscopio);
        imagen               = findViewById(R.id.imagen);
        txtEstadoGiro        = findViewById(R.id.txtEstadoGiro);

        contenedorAcelerometro = findViewById(R.id.contenedorAcelerometro);
        txtEmojiAccel          = findViewById(R.id.txtEmojiAccel);
        txtEstadoAccel         = findViewById(R.id.txtEstadoAccel);

        txtPuntuacionFinal  = findViewById(R.id.txtPuntuacionFinal);
        txtMensajeRecord    = findViewById(R.id.txtMensajeRecord);
    }

    private void inicializarSensores() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor      = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void inicializarMedia() {
        cancionGiroscopio = MediaPlayer.create(this, R.raw.cancion);
        cancionSacudida   = MediaPlayer.create(this, R.raw.cancion);
        cancionMovimiento = MediaPlayer.create(this, R.raw.cancion);
    }

    private void cargarMaxPuntuacion() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        maxPuntuacion = prefs.getInt(KEY_MAX, 0);
        txtMaxPuntuacion.setText("Máxima puntuación: " + maxPuntuacion);
    }

    private void guardarMaxPuntuacion() {
        if (puntuacion > maxPuntuacion) {
            maxPuntuacion = puntuacion;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putInt(KEY_MAX, maxPuntuacion).apply();
        }
    }

    // =========================================================
    // LISTA DE MINIJUEGOS
    // Para añadir uno nuevo: instancialo aquí y añádelo a la lista
    // =========================================================
    private void crearListaMinijuegos() {
        listaMinijuegos = new ArrayList<>();
        listaMinijuegos.add(new MinijuegoBotones());
        listaMinijuegos.add(new MiniJuegoGiroscopio());
        listaMinijuegos.add(new MiniJuegoSacudida());
        listaMinijuegos.add(new MiniJuegoMovimiento());
    }

    // =========================================================
    // FLUJO DEL JUEGO
    // =========================================================

    private void iniciarPartida() {
        puntuacion = 0;
        siguienteMinijuego();
    }

    private void siguienteMinijuego() {
        // Elegir un minijuego al azar
        minijuegoActual = listaMinijuegos.get(random.nextInt(listaMinijuegos.size()));
        mostrarPantalla(Estado.EXPLICACION);

        txtTituloMinijuego.setText(minijuegoActual.getTitulo());
        txtExplicacion.setText(minijuegoActual.getExplicacion());
        txtPuntuacionActual.setText("Puntuación: " + puntuacion);
    }

    private void iniciarMinijuegoActual() {
        mostrarPantalla(Estado.JUEGO);
        registrarSensoresSiNecesario();

        minijuegoActual.iniciar(new ResultadoCallback() {
            @Override
            public void onGano() {
                runOnUiThread(() -> {
                    puntuacion++;
                    minijuegoActual.detener();
                    ocultarTodosLosContenedoresJuego();
                    // Pequeña pausa antes del siguiente minijuego
                    handler.postDelayed(() -> siguienteMinijuego(), 800);
                });
            }

            @Override
            public void onPerdio() {
                runOnUiThread(() -> {
                    minijuegoActual.detener();
                    ocultarTodosLosContenedoresJuego();
                    mostrarGameOver();
                });
            }
        });
    }

    private void mostrarGameOver() {
        guardarMaxPuntuacion();
        mostrarPantalla(Estado.GAME_OVER);
        pausarTodosLosMediaPlayers();

        txtPuntuacionFinal.setText("Puntuación: " + puntuacion);
        txtMensajeRecord.setText(puntuacion >= maxPuntuacion && puntuacion > 0
                ? "🏆 ¡Nuevo récord!" : "");

        // Volver al menú automáticamente tras 3 segundos
        handler.postDelayed(() -> {
            txtMaxPuntuacion.setText("Máxima puntuación: " + maxPuntuacion);
            mostrarPantalla(Estado.MENU);
        }, 3000);
    }

    // =========================================================
    // GESTIÓN DE PANTALLAS Y CONTENEDORES
    // =========================================================

    private void mostrarPantalla(Estado estado) {
        estadoActual = estado;
        pantallaMenu.setVisibility(estado == Estado.MENU ? View.VISIBLE : View.GONE);
        pantallaExplicacion.setVisibility(estado == Estado.EXPLICACION ? View.VISIBLE : View.GONE);
        pantallaJuego.setVisibility(estado == Estado.JUEGO ? View.VISIBLE : View.GONE);
        pantallaGameOver.setVisibility(estado == Estado.GAME_OVER ? View.VISIBLE : View.GONE);
    }

    private void ocultarTodosLosContenedoresJuego() {
        contenedorBotones.setVisibility(View.GONE);
        contenedorGiroscopio.setVisibility(View.GONE);
        contenedorAcelerometro.setVisibility(View.GONE);
        txtTemporizador.setText("");
        imagen.setVisibility(View.INVISIBLE);
    }

    // =========================================================
    // SENSORES — Callback compartido entre minijuegos
    // =========================================================

    private void registrarSensoresSiNecesario() {
        if (sensorManager == null) return;
        sensorManager.unregisterListener(this);
        // Solo registramos si estamos en fase de juego
        if (estadoActual == Estado.JUEGO) {
            if (rotationSensor != null)
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            if (accelerometerSensor != null)
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    // Variables de sensor compartidas entre minijuegos de sensores
    private final float[] rotationMatrix   = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] gravity           = new float[3];
    private static final float ALPHA        = 0.8f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (minijuegoActual == null || estadoActual != Estado.JUEGO) return;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            float pitch = (float) Math.toDegrees(orientationAngles[1]);
            float roll  = (float) Math.toDegrees(orientationAngles[2]);
            if (minijuegoActual instanceof MiniJuegoGiroscopio)
                ((MiniJuegoGiroscopio) minijuegoActual).onDatosGiro(pitch, roll);
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
            gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
            gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];
            float lx = event.values[0] - gravity[0];
            float ly = event.values[1] - gravity[1];
            float lz = event.values[2] - gravity[2];
            if (minijuegoActual instanceof MiniJuegoSacudida)
                ((MiniJuegoSacudida) minijuegoActual).onDatosAcel(lx, ly, lz);
            if (minijuegoActual instanceof MiniJuegoMovimiento)
                ((MiniJuegoMovimiento) minijuegoActual).onDatosAcel(ly);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // =========================================================
    // MEDIA — helpers
    // =========================================================

    private void pausarTodosLosMediaPlayers() {
        pausarSi(cancionGiroscopio);
        pausarSi(cancionSacudida);
        pausarSi(cancionMovimiento);
    }

    private void pausarSi(MediaPlayer mp) {
        if (mp != null && mp.isPlaying()) mp.pause();
    }

    private void liberarMediaPlayers() {
        if (cancionGiroscopio != null) { cancionGiroscopio.release(); cancionGiroscopio = null; }
        if (cancionSacudida   != null) { cancionSacudida.release();   cancionSacudida   = null; }
        if (cancionMovimiento != null) { cancionMovimiento.release(); cancionMovimiento = null; }
    }

    // =========================================================
    //
    //  MINIJUEGO 1 — BOTONES DE COLORES
    //  Falla si pulsas el botón incorrecto.
    //  Gana si pulsas el correcto.
    //
    // =========================================================
    private class MinijuegoBotones implements Minijuego {

        private final String[] COLORES_NOMBRES = {"Rojo", "Amarillo", "Verde", "Azul"};
        private final int[]    COLORES_INT      = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE};

        private ResultadoCallback callback;
        private boolean activo = false;

        @Override public String getTitulo() { return "🎨 Botones de colores"; }

        @Override
        public String getExplicacion() {
            return "Pulsa el botón cuyo COLOR de fondo coincida con el COLOR indicado por el texto del botón del medio.\n\n¡Un fallo y pierdes!";
        }

        @Override
        public void iniciar(ResultadoCallback cb) {
            this.callback = cb;
            this.activo   = true;

            contenedorBotones.setVisibility(View.VISIBLE);

            // Elegir color objetivo al azar
            int idxObjetivo = random.nextInt(COLORES_NOMBRES.length);
            String nombreObjetivo = COLORES_NOMBRES[idxObjetivo];
            int colorObjetivo     = COLORES_INT[idxObjetivo];

            // Asignar colores de fondo a los 3 botones (uno es el correcto)
            List<Integer> indices = new ArrayList<>();
            indices.add(idxObjetivo);
            int otro1, otro2;
            do { otro1 = random.nextInt(COLORES_NOMBRES.length); } while (otro1 == idxObjetivo);
            do { otro2 = random.nextInt(COLORES_NOMBRES.length); } while (otro2 == idxObjetivo || otro2 == otro1);
            indices.add(otro1);
            indices.add(otro2);
            Collections.shuffle(indices);

            int colorBtn1 = COLORES_INT[indices.get(0)];
            int colorBtn2 = COLORES_INT[indices.get(1)];
            int colorBtn3 = COLORES_INT[indices.get(2)];

            btn1.setBackgroundTintList(ColorStateList.valueOf(colorBtn1));
            btn2.setBackgroundTintList(ColorStateList.valueOf(colorBtn2));
            btn3.setBackgroundTintList(ColorStateList.valueOf(colorBtn3));

            btn1.setText(""); btn2.setText(""); btn3.setText("");

            // El botón del medio muestra el nombre del color a buscar en color de texto
            btn2.setText(nombreObjetivo);
            btn2.setTextColor(colorObjetivo);

            // Listener: correcto = fondo coincide con colorObjetivo
            btn1.setOnClickListener(v -> { if (activo) evaluar(colorBtn1, colorObjetivo); });
            btn2.setOnClickListener(v -> { if (activo) evaluar(colorBtn2, colorObjetivo); });
            btn3.setOnClickListener(v -> { if (activo) evaluar(colorBtn3, colorObjetivo); });
        }

        private void evaluar(int colorPulsado, int colorObjetivo) {
            activo = false;
            if (colorPulsado == colorObjetivo) callback.onGano();
            else                               callback.onPerdio();
        }

        @Override
        public void detener() {
            activo = false;
            btn1.setOnClickListener(null);
            btn2.setOnClickListener(null);
            btn3.setOnClickListener(null);
        }
    }

    // =========================================================
    //
    //  MINIJUEGO 2 — GIROSCOPIO
    //  Pon el móvil horizontal sobre su lateral en 5 segundos.
    //  Mantenerlo 1.5s seguidos = victoria. Tiempo agotado = derrota.
    //
    // =========================================================
    private class MiniJuegoGiroscopio implements Minijuego {

        private static final long TIEMPO_LIMITE_MS   = 5000;
        private static final long TIEMPO_MANTENER_MS = 1500;

        private ResultadoCallback callback;
        private CountDownTimer timerGlobal;
        private long tiempoEnPosicion = 0;
        private boolean enPosicion    = false;
        private boolean activo        = false;

        @Override public String getTitulo() { return "📱 Giroscopio"; }

        @Override
        public String getExplicacion() {
            return "Pon el móvil HORIZONTAL apoyado sobre uno de sus laterales y mantenlo así durante 1,5 segundos.\n\n¡Tienes 5 segundos!";
        }

        @Override
        public void iniciar(ResultadoCallback cb) {
            this.callback        = cb;
            this.activo          = true;
            this.tiempoEnPosicion = 0;
            this.enPosicion      = false;

            contenedorGiroscopio.setVisibility(View.VISIBLE);
            imagen.setVisibility(View.INVISIBLE);
            txtEstadoGiro.setText("Inclina el móvil sobre su lateral...");
            pausarSi(cancionGiroscopio);

            timerGlobal = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
                @Override public void onTick(long ms) {
                    txtTemporizador.setText("⏱ " + (ms / 1000 + 1) + "s");
                }
                @Override public void onFinish() {
                    if (activo) { activo = false; callback.onPerdio(); }
                }
            }.start();
        }

        // Llamado desde onSensorChanged de la actividad
        void onDatosGiro(float pitch, float roll) {
            if (!activo) return;

            boolean posicionCorrecta = (Math.abs(roll) > 70f) && (Math.abs(pitch) < 20f);

            if (posicionCorrecta) {
                if (!enPosicion) {
                    enPosicion        = true;
                    tiempoEnPosicion  = System.currentTimeMillis();
                }
                txtEstadoGiro.setText("✅ ¡Bien! Mantén...");
                imagen.setVisibility(View.VISIBLE);
                if (cancionGiroscopio != null && !cancionGiroscopio.isPlaying()) {
                    cancionGiroscopio.setLooping(true);
                    cancionGiroscopio.start();
                }
                // ¿Lleva suficiente tiempo en posición?
                if (System.currentTimeMillis() - tiempoEnPosicion >= TIEMPO_MANTENER_MS) {
                    activo = false;
                    timerGlobal.cancel();
                    callback.onGano();
                }
            } else {
                enPosicion = false;
                imagen.setVisibility(View.INVISIBLE);
                txtEstadoGiro.setText("Inclina el móvil sobre su lateral...");
                pausarSi(cancionGiroscopio);
            }
        }

        @Override
        public void detener() {
            activo = false;
            if (timerGlobal != null) timerGlobal.cancel();
            pausarSi(cancionGiroscopio);
            imagen.setVisibility(View.INVISIBLE);
        }
    }

    // =========================================================
    //
    //  MINIJUEGO 3 — SACUDIDA
    //  Sacude el móvil en 4 segundos.
    //
    // =========================================================
    private class MiniJuegoSacudida implements Minijuego {

        private static final long  TIEMPO_LIMITE_MS = 4000;
        private static final float SHAKE_THRESHOLD  = 25.0f;

        private ResultadoCallback callback;
        private CountDownTimer timer;
        private boolean activo = false;

        @Override public String getTitulo() { return "🤝 ¡Sacude!"; }

        @Override
        public String getExplicacion() {
            return "¡Sacude el móvil con fuerza en 4 segundos!\n\n¡Dale fuerte!";
        }

        @Override
        public void iniciar(ResultadoCallback cb) {
            this.callback = cb;
            this.activo   = true;

            contenedorAcelerometro.setVisibility(View.VISIBLE);
            txtEmojiAccel.setText("📱");
            txtEstadoAccel.setText("¡Sacude el móvil!");

            timer = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
                @Override public void onTick(long ms) {
                    txtTemporizador.setText("⏱ " + (ms / 1000 + 1) + "s");
                }
                @Override public void onFinish() {
                    if (activo) { activo = false; callback.onPerdio(); }
                }
            }.start();
        }

        void onDatosAcel(float lx, float ly, float lz) {
            if (!activo) return;
            float magnitud = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
            if (magnitud > SHAKE_THRESHOLD) {
                activo = false;
                timer.cancel();
                txtEmojiAccel.setText("💥");
                if (cancionSacudida != null) {
                    cancionSacudida.setLooping(false);
                    cancionSacudida.start();
                }
                callback.onGano();
            }
        }

        @Override
        public void detener() {
            activo = false;
            if (timer != null) timer.cancel();
            pausarSi(cancionSacudida);
        }
    }

    // =========================================================
    //
    //  MINIJUEGO 4 — MOVIMIENTO ADELANTE/ATRÁS
    //  Mueve el móvil hacia adelante y atrás repetidamente
    //  (al menos 4 cambios de dirección) en 5 segundos.
    //
    // =========================================================
    private class MiniJuegoMovimiento implements Minijuego {

        private static final long  TIEMPO_LIMITE_MS  = 5000;
        private static final float MOVE_THRESHOLD     = 12.0f;
        private static final int   CAMBIOS_NECESARIOS = 4;

        private ResultadoCallback callback;
        private CountDownTimer timer;
        private boolean activo    = false;
        private int     cambios   = 0;
        private float   ultimoEje = 0;

        @Override public String getTitulo() { return "↔️ ¡Muévelo!"; }

        @Override
        public String getExplicacion() {
            return "Mueve el móvil hacia adelante y hacia atrás repetidamente en 5 segundos.\n\n¡Necesitas al menos 4 movimientos seguidos!";
        }

        @Override
        public void iniciar(ResultadoCallback cb) {
            this.callback = cb;
            this.activo   = true;
            this.cambios  = 0;
            this.ultimoEje = 0;

            contenedorAcelerometro.setVisibility(View.VISIBLE);
            txtEmojiAccel.setText("↔️");
            txtEstadoAccel.setText("Movimientos: 0 / " + CAMBIOS_NECESARIOS);

            timer = new CountDownTimer(TIEMPO_LIMITE_MS, 100) {
                @Override public void onTick(long ms) {
                    txtTemporizador.setText("⏱ " + (ms / 1000 + 1) + "s");
                }
                @Override public void onFinish() {
                    if (activo) { activo = false; callback.onPerdio(); }
                }
            }.start();
        }

        void onDatosAcel(float ejeY) {
            if (!activo) return;

            if ((ultimoEje > MOVE_THRESHOLD && ejeY < -MOVE_THRESHOLD) ||
                    (ultimoEje < -MOVE_THRESHOLD && ejeY > MOVE_THRESHOLD)) {
                cambios++;
                ultimoEje = ejeY;
                txtEstadoAccel.setText("Movimientos: " + cambios + " / " + CAMBIOS_NECESARIOS);

                if (cambios >= CAMBIOS_NECESARIOS) {
                    activo = false;
                    timer.cancel();
                    if (cancionMovimiento != null) {
                        cancionMovimiento.setLooping(false);
                        cancionMovimiento.start();
                    }
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
            pausarSi(cancionMovimiento);
        }
    }
}