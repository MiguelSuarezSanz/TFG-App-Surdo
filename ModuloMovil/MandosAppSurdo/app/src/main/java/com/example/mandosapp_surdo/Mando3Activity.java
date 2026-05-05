package com.example.mandosapp_surdo;

import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.ImageView;

public class Mando3Activity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private ImageView imagen;
    private MediaPlayer cancion1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mando3);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /* Al iniciar la actividad, cargo los modulos de musica y de sensores, y la imagen con, la
        cancion correspondiente, los sensores del telefono y la imagen respectivamente */
        cancion = MediaPlayer.create(this, R.raw.cancion);
        imagen = findViewById(R.id.imagen);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        /* Una vez cargados, si el modulo de sensores tiene guardados los sensores del telefono,
        carga el modulo de rotacion con la informacion del giroscopio */
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Convertir a matriz de rotación
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

        // Obtener ángulos (rad)
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        // Convertir a grados
        float pitch = (float) Math.toDegrees(orientationAngles[1]);

        /*
        Una vez obtenido la informacion de la inclinacion del telefono, compruebo si dicha
        inclinacion es mayor y menor a 10 grados, tanto positivos como negativos, dependiendo de si
        esta condicion se cumple o no sucederan lo siguientes casos:
            - Si esta dentro del rango especificado, hara aparecer la imagen en la pantalla y
            comenzara a sonar la cancion y la pondra en bucle
            - Si no esta dentro del rango definido, ocultara la imagen y pausara la cancion
        */
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}