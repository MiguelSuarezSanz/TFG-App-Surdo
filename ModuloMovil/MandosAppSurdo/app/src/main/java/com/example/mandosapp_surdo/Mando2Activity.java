package com.example.mandosapp_surdo;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Random;

public class Mando2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mando2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /* Cargo todos los valores de los botones que se modificaran cuando entre el color enviado
        por la actividad principal */
        String[] colores;
        Bundle extras = getIntent().getExtras();
        String color = extras.getString("color");
        MaterialButton btn1 = findViewById(R.id.btn1);
        MaterialButton btn2 = findViewById(R.id.btn2);
        MaterialButton btn3 = findViewById(R.id.btn3);
        String textobtn2 = "";

        /*
            El swicth funciona de esta manera:
                - Dependiendo del color recibido, se cargara primero un array con la lista de
                colores de la actividad principal, salvo el color pasado por ella misma
                - Despues, el color de fondo del primer boton se cambiara al color recibido
                - Luego, se modificara el segundo boton para que muestre como texto, uno de los
                colores del array, con las letras del color recibido
                - Por ultimo, se le agregara al tercer boton el color recibido como texto
        */
        switch (color) {
            case "Rojo":
                colores = new String[] {"Amarillo", "Verde", "Azul"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                btn2.setTextColor(Color.RED);
                btn2.setText(textobtn2);
                btn3.setText("Rojo");
                break;
            case "Amarillo":
                colores = new String[] {"Rojo", "Verde", "Azul"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
                btn2.setTextColor(Color.YELLOW);
                btn2.setText(textobtn2);
                btn3.setText("Amarillo");
                break;
            case "Verde":
                colores = new String[] {"Rojo", "Amarillo", "Azul"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                btn2.setTextColor(Color.GREEN);
                btn2.setText(textobtn2);
                btn3.setText("Verde");
                break;
            case "Azul":
                colores = new String[] {"Rojo", "Amarillo", "Verde"};
                textobtn2 = colores[new Random().nextInt(colores.length)];
                btn1.setBackgroundTintList(ColorStateList.valueOf(Color.BLUE));
                btn2.setTextColor(Color.BLUE);
                btn2.setText(textobtn2);
                btn3.setText("Azul");
                break;
        }

        /* Una vez la personalizacion ha concluido, se elegira un numero al azar entre el 1 y el 3,
        los botones tienen preasignados un numero de ese rango, al pulsar cualquiera de los tres
        botones, se comprobara el numero que tiene asignado dicho boton con el elegido, si coinciden
        ambos numeros, ese boton es el correcto */
        int numRandom = (int) (Math.random() * 3) + 1;

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = 1;

                if (num == numRandom) {
                    Toast.makeText(Mando2Activity.this, "Boton Correcto", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Mando2Activity.this, "Boton Incorrecto", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = 2;

                if (num == numRandom) {
                    Toast.makeText(Mando2Activity.this, "Boton Correcto", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Mando2Activity.this, "Boton Incorrecto", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = 3;

                if (num == numRandom) {
                    Toast.makeText(Mando2Activity.this, "Boton Correcto", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Mando2Activity.this, "Boton Incorrecto", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}