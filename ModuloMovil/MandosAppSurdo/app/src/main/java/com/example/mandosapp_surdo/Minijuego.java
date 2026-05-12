package com.example.mandosapp_surdo;

public interface Minijuego {
    String getTitulo();
    String getExplicacion();
    void iniciar(ResultadoCallback callback);
    void detener();
}
