package com.example.mandosapp_surdo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

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

        String[] colores = new String[] {"Rojo", "Amarillo", "Verde", "Azul"};

        final Button mando1 = findViewById(R.id.mando1);
        final Button mando2 = findViewById(R.id.mando2);
        final Button mando3 = findViewById(R.id.mando3);

        mando1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent myIntent = new Intent(MainActivity.this, Mando1Activity.class);
                startActivity(myIntent);
            }
        });

        mando2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String color = colores[new Random().nextInt(colores.length)];

                final Intent myIntent = new Intent(MainActivity.this, Mando2Activity.class);
                myIntent.putExtra("color", color);
                startActivity(myIntent);
            }
        });

        mando3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent myIntent = new Intent(MainActivity.this, Mando3Activity.class);
                startActivity(myIntent);
            }
        });

    }
}