package com.quictunnel.client.jni;

/**
 * Puente entre el código Java del túnel y la librería nativa quiche.
 *
 * quiche está escrito en Rust y compilado como código nativo (.so en Android).
 * Java no puede llamarlo directamente, necesita JNI (Java Native Interface)
 * como intermediario.
 *
 * Esta clase aísla todo el código JNI en un único lugar. El resto del
 * módulo (QuicClientConnection, QuicTunnelClient...) nunca toca JNI
 * directamente, solo usa esta clase.
 *
 * Los métodos marcados como 'native' no tienen implementación en Java.
 * Su implementación real está en el código Rust de quiche, compilado
 * en los ficheros .so de src/main/jniLibs/:
 *   arm64-v8a/libquiche.so    → móviles modernos
 *   armeabi-v7a/libquiche.so  → móviles antiguos
 *   x86_64/libquiche.so       → emulador
 */
public class QuicheWrapper {

    /**
     * Carga el binario nativo de quiche.
     * Se llama explícitamente desde QuicTunnelClient al arrancar,
     * no automáticamente al cargar la clase.
     * Así los tests pueden instanciar QuicheWrapper sin necesitar el .so.
     */
    public static void loadNativeLibrary() {
        System.loadLibrary("quiche_jni");
    }

    // ─────────────────────────────────────────────
    // Gestión de la conexión
    // ─────────────────────────────────────────────

    /**
     * Crea una nueva conexión QUIC con el servidor.
     *
     * @param host          IP o hostname del servidor.
     * @param port          Puerto del servidor.
     * @param caCertPath    Ruta al certificado de la CA para verificar al servidor.
     * @param clientCertPath Ruta al certificado del cliente (mTLS).
     * @param clientKeyPath  Ruta a la clave privada del cliente (mTLS).
     * @return Handle nativo de la conexión. Se usa en el resto de llamadas.
     *         Devuelve -1 si falla.
     */
    public native long connect(
            String host,
            int port,
            String caCertPath,
            String clientCertPath,
            String clientKeyPath
    );

    /**
     * Cierra la conexión QUIC identificada por el handle.
     *
     * @param connHandle Handle de la conexión devuelto por connect().
     */
    public native void close(long connHandle);

    /**
     * Comprueba si la conexión está activa.
     *
     * @param connHandle Handle de la conexión.
     * @return true si la conexión está establecida y puede enviar/recibir.
     */
    public native boolean isConnected(long connHandle);

    // ─────────────────────────────────────────────
    // Envío y recepción de datagramas
    // ─────────────────────────────────────────────

    /**
     * Envía un datagrama QUIC al servidor.
     * El datagrama ya lleva la cabecera [version|length|payload]
     * construida por QuicheTunnelConnection antes de llegar aquí.
     *
     * @param connHandle Handle de la conexión.
     * @param data       Bytes a enviar (cabecera + payload).
     * @return Número de bytes enviados, o -1 si falla.
     */
    public native int sendDatagram(long connHandle, byte[] data);

    /**
     * Recibe un datagrama QUIC del servidor.
     * Bloqueante: espera hasta que llega un datagrama o se agota el timeout.
     *
     * @param connHandle Handle de la conexión.
     * @param buffer     Buffer donde se escriben los bytes recibidos.
     * @param timeoutMs  Tiempo máximo de espera en milisegundos.
     * @return Número de bytes recibidos, 0 si timeout, -1 si error.
     */
    public native int receiveDatagram(long connHandle, byte[] buffer, int timeoutMs);

    // ─────────────────────────────────────────────
    // Estado y diagnóstico
    // ─────────────────────────────────────────────

    /**
     * Devuelve el último error de quiche como String.
     * Útil para construir mensajes de error en TunnelError.
     *
     * @param connHandle Handle de la conexión, o -1 para errores globales.
     * @return Descripción del último error, o null si no hay error.
     */
    public native String getLastError(long connHandle);
}