package com.example.mandosapp_surdo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.mandosapp_surdo.Minijuego;
import com.example.mandosapp_surdo.ResultadoCallback;
import com.example.mandosapp_surdo.minijuegos.DueloAMediodia;
import com.example.mandosapp_surdo.minijuegos.ElijeElBoton;
import com.example.mandosapp_surdo.minijuegos.SimpleBoton;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class VentanaMinijuegosActivity extends AppCompatActivity implements SensorEventListener {

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

    // Juego — compartido
    private TextView txtTemporizador;

    // Minijuego: Un Simple Boton
    private LinearLayout contenedorBoton;
    private MaterialButton btnSimple;
    private TextView instruccionesBtnSimple;

    // Minijuego: Botones
    private LinearLayout contenedorBotones;
    private MaterialButton btn1, btn2, btn3;

    // Minijuego: Giroscopio
    private LinearLayout contenedorGiroscopio;
    private ImageView imagen;
    private TextView txtEstadoGiro;

    // Minijuego: Acelerómetro (movimiento)
    private LinearLayout contenedorAcelerometro;
    private TextView txtEmojiAccel;
    private TextView txtEstadoAccel;

    // Minijuego: Pesca
    private LinearLayout contenedorPesca;
    private TextView txtEmojiPesca;
    private TextView txtEstadoPesca;

    // Minijuego: Dibujo de figuras
    private FrameLayout contenedorDibujo;
    private FiguraView figuraView;
    private TextView txtFiguraObjetivo;
    private TextView txtEstadoDibujo;
    private TextView txtResultadoDibujo;

    // Game Over
    private TextView txtPuntuacionFinal;
    private TextView txtMensajeRecord;

    // =========================================================
    // SENSORES
    // =========================================================
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometerSensor;

    // Datos de sensor compartidos
    private final float[] rotationMatrix    = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] gravity           = new float[3];
    private static final float ALPHA        = 0.8f;

    // =========================================================
    // ESTADO DEL JUEGO
    // =========================================================
    private int puntuacion    = 0;
    private int maxPuntuacion = 0;
    private static final String PREFS_NAME = "minijuegos_prefs";
    private static final String KEY_MAX    = "max_puntuacion";

    private List<Minijuego> listaMinijuegos;
    private Minijuego minijuegoActual;
    private final Random  random  = new Random();
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

        // Minijuego: Elije el Boton
        contenedorBotones = findViewById(R.id.contenedorBotones);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);

        // Minijuego: Un Simple Boton
        contenedorBoton = findViewById(R.id.contenedorSimpleBoton);
        btnSimple = findViewById(R.id.botonSimple);
        instruccionesBtnSimple = findViewById(R.id.txtInstruccionSimpleBoton);

        contenedorGiroscopio = findViewById(R.id.contenedorGiroscopio);
        imagen               = findViewById(R.id.imagen);
        txtEstadoGiro        = findViewById(R.id.txtEstadoGiro);

        contenedorAcelerometro = findViewById(R.id.contenedorAcelerometro);
        txtEmojiAccel          = findViewById(R.id.txtEmojiAccel);
        txtEstadoAccel         = findViewById(R.id.txtEstadoAccel);

        contenedorPesca  = findViewById(R.id.contenedorPesca);
        txtEmojiPesca    = findViewById(R.id.txtEmojiPesca);
        txtEstadoPesca   = findViewById(R.id.txtEstadoPesca);

        contenedorDibujo   = findViewById(R.id.contenedorDibujo);
        txtFiguraObjetivo  = findViewById(R.id.txtFiguraObjetivo);
        txtEstadoDibujo    = findViewById(R.id.txtEstadoDibujo);
        txtResultadoDibujo = findViewById(R.id.txtResultadoDibujo);

        // FiguraView se crea por código y se inserta al fondo del contenedor
        figuraView = new FiguraView(this);
        contenedorDibujo.addView(figuraView, 0);

        txtPuntuacionFinal = findViewById(R.id.txtPuntuacionFinal);
        txtMensajeRecord   = findViewById(R.id.txtMensajeRecord);
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
    // Para añadir uno nuevo: instancialo aquí y añádelo a la lista.
    // =========================================================
    private void crearListaMinijuegos() {
        listaMinijuegos = new ArrayList<>();
        listaMinijuegos.add(new ElijeElBoton(contenedorBotones, btn1, btn2, btn3));
        listaMinijuegos.add(new DueloAMediodia(contenedorGiroscopio, imagen, txtEstadoGiro, txtTemporizador));
        listaMinijuegos.add(new SimpleBoton(contenedorBoton, btnSimple, instruccionesBtnSimple, txtTemporizador));
        listaMinijuegos.add(new MiniJuegoMovimiento());
        listaMinijuegos.add(new MiniJuegoPesca());           // NUEVO
        listaMinijuegos.add(new MiniJuegoDibujoFiguras());   // NUEVO*/
    }

    // =========================================================
    // FLUJO DEL JUEGO
    // =========================================================

    private void iniciarPartida() {
        puntuacion = 0;
        siguienteMinijuego();
    }

    private void siguienteMinijuego() {
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
            @Override public void onGano() {
                runOnUiThread(() -> {
                    puntuacion++;
                    minijuegoActual.detener();
                    ocultarTodosLosContenedoresJuego();
                    handler.postDelayed(() -> siguienteMinijuego(), 800);
                });
            }
            @Override public void onPerdio() {
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
        pantallaMenu.setVisibility(        estado == Estado.MENU        ? View.VISIBLE : View.GONE);
        pantallaExplicacion.setVisibility( estado == Estado.EXPLICACION ? View.VISIBLE : View.GONE);
        pantallaJuego.setVisibility(       estado == Estado.JUEGO       ? View.VISIBLE : View.GONE);
        pantallaGameOver.setVisibility(    estado == Estado.GAME_OVER   ? View.VISIBLE : View.GONE);
    }

    private void ocultarTodosLosContenedoresJuego() {
        contenedorBotones.setVisibility(View.GONE);
        contenedorGiroscopio.setVisibility(View.GONE);
        contenedorAcelerometro.setVisibility(View.GONE);
        contenedorPesca.setVisibility(View.GONE);
        contenedorDibujo.setVisibility(View.GONE);
        txtTemporizador.setText("");
        imagen.setVisibility(View.INVISIBLE);
    }

    // =========================================================
    // SENSORES — Distribuye datos a cada minijuego
    // =========================================================

    private void registrarSensoresSiNecesario() {
        if (sensorManager == null) return;
        sensorManager.unregisterListener(this);
        if (estadoActual == Estado.JUEGO) {
            if (rotationSensor != null)
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            if (accelerometerSensor != null)
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (minijuegoActual == null || estadoActual != Estado.JUEGO) return;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            float pitch = (float) Math.toDegrees(orientationAngles[1]);
            float roll  = (float) Math.toDegrees(orientationAngles[2]);
            float yaw   = (float) Math.toDegrees(orientationAngles[0]);

            if (minijuegoActual instanceof DueloAMediodia)
                ((DueloAMediodia) minijuegoActual).onDatosGiro(pitch, roll);
            if (minijuegoActual instanceof MiniJuegoDibujoFiguras)
                ((MiniJuegoDibujoFiguras) minijuegoActual).onDatosGiro(pitch, yaw);
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
            gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
            gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];
            float lx = event.values[0] - gravity[0];
            float ly = event.values[1] - gravity[1];
            float lz = event.values[2] - gravity[2];
            float mag = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);

            if (minijuegoActual instanceof MiniJuegoMovimiento)
                ((MiniJuegoMovimiento) minijuegoActual).onDatosAcel(ly);
            if (minijuegoActual instanceof MiniJuegoPesca)
                ((MiniJuegoPesca) minijuegoActual).onDatosAcel(mag);
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
    //  MINIJUEGO 3 — MOVIMIENTO ADELANTE/ATRÁS
    //  4 cambios de dirección en 5 segundos.
    //
    // =========================================================
    private class MiniJuegoMovimiento implements Minijuego {

        private static final long  TIEMPO_LIMITE_MS   = 5000;
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
            this.callback  = cb;
            this.activo    = true;
            this.cambios   = 0;
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
            if ((ultimoEje > MOVE_THRESHOLD  && ejeY < -MOVE_THRESHOLD) ||
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

    // =========================================================
    //
    //  MINIJUEGO 4 — PESCA
    //
    //  FASE 1 · LANZAR (5s):
    //    Sacude el móvil para lanzar el cebo.
    //    Tiempo agotado sin sacudir → pierde.
    //
    //  FASE 2 · ESPERAR (15s):
    //    El pez aparece en un momento ALEATORIO dentro de esos 15s.
    //    Cualquier sacudida ANTES de que aparezca el pez → pierde.
    //    Tiempo agotado sin que el jugador haya pescado → pierde.
    //
    //  FASE 3 · ¡PEZ! (3s):
    //    Sacude para sacar el pez.
    //    Tiempo agotado → pierde.
    //
    // =========================================================

    private enum Fase { LANZAR, ESPERAR, PEZ_ACTIVO, INACTIVO }

    private class MiniJuegoPesca implements Minijuego {

        private final float SHAKE_THRESHOLD  = 35.0f;
        private final long  TIEMPO_LANZAR_MS = 5000;
        private final long  TIEMPO_ESPERA_MS = 15000;
        private final long  TIEMPO_SACAR_MS  = 3000;

        private ResultadoCallback callback;
        private Fase fase      = Fase.INACTIVO;
        private CountDownTimer timer;
        private boolean activo = false;

        @Override public String getTitulo() { return "🎣 ¡A pescar!"; }

        @Override
        public String getExplicacion() {
            return "① Sacude el móvil para LANZAR el cebo (5s).\n\n" +
                    "② Espera a que pique el pez. ¡No sacudas antes o lo asustarás!\n\n" +
                    "③ Cuando aparezca 🐟, ¡tienes 3 segundos para sacudir y sacar el pez!";
        }

        @Override
        public void iniciar(ResultadoCallback cb) {
            this.callback = cb;
            this.activo   = true;
            contenedorPesca.setVisibility(View.VISIBLE);
            iniciarFaseLanzar();
        }

        // ------ FASE 1: Lanzar ------
        private void iniciarFaseLanzar() {
            fase = Fase.LANZAR;
            txtEmojiPesca.setText("🎣");
            txtEstadoPesca.setText("¡Sacude para lanzar el cebo!");

            timer = new CountDownTimer(TIEMPO_LANZAR_MS, 100) {
                @Override public void onTick(long ms) {
                    txtTemporizador.setText("⏱ Lanzar: " + (ms / 1000 + 1) + "s");
                }
                @Override public void onFinish() {
                    if (activo) { activo = false; callback.onPerdio(); }
                }
            }.start();
        }

        // ------ FASE 2: Esperar al pez ------
        private void iniciarFaseEspera() {
            fase = Fase.ESPERAR;
            timer.cancel();
            txtEmojiPesca.setText("🌊");
            txtEstadoPesca.setText("El cebo está en el agua... ¡espera!");
            txtTemporizador.setText("");

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
            txtEmojiPesca.setText("🐟");
            txtEstadoPesca.setText("¡¡PICA!! ¡Sacude para sacarlo!");

            timer = new CountDownTimer(TIEMPO_SACAR_MS, 100) {
                @Override public void onTick(long ms) {
                    txtTemporizador.setText("⏱ " + (ms / 1000 + 1) + "s");
                }
                @Override public void onFinish() {
                    if (activo) { activo = false; callback.onPerdio(); }
                }
            }.start();
        }

        // Llamado desde onSensorChanged con la magnitud de aceleración lineal
        void onDatosAcel(float magnitud) {
            if (!activo) return;
            boolean sacudida = magnitud > SHAKE_THRESHOLD;

            switch (fase) {
                case LANZAR:
                    if (sacudida) {
                        txtEmojiPesca.setText("🎯");
                        txtEstadoPesca.setText("¡Lanzado! Ahora espera...");
                        iniciarFaseEspera();
                    }
                    break;

                case ESPERAR:
                    if (sacudida) {
                        // Sacudida prematura: asusta al pez
                        activo = false;
                        timer.cancel();
                        handler.removeCallbacksAndMessages(null);
                        txtEmojiPesca.setText("💦");
                        txtEstadoPesca.setText("¡Asustaste al pez!");
                        callback.onPerdio();
                    }
                    break;

                case PEZ_ACTIVO:
                    if (sacudida) {
                        activo = false;
                        timer.cancel();
                        txtEmojiPesca.setText("🏆");
                        txtEstadoPesca.setText("¡Pescado!");
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
            fase   = Fase.INACTIVO;
            if (timer != null) timer.cancel();
            handler.removeCallbacksAndMessages(null);
        }
    }

    // =========================================================
    //
    //  MINIJUEGO 5 — DIBUJAR FIGURAS EN EL AIRE
    //
    //  El programa elige 3 figuras al azar de entre:
    //  Círculo, Cuadrado, Triángulo, Estrella, Hexágono.
    //  El jugador tiene 5s para dibujar cada una moviendo
    //  el móvil como si fuera un pincel en el aire.
    //
    //  La trayectoria se obtiene integrando el giroscopio
    //  (yaw + pitch) y se analizan estas características:
    //    · Número de esquinas (cambios bruscos de dirección)
    //    · Si la trayectoria es cerrada (inicio ≈ fin)
    //    · Varianza angular (suavidad de la curva)
    //    · Ratio de aspecto (ancho/alto de la bounding box)
    //
    // =========================================================

    private enum Figura {
        CIRCULO   ("○ Círculo",   "Haz un movimiento circular continuo y ciérralo"),
        CUADRADO  ("□ Cuadrado",  "Dibuja 4 lados con esquinas de ~90°"),
        TRIANGULO ("△ Triángulo", "Dibuja 3 lados con esquinas bruscas"),
        ESTRELLA  ("★ Estrella",  "Dibuja picos arriba/abajo alternando (≥5)"),
        HEXAGONO  ("⬡ Hexágono",  "Dibuja 6 lados con esquinas de ~120°");

        final String nombre, pista;
        Figura(String n, String p) { nombre = n; pista = p; }
    }
    private class MiniJuegoDibujoFiguras implements Minijuego {

        private static final int   NUM_FIGURAS      = 3;
        private static final long  TIEMPO_FIGURA_MS = 5000;
        // Un cambio de dirección > este ángulo (grados) cuenta como esquina
        private static final float UMBRAL_ESQUINA   = 55f;

        private ResultadoCallback callback;
        private boolean activo      = false;
        private int figuraActualIdx = 0;
        private List<Figura> secuencia;

        // Trayectoria del gesto actual (yaw, pitch en grados)
        private final List<float[]> trayectoria = new ArrayList<>();
        private float lastYaw = Float.NaN, lastPitch = Float.NaN;

        private CountDownTimer timerFigura;

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

            contenedorDibujo.setVisibility(View.VISIBLE);
            iniciarFigura();
        }

        private void iniciarFigura() {
            Figura f = secuencia.get(figuraActualIdx);
            trayectoria.clear();
            lastYaw   = Float.NaN;
            lastPitch = Float.NaN;
            figuraView.limpiar();

            txtFiguraObjetivo.setText("Dibuja: " + f.nombre);
            txtEstadoDibujo.setText("Figura " + (figuraActualIdx + 1) + " de " + NUM_FIGURAS
                    + "\n" + f.pista);
            txtResultadoDibujo.setText("");

            if (timerFigura != null) timerFigura.cancel();
            timerFigura = new CountDownTimer(TIEMPO_FIGURA_MS, 50) {
                @Override public void onTick(long ms) {
                    txtTemporizador.setText("⏱ " + (ms / 1000 + 1) + "s");
                }
                @Override public void onFinish() {
                    if (activo) evaluarFigura(secuencia.get(figuraActualIdx));
                }
            }.start();
        }

        // Recibe pitch y yaw en grados desde onSensorChanged
        void onDatosGiro(float pitch, float yaw) {
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

            int     esquinas       = contarEsquinas();
            boolean cerrada        = esTrayectoriaCerrada();
            float   varianza       = calcularVarianzaAngular();
            float   ratio          = calcularRatioAspecto();
            boolean correcto       = false;

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
            txtResultadoDibujo.setText("✅ ¡Correcto!");
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
            txtResultadoDibujo.setText(msg);
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
    }

    // =========================================================
    //  FiguraView — Canvas personalizado que dibuja la
    //  trayectoria del gesto en tiempo real sobre la pantalla.
    // =========================================================
    private class FiguraView extends View {

        private final Paint paintLinea = new Paint();
        private final Paint paintDot   = new Paint();
        private final Path  path       = new Path();

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

            int   w      = getWidth();
            int   h      = getHeight();
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