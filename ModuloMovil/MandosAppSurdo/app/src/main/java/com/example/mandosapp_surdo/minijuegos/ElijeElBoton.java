package com.example.mandosapp_surdo.minijuegos;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import com.example.mandosapp_surdo.NsDondePonerte.Minijuego;
import com.example.mandosapp_surdo.NsDondePonerte.ResultadoCallback;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ElijeElBoton implements Minijuego {

    private LinearLayout botonera;
    private MaterialButton btn1, btn2, btn3;
    private final String[] NOMBRES = {"Rojo", "Amarillo", "Verde", "Azul"};
    private final int[]    COLORES = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE};
    private ResultadoCallback callback;
    private boolean activo = false;
    private final Random random  = new Random();

    public ElijeElBoton (LinearLayout botonera, MaterialButton btn1, MaterialButton btn2, MaterialButton btn3) {
        this.botonera = botonera;
        this.btn1 = btn1;
        this.btn2 = btn2;
        this.btn3 = btn3;
    }

    @Override public String getTitulo() {
        return "Elije el Boton";
    }

    @Override
    public String getExplicacion() {
        return "Tienes a elegir entre tres botones, pulsa el que te pidan." +
                "\n\nNo te preocupes, aqui no hay ninguna punchline :)";
    }

    @Override
    public void iniciar(ResultadoCallback callback) {

        this.callback = callback;
        this.activo = true;
        botonera.setVisibility(View.VISIBLE);
        int idxObj = random.nextInt(NOMBRES.length);
        int colorObj = COLORES[idxObj];
        int otro1, otro2;
        do { otro1 = random.nextInt(NOMBRES.length); } while (otro1 == idxObj);
        do { otro2 = random.nextInt(NOMBRES.length); } while (otro2 == idxObj || otro2 == otro1);

        List<Integer> indices = new ArrayList<>();
        indices.add(idxObj); indices.add(otro1); indices.add(otro2);
        Collections.shuffle(indices);

        int c1 = COLORES[indices.get(0)];
        int c2 = COLORES[indices.get(1)];
        int c3 = COLORES[indices.get(2)];

        btn1.setBackgroundTintList(ColorStateList.valueOf(c1));
        btn2.setBackgroundTintList(ColorStateList.valueOf(c2));
        btn3.setBackgroundTintList(ColorStateList.valueOf(c3));
        btn1.setText(""); btn3.setText("");
        btn2.setText(NOMBRES[idxObj]);
        btn2.setTextColor(colorObj);

        btn1.setOnClickListener(v -> { if (activo) evaluar(c1, colorObj); });
        btn2.setOnClickListener(v -> { if (activo) evaluar(c2, colorObj); });
        btn3.setOnClickListener(v -> { if (activo) evaluar(c3, colorObj); });
    }

    private void evaluar(int pulsado, int objetivo) {
        activo = false;

        if (pulsado == objetivo) {
            callback.onGano();
        } else {
            callback.onPerdio();
        }
    }

    @Override
    public void detener() {
        activo = false;
        btn1.setOnClickListener(null);
        btn2.setOnClickListener(null);
        btn3.setOnClickListener(null);
    }
}