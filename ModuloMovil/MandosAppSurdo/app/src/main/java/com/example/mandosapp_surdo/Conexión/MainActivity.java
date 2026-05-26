package com.example.mandosapp_surdo.Conexión;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mandosapp_surdo.R;

public class MainActivity extends AppCompatActivity {

    boolean debug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        final Button mando1 = findViewById(R.id.mando1);
        final Button mando2 = findViewById(R.id.mando2);

        /* Si esta activo el debug, mostrara el boton que lleva directamente a la pantalla de
        minijuegos, y si es false, hace que el boton Jugar ocupe el layout completo y que su texto
        sea mas grande */
        if (debug) {
            mando2.setVisibility(View.VISIBLE);
        } else {
            ViewGroup.LayoutParams params = mando1.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mando1.setLayoutParams(params);
            mando1.setTextSize(30);
        }

        mando1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent myIntent = new Intent(MainActivity.this, Conexion.class);
                startActivity(myIntent);
            }
        });
        mando2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent myIntent = new Intent(MainActivity.this, VentanaMinijuegosActivity.class);
                myIntent.putExtra("debug", debug);
                startActivity(myIntent);
            }
        });
    }
}