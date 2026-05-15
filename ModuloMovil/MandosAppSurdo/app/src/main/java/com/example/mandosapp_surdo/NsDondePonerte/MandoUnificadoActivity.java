package com.example.mandosapp_surdo.NsDondePonerte;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mandosapp_surdo.R;
import com.google.android.material.button.MaterialButton;
import java.util.Random;

public class MandoUnificadoActivity extends AppCompatActivity implements SensorEventListener {

    // =========================================================
    // ATRIBUTOS COMPARTIDOS
    // =========================================================

    // Indica qué mando está activo ahora mismo (empieza en el 2)
    private int mandoActivo = 2;

    // Contenedores de cada mando en el layout
    private LinearLayout contenedorMando2;
    private ConstraintLayout contenedorMando3;

    // =========================================================
    // ATRIBUTOS DEL MANDO 3 (deben ser de instancia porque los
    // usa onSensorChanged, que se llama fuera de onCreate)
    // =========================================================
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private ImageView imagen;
    private MediaPlayer cancion;

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mando_unificado);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Referencio los contenedores
        contenedorMando2 = findViewById(R.id.contenedorMando2);
        contenedorMando3 = findViewById(R.id.contenedorMando3);

        // Inicializo cada mando de forma independiente
        inicializarMando2();
        inicializarMando3();

        // Botón para alternar entre mandos
        Button btnCambiar = findViewById(R.id.btnCambiarMando);
        btnCambiar.setOnClickListener(v -> cambiarMando());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Solo registro el sensor si el mando 3 está activo
        if (mandoActivo == 3 && rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Siempre desregistro el sensor y pauso la canción al salir
        sensorManager.unregisterListener(this);
        if (cancion != null && cancion.isPlaying()) {
            cancion.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libero el MediaPlayer al cerrar la actividad
        if (cancion != null) {
            cancion.release();
            cancion = null;
        }
    }

    // =========================================================
    // LÓGICA DE CAMBIO DE MANDO
    // =========================================================

    private void cambiarMando() {
        if (mandoActivo == 2) {
            // Oculto el mando 2 y muestro el 3
            contenedorMando2.setVisibility(View.GONE);
            contenedorMando3.setVisibility(View.VISIBLE);
            mandoActivo = 3;

            // Registro el sensor al activar el mando 3
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            }

        } else {
            // Oculto el mando 3 y muestro el 2
            contenedorMando3.setVisibility(View.GONE);
            contenedorMando2.setVisibility(View.VISIBLE);
            mandoActivo = 2;

            // Desregistro el sensor y paro la canción al salir del mando 3
            sensorManager.unregisterListener(this);
            if (cancion != null && cancion.isPlaying()) {
                cancion.pause();
            }
            imagen.setVisibility(View.GONE);
        }
    }

    // =========================================================
    // MANDO 2 — Botones de colores
    // Código extraído de Mando2Activity sin apenas cambios
    // =========================================================

    private void inicializarMando2() {

        // Recojo el color enviado por la actividad anterior igual que antes
        String[] colores;
        Bundle extras = getIntent().getExtras();
        String color = (extras != null) ? extras.getString("color", "Rojo") : "Rojo";

        MaterialButton btn1 = findViewById(R.id.btn1);
        MaterialButton btn2 = findViewById(R.id.btn2);
        MaterialButton btn3 = findViewById(R.id.btn3);
        String textobtn2;

        // Switch idéntico al original
        switch (color) {
            case "Rojo":
                colores = new String[]{"Amarillo", "Verde", "Azul"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                btn2.setTextColor(Color.RED);
                btn2.setText(textobtn2);
                btn3.setText("Rojo");
                break;
            case "Amarillo":
                colores = new String[]{"Rojo", "Verde", "Azul"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
                btn2.setTextColor(Color.YELLOW);
                btn2.setText(textobtn2);
                btn3.setText("Amarillo");
                break;
            case "Verde":
                colores = new String[]{"Rojo", "Amarillo", "Azul"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                btn2.setTextColor(Color.GREEN);
                btn2.setText(textobtn2);
                btn3.setText("Verde");
                break;
            case "Azul":
            default:
                colores = new String[]{"Rojo", "Amarillo", "Verde"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.BLUE));
                btn2.setTextColor(Color.BLUE);
                btn2.setText(textobtn2);
                btn3.setText("Azul");
                break;
        }

        // Número aleatorio para determinar el botón correcto, igual que antes
        int numRandom = (int) (Math.random() * 3) + 1;

        btn1.setOnClickListener(v -> comprobarBoton(1, numRandom));
        btn2.setOnClickListener(v -> comprobarBoton(2, numRandom));
        btn3.setOnClickListener(v -> comprobarBoton(3, numRandom));
    }

    // Extraigo la comprobación a un método para no repetir código en los tres listeners
    private void comprobarBoton(int numBoton, int numRandom) {
        if (numBoton == numRandom) {
            Toast.makeText(this, "Boton Correcto", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Boton Incorrecto", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================
    // MANDO 3 — Giroscopio + imagen + canción
    // Código extraído de Mando3Activity sin apenas cambios
    // =========================================================

    private void inicializarMando3() {
        cancion = MediaPlayer.create(this, R.raw.cancion);
        imagen = findViewById(R.id.imagen);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        // El sensor NO se registra aquí: el mando 3 empieza oculto,
        // se activará cuando el usuario pulse "Cambiar Mando"
    }

    // Callback del sensor — idéntico al original de Mando3Activity
    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        float pitch = (float) Math.toDegrees(orientationAngles[1]);

        if (pitch < 10.0 && pitch > -10.0) {
            imagen.setVisibility(ImageView.VISIBLE);
            cancion.start();
            cancion.setLooping(true);
        } else {
            imagen.setVisibility(ImageView.GONE);
            cancion.pause();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}