package com.example.mandosapp_surdo.interfaces;

public interface Minijuego {
    String getTitulo();
    String getExplicacion();
    void iniciar(ResultadoCallback callback);
    void detener();
}
