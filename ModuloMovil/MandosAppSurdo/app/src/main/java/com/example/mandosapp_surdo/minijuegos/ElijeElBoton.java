package com.example.mandosapp_surdo.minijuegos;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import com.example.mandosapp_surdo.interfaces.Minijuego;
import com.example.mandosapp_surdo.interfaces.ResultadoCallback;
import com.google.android.material.button.MaterialButton;

import java.util.Random;

public class ElijeElBoton implements Minijuego {

    private LinearLayout botonera;
    private MaterialButton btn1, btn2, btn3;
    private final String[] NOMBRES = {"Rojo", "Amarillo", "Verde", "Azul"};
    private final int[] COLORES = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE};
    private ResultadoCallback callback;
    private boolean activo = false;
    private final Random random = new Random();

    public ElijeElBoton (LinearLayout botonera, MaterialButton btn1, MaterialButton btn2, MaterialButton btn3) {
        this.botonera = botonera;
        this.btn1 = btn1;
        this.btn2 = btn2;
        this.btn3 = btn3;
    }

    @Override
    public String getTitulo() {
        return "Elije el Boton";
    }

    @Override
    public String getIntroduccion() {
        return "Tienes a elegir entre tres botones, pulsa el que quieras, tan sencillo como eso." +
                "¿Por que me miras asi, que no hay nada raro con estos tres botones?" +
                "¿¡Quieres jugar pedazo cenutrio!?";
    }

    @Override
    public String getExplicacion() {
        return "Elige entre tres botones, si pulsas el correcto, ganas, si no, pierdes";
    }

    @Override
    public void iniciar(ResultadoCallback cb) {
        this.callback = cb;
        this.activo   = true;
        botonera.setVisibility(View.VISIBLE);

        // --- Color elegido (el correcto, siempre en btn1) ---
        int idxElegido    = random.nextInt(NOMBRES.length);
        int colorElegido  = COLORES[idxElegido];
        String nombreElegido = NOMBRES[idxElegido];

        // --- Color del fondo de btn2 (distinto al elegido) ---
        int idxFondoBtn2;
        do { idxFondoBtn2 = random.nextInt(NOMBRES.length); }
        while (idxFondoBtn2 == idxElegido);
        int colorFondoBtn2 = COLORES[idxFondoBtn2];

        // --- Color del texto de btn3 (distinto al elegido, puede coincidir con fondoBtn2) ---
        int idxTextoBtn3;
        do { idxTextoBtn3 = random.nextInt(NOMBRES.length); }
        while (idxTextoBtn3 == idxElegido);
        String nombreTextoBtn3 = NOMBRES[idxTextoBtn3];

        // ---- BTN 1: fondo = color elegido, sin texto ----
        btn1.setBackgroundTintList(ColorStateList.valueOf(colorElegido));
        btn1.setText("");

        // ---- BTN 2: fondo distinto al elegido, texto = nombre elegido en NEGRO ----
        btn2.setBackgroundTintList(ColorStateList.valueOf(colorFondoBtn2));
        btn2.setText(nombreElegido);
        btn2.setTextColor(Color.BLACK);

        // ---- BTN 3: fondo gris, texto = otro color, letras en color elegido ----
        btn3.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        btn3.setText(nombreTextoBtn3);
        btn3.setTextColor(colorElegido);

        int ganador = (int) (Math.random() * 3) + 1;

        btn1.setOnClickListener(v -> { if (activo) { evaluar(1, ganador); } });
        btn2.setOnClickListener(v -> { if (activo) { evaluar(2, ganador); } });
        btn3.setOnClickListener(v -> { if (activo) { evaluar(3, ganador); } });
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