package com.quictunnel.client;

import com.quictunnel.client.jni.QuicheWrapper;

/**
 * Implementación falsa de QuicheWrapper para pruebas.
 * Simula el comportamiento de quiche sin necesitar binarios nativos.
 *
 * Permite probar la lógica del cliente (reconexión, callbacks,
 * estados...) en cualquier plataforma sin Android ni .so.
 */
public class MockQuicheWrapper extends QuicheWrapper {

    // Sobrescribimos el bloque static para que NO cargue libquiche.so
    static {}

    /**
     * Controla si la conexión debe fallar o no.
     * Los tests pueden cambiarlo para simular fallos.
     */
    public boolean shouldConnectFail = false;

    /**
     * Controla si la conexión está activa.
     */
    public boolean connected = false;

    /**
     * Datos que el mock devolverá en el próximo receiveDatagram.
     * null significa timeout (no hay datos).
     */
    public byte[] nextReceiveData = null;

    /**
     * Contador de llamadas a sendDatagram.
     * Útil para verificar que send() se llama el número correcto de veces.
     */
    public int sendCount = 0;

    /**
     * Último payload enviado via sendDatagram.
     */
    public byte[] lastSentData = null;

    @Override
    public long connect(String host, int port,
                        String caCertPath,
                        String clientCertPath,
                        String clientKeyPath) {
        if (shouldConnectFail) {
            return -1;
        }
        connected = true;
        return 1L; // handle ficticio
    }

    @Override
    public void close(long connHandle) {
        connected = false;
    }

    @Override
    public boolean isConnected(long connHandle) {
        return connected;
    }

    @Override
    public int sendDatagram(long connHandle, byte[] data) {
        sendCount++;
        lastSentData = data;
        return data.length;
    }

    @Override
    public int receiveDatagram(long connHandle, byte[] buffer, int timeoutMs) {
        if (nextReceiveData == null) {
            return 0; // timeout
        }
        System.arraycopy(nextReceiveData, 0, buffer, 0, nextReceiveData.length);
        int length = nextReceiveData.length;
        nextReceiveData = null; // consumimos el dato
        return length;
    }

    @Override
    public String getLastError(long connHandle) {
        return shouldConnectFail ? "Mock: conexión fallida" : null;
    }
}