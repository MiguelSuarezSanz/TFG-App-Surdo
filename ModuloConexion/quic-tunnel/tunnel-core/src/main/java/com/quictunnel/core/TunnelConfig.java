package com.quictunnel.core;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuración del túnel.
 * Se construye con el patrón Builder para que sea legible
 * y no haya confusión con el orden de los parámetros.
 *
 * Ejemplo de uso:
 *   TunnelConfig config = TunnelConfig.builder()
 *       .host("192.168.1.10")
 *       .port(4242)
 *       .caCert("ca.crt")
 *       .serverCert("server.crt")
 *       .serverKey("server.key")
 *       .build();
 */
public class TunnelConfig {

    // Conexión
    private final String host;
    private final int port;

    // Certificados
    private final String caCertPath;
    private final String certPath;
    private final String keyPath;

    // Timeouts y reconexión
    private final int connectionTimeoutMs;
    private final int keepaliveIntervalMs;
    private final int reconnectMaxAttempts;
    private final int reconnectMinDelayMs;
    private final int reconnectMaxDelayMs;

    // Modelo de hilos
    private final Executor callbackExecutor;

    private TunnelConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.caCertPath = builder.caCertPath;
        this.certPath = builder.certPath;
        this.keyPath = builder.keyPath;
        this.connectionTimeoutMs = builder.connectionTimeoutMs;
        this.keepaliveIntervalMs = builder.keepaliveIntervalMs;
        this.reconnectMaxAttempts = builder.reconnectMaxAttempts;
        this.reconnectMinDelayMs = builder.reconnectMinDelayMs;
        this.reconnectMaxDelayMs = builder.reconnectMaxDelayMs;
        this.callbackExecutor = builder.callbackExecutor;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getCaCertPath() { return caCertPath; }
    public String getCertPath() { return certPath; }
    public String getKeyPath() { return keyPath; }
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public int getKeepaliveIntervalMs() { return keepaliveIntervalMs; }
    public int getReconnectMaxAttempts() { return reconnectMaxAttempts; }
    public int getReconnectMinDelayMs() { return reconnectMinDelayMs; }
    public int getReconnectMaxDelayMs() { return reconnectMaxDelayMs; }
    public Executor getCallbackExecutor() { return callbackExecutor; }

    public static class Builder {

        // Conexión
        private String host;
        private int port;

        // Certificados
        private String caCertPath;
        private String certPath;
        private String keyPath;

        // Valores por defecto razonables
        private int connectionTimeoutMs = 3000;  // 3 segundos para el handshake
        private int keepaliveIntervalMs = 5000;  // ping cada 5 segundos
        private int reconnectMaxAttempts = 5;    // 5 intentos antes de ERROR
        private int reconnectMinDelayMs = 1000;  // espera mínima 1 segundo
        private int reconnectMaxDelayMs = 30000; // espera máxima 30 segundos

        // Por defecto un hilo dedicado para los callbacks
        // así no se bloquea el hilo de red de Netty/quiche
        private Executor callbackExecutor = Executors.newSingleThreadExecutor();

        /**
         * IP o hostname del servidor.
         * Solo necesario en el cliente, el servidor no lo usa.
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Puerto donde escucha el servidor.
         * Tanto el servidor como el cliente deben usar el mismo.
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Ruta al certificado de la CA.
         * Tanto servidor como cliente lo necesitan para verificar al otro extremo.
         */
        public Builder caCert(String caCertPath) {
            this.caCertPath = caCertPath;
            return this;
        }

        /**
         * Ruta al certificado propio (server.crt o client.crt).
         */
        public Builder cert(String certPath) {
            this.certPath = certPath;
            return this;
        }

        /**
         * Ruta a la clave privada propia (server.key o client.key).
         */
        public Builder key(String keyPath) {
            this.keyPath = keyPath;
            return this;
        }

        /**
         * Tiempo máximo en ms para completar el handshake mTLS.
         * Si se supera se produce un error CONNECTION_FAILED.
         */
        public Builder connectionTimeout(int ms) {
            this.connectionTimeoutMs = ms;
            return this;
        }

        /**
         * Intervalo en ms entre pings QUIC nativos para detectar
         * si la conexión sigue viva.
         */
        public Builder keepaliveInterval(int ms) {
            this.keepaliveIntervalMs = ms;
            return this;
        }

        /**
         * Número máximo de intentos de reconexión antes de pasar a ERROR.
         * -1 para reintentar indefinidamente.
         */
        public Builder reconnectMaxAttempts(int attempts) {
            this.reconnectMaxAttempts = attempts;
            return this;
        }

        /**
         * Backoff exponencial para la reconexión.
         * minMs: tiempo mínimo entre intentos.
         * maxMs: tiempo máximo entre intentos.
         * El tiempo se dobla en cada intento hasta llegar al máximo.
         */
        public Builder reconnectBackoff(int minMs, int maxMs) {
            this.reconnectMinDelayMs = minMs;
            this.reconnectMaxDelayMs = maxMs;
            return this;
        }

        /**
         * Executor en el que se ejecutarán los callbacks de TunnelListener.
         * El juego elige en qué hilo quiere recibir los eventos.
         * En Android puede ser el hilo principal si se necesita tocar la UI.
         */
        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = executor;
            return this;
        }

        /**
         * Construye la configuración validando que los campos obligatorios
         * estén presentes.
         *
         * @throws TunnelException si falta algún campo obligatorio.
         */
        public TunnelConfig build() throws TunnelException {
            if (port <= 0 || port > 65535) {
                throw new TunnelException(
                        TunnelError.Type.CONFIGURATION_ERROR,
                        "Puerto inválido: " + port
                );
            }
            if (caCertPath == null) {
                throw new TunnelException(
                        TunnelError.Type.CONFIGURATION_ERROR,
                        "Falta el certificado de la CA (caCert)"
                );
            }
            if (certPath == null) {
                throw new TunnelException(
                        TunnelError.Type.CONFIGURATION_ERROR,
                        "Falta el certificado propio (cert)"
                );
            }
            if (keyPath == null) {
                throw new TunnelException(
                        TunnelError.Type.CONFIGURATION_ERROR,
                        "Falta la clave privada (key)"
                );
            }
            return new TunnelConfig(this);
        }
    }
}