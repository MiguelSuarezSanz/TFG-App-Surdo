package com.example.mandosapp_surdo.Conexión;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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

import com.example.mandosapp_surdo.interfaces.Minijuego;
import com.example.mandosapp_surdo.interfaces.ResultadoCallback;
import com.example.mandosapp_surdo.R;
import com.example.mandosapp_surdo.enume.Estado;
import com.example.mandosapp_surdo.minijuegos.CurtKobainSimulator;
import com.example.mandosapp_surdo.minijuegos.DiaPesca;
import com.example.mandosapp_surdo.minijuegos.DibujaLaFigura;
import com.example.mandosapp_surdo.minijuegos.DueloIrlandes;
import com.example.mandosapp_surdo.minijuegos.ElijeElBoton;
import com.example.mandosapp_surdo.minijuegos.SaludoCatalan;
import com.example.mandosapp_surdo.minijuegos.SimpleBoton;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VentanaMinijuegosActivity extends AppCompatActivity implements SensorEventListener {

    private Estado estadoActual = Estado.MENU;

    // =========================================================
    // VISTAS — Pantallas
    // =========================================================
    private View pantallaMenu;
    private View pantallaExplicacion;
    private View pantallaJuego;
    private View pantallaGameOver;

    // Menú
    private TextView txtEsperandoServidor;

    // Explicación — solo título y botón listo
    private TextView txtTituloMinijuego;

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
    private TextView txtFiguraObjetivo;
    private TextView txtEstadoDibujo;
    private TextView txtResultadoDibujo;

    // Minijuego: Duelo a la Irlandesa
    private LinearLayout contenedorDueloIrlandes;
    private View vistaLiquido;
    private TextView txtPorcentajeCerveza;
    private TextView txtEstadoCerveza;

    // Game Over
    private TextView txtPuntuacionFinal;
    private TextView txtComentarioDerrota;

    // =========================================================
    // SENSORES
    // =========================================================
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometerSensor;

    private final float[] rotationMatrix    = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] gravity           = new float[3];
    private static final float ALPHA        = 0.8f;

    // =========================================================
    // ESTADO DEL JUEGO
    // =========================================================
    private int puntuacion = 0;
    private static final String PREFS_NAME = "minijuegos_prefs";
    private static final String KEY_MAX = "max_puntuacion";
    private List<Minijuego> listaMinijuegos;
    private Minijuego minijuegoActual;
    private final Random  random  = new Random();
    private final Handler handler = new Handler();

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // =========================================================
    // INICIALIZACIÓN
    // =========================================================
    private void vincularVistas() {
        pantallaMenu  = findViewById(R.id.pantallaMenu);
        pantallaExplicacion = findViewById(R.id.pantallaExplicacion);
        pantallaJuego = findViewById(R.id.pantallaJuego);
        pantallaGameOver = findViewById(R.id.pantallaGameOver);

        txtEsperandoServidor = findViewById(R.id.txtEsperandoServidor);
        txtTituloMinijuego = findViewById(R.id.txtTituloMinijuego);
        txtTemporizador = findViewById(R.id.txtTemporizador);

        contenedorBotones = findViewById(R.id.contenedorBotones);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);

        contenedorBoton  = findViewById(R.id.contenedorSimpleBoton);
        btnSimple = findViewById(R.id.botonSimple);
        instruccionesBtnSimple = findViewById(R.id.txtInstruccionSimpleBoton);

        contenedorGiroscopio = findViewById(R.id.contenedorGiroscopio);
        imagen  = findViewById(R.id.imagen);
        txtEstadoGiro = findViewById(R.id.txtEstadoGiro);

        contenedorAcelerometro = findViewById(R.id.contenedorAcelerometro);
        txtEmojiAccel          = findViewById(R.id.txtEmojiAccel);
        txtEstadoAccel         = findViewById(R.id.txtEstadoAccel);

        contenedorPesca  = findViewById(R.id.contenedorPesca);
        txtEmojiPesca    = findViewById(R.id.txtEmojiPesca);
        txtEstadoPesca   = findViewById(R.id.txtEstadoPesca);

        contenedorDibujo = findViewById(R.id.contenedorDibujo);
        txtFiguraObjetivo = findViewById(R.id.txtFiguraObjetivo);
        txtEstadoDibujo = findViewById(R.id.txtEstadoDibujo);
        txtResultadoDibujo = findViewById(R.id.txtResultadoDibujo);

        contenedorDueloIrlandes = findViewById(R.id.contenedorDueloIrlandes);
        vistaLiquido = findViewById(R.id.vistaLiquido);
        txtPorcentajeCerveza = findViewById(R.id.txtPorcentajeCerveza);
        txtEstadoCerveza = findViewById(R.id.txtEstadoCerveza);

        txtPuntuacionFinal = findViewById(R.id.txtPuntuacionFinal);
        txtComentarioDerrota = findViewById(R.id.txtComentarioDerrota);
    }

    private void inicializarSensores() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor      = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    // =========================================================
    // LISTA DE MINIJUEGOS
    // =========================================================
    private void crearListaMinijuegos() {
        listaMinijuegos = new ArrayList<>();
        listaMinijuegos.add(new ElijeElBoton(contenedorBotones, btn1, btn2, btn3));
        listaMinijuegos.add(new CurtKobainSimulator(contenedorGiroscopio, imagen, txtEstadoGiro, txtTemporizador));
        listaMinijuegos.add(new SimpleBoton(contenedorBoton, btnSimple, instruccionesBtnSimple, txtTemporizador));
        listaMinijuegos.add(new SaludoCatalan(contenedorAcelerometro, txtEmojiAccel, txtEstadoAccel, txtTemporizador));
        listaMinijuegos.add(new DiaPesca(contenedorPesca, txtEmojiPesca, txtEstadoPesca, txtTemporizador, handler));
        listaMinijuegos.add(new DueloIrlandes(contenedorDueloIrlandes, vistaLiquido, txtPorcentajeCerveza, txtEstadoCerveza, txtTemporizador));
        // listaMinijuegos.add(new DibujaLaFigura(contenedorDibujo, txtFiguraObjetivo, txtEstadoDibujo, txtResultadoDibujo, txtTemporizador, handler));
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

        mostrarPantalla(Estado.GAME_OVER);
        txtPuntuacionFinal.setText("Puntuación: " + puntuacion);
        txtComentarioDerrota.setText("");

        // Guardar récord por si se necesita en el futuro
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int maxPuntuacion = prefs.getInt(KEY_MAX, 0);
        if (puntuacion > maxPuntuacion)
            prefs.edit().putInt(KEY_MAX, puntuacion).apply();

        handler.postDelayed(() -> mostrarPantalla(Estado.MENU), 4000);
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
        contenedorPesca.setVisibility(View.GONE);
        contenedorDibujo.setVisibility(View.GONE);
        contenedorBoton.setVisibility(View.GONE);
        contenedorDueloIrlandes.setVisibility(View.GONE);
        txtTemporizador.setText("");
        imagen.setVisibility(View.INVISIBLE);
    }

    // =========================================================
    // SENSORES
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
            float roll = (float) Math.toDegrees(orientationAngles[2]);
            float yaw = (float) Math.toDegrees(orientationAngles[0]);

            if (minijuegoActual instanceof CurtKobainSimulator)
                ((CurtKobainSimulator) minijuegoActual).onDatosGiro(pitch, roll);
            if (minijuegoActual instanceof DibujaLaFigura)
                ((DibujaLaFigura) minijuegoActual).onDatosGiro(pitch, yaw);
            if (minijuegoActual instanceof DueloIrlandes)
                ((DueloIrlandes) minijuegoActual).onDatosGiro(pitch);
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
            gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
            gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];
            float lx = event.values[0] - gravity[0];
            float ly = event.values[1] - gravity[1];
            float lz = event.values[2] - gravity[2];
            float mag = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);

            if (minijuegoActual instanceof SaludoCatalan)
                ((SaludoCatalan) minijuegoActual).onDatosAcel(ly);
            if (minijuegoActual instanceof DiaPesca)
                ((DiaPesca) minijuegoActual).onDatosAcel(mag);
            if (minijuegoActual instanceof CurtKobainSimulator)
                ((CurtKobainSimulator) minijuegoActual).onDatosAcel(mag);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}