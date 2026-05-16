package com.example.mandosapp_surdo.NsDondePonerte;

public interface Minijuego {
    String getTitulo();
    String getExplicacion();
    void iniciar(ResultadoCallback callback);
    void detener();
}
