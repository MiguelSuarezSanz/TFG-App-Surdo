package com.example.mandosapp_surdo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Mando1Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mando1);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        final Button btnArriba = findViewById(R.id.btnArriba);
        final Button btnAbajo = findViewById(R.id.btnAbajo);
        final Button btnIzq = findViewById(R.id.btnIzq);
        final Button btnDcha = findViewById(R.id.btnDcha);
        final Button btnA = findViewById(R.id.btnA);
        final Button btnB = findViewById(R.id.btnB);


        btnArriba.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Mando1Activity.this, "Boton presionado = Arriba", Toast.LENGTH_SHORT).show();
            }
        });

        btnAbajo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Mando1Activity.this, "Boton presionado = Abajo", Toast.LENGTH_SHORT).show();
            }
        });

        btnIzq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Mando1Activity.this, "Boton presionado = Izquierda", Toast.LENGTH_SHORT).show();
            }
        });

        btnDcha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Mando1Activity.this, "Boton presionado = Derecha", Toast.LENGTH_SHORT).show();
            }
        });

        btnA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Mando1Activity.this, "Boton presionado = A", Toast.LENGTH_SHORT).show();
            }
        });

        btnB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Mando1Activity.this, "Boton presionado = B", Toast.LENGTH_SHORT).show();
            }
        });
    }
}