package com.quictunnel.client;

import com.quictunnel.core.TunnelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestiona la política de reconexión automática del cliente.
 *
 * Cuando la conexión se cae, el cliente no intenta reconectar
 * inmediatamente sino que espera un tiempo que va aumentando
 * exponencialmente con cada intento fallido.
 *
 * Esto evita bombardear al servidor con reconexiones si está caído,
 * y da tiempo a que la red se recupere.
 *
 * Ejemplo con configuración por defecto (min=1s, max=30s):
 *   Intento 1 → espera 1s
 *   Intento 2 → espera 2s
 *   Intento 3 → espera 4s
 *   Intento 4 → espera 8s
 *   Intento 5 → espera 16s
 *   Intento 6 → espera 30s  ← toca el máximo, no sube más
 */
public class ClientReconnectPolicy {

    private static final Logger log = LoggerFactory.getLogger(ClientReconnectPolicy.class);

    private final int maxAttempts;
    private final long minDelayMs;
    private final long maxDelayMs;

    /**
     * Número de intentos realizados hasta ahora.
     * Se resetea a 0 cuando la conexión se establece con éxito.
     */
    private int attempts;

    /**
     * Indica si la reconexión automática está habilitada.
     * Se deshabilita cuando el usuario llama a stop() explícitamente,
     * para que el túnel no intente reconectar tras un cierre voluntario.
     */
    private boolean enabled;

    /**
     * @param config La configuración del túnel con los parámetros de reconexión.
     */
    public ClientReconnectPolicy(TunnelConfig config) {
        this.maxAttempts = config.getReconnectMaxAttempts();
        this.minDelayMs = config.getReconnectMinDelayMs();
        this.maxDelayMs = config.getReconnectMaxDelayMs();
        this.attempts = 0;
        this.enabled = true;
    }

    /**
     * Indica si se debe intentar reconectar.
     *
     * Devuelve false si:
     * - La reconexión está deshabilitada (stop() fue llamado)
     * - Se agotaron los intentos máximos (a menos que sea -1 = infinito)
     *
     * @return true si se debe intentar reconectar.
     */
    public boolean shouldReconnect() {
        if (!enabled) {
            return false;
        }
        // maxAttempts = -1 significa reintentar indefinidamente
        if (maxAttempts != -1 && attempts >= maxAttempts) {
            log.warn("Se agotaron los intentos de reconexión ({})", maxAttempts);
            return false;
        }
        return true;
    }

    /**
     * Espera el tiempo correspondiente al intento actual y luego
     * incrementa el contador de intentos.
     *
     * Debe llamarse justo antes de cada intento de reconexión.
     *
     * @throws InterruptedException si el hilo es interrumpido durante la espera.
     */
    public void waitBeforeNextAttempt() throws InterruptedException {
        long delay = calculateDelay();
        log.info("Reconexión en {}ms (intento {}/{})",
                delay,
                attempts + 1,
                maxAttempts == -1 ? "∞" : maxAttempts
        );
        Thread.sleep(delay);
        attempts++;
    }

    /**
     * Resetea el contador de intentos.
     * Se llama cuando la conexión se establece con éxito.
     */
    public void reset() {
        if (attempts > 0) {
            log.info("Conexión establecida. Reseteando contador de reconexión.");
        }
        attempts = 0;
    }

    /**
     * Deshabilita la reconexión automática.
     * Se llama cuando el usuario invoca stop() explícitamente.
     */
    public void disable() {
        enabled = false;
    }

    /**
     * Devuelve el número de intentos realizados hasta ahora.
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Calcula el tiempo de espera para el intento actual
     * usando backoff exponencial.
     *
     * Fórmula: min * 2^intentos, limitado al máximo configurado.
     *
     * @return Tiempo de espera en milisegundos.
     */
    private long calculateDelay() {
        // Math.pow(2, attempts) dobla el tiempo en cada intento
        // Math.min lo limita al máximo configurado
        long delay = (long) (minDelayMs * Math.pow(2, attempts));
        return Math.min(delay, maxDelayMs);
    }
}