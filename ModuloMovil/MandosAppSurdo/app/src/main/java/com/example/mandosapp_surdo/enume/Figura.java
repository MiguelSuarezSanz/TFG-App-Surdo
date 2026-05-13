package com.example.mandosapp_surdo.enume;

public enum Figura {
    CIRCULO   ("○ Círculo",   "Haz un movimiento circular continuo y ciérralo"),
    CUADRADO  ("□ Cuadrado",  "Dibuja 4 lados con esquinas de ~90°"),
    TRIANGULO ("△ Triángulo", "Dibuja 3 lados con esquinas bruscas"),
    ESTRELLA  ("★ Estrella",  "Dibuja picos arriba/abajo alternando (≥5)"),
    HEXAGONO  ("⬡ Hexágono",  "Dibuja 6 lados con esquinas de ~120°");

    public final String nombre;
    public final String pista;
    Figura(String n, String p) {
        nombre = n; pista = p;
    }
}
