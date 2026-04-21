package com.quictunnel.core;

/**
 * Representa un error ocurrido en el túnel.
 * Contiene el tipo de error y un mensaje descriptivo.
 * El tipo permite al juego reaccionar de forma diferente
 * según lo que haya ocurrido.
 */
public class TunnelError {

    /**
     * Tipos de error que puede producir el túnel.
     */
    public enum Type {

        /**
         * No se pudo establecer conexión con el servidor.
         * Causas típicas: servidor apagado, IP incorrecta, puerto bloqueado.
         */
        CONNECTION_FAILED,

        /**
         * El handshake mTLS falló.
         * Causas típicas: certificado inválido, caducado, o no firmado por la CA correcta.
         */
        HANDSHAKE_FAILED,

        /**
         * La conexión se perdió inesperadamente.
         * El túnel intentará reconectar automáticamente si está configurado para ello.
         */
        CONNECTION_LOST,

        /**
         * Se recibió un datagrama con el campo length incorrecto.
         * El datagrama se descarta, el juego es notificado por si le interesa saberlo.
         */
        INVALID_DATAGRAM,

        /**
         * La configuración del túnel es incorrecta o incompleta.
         * Causas típicas: falta algún certificado, puerto fuera de rango...
         * Este error ocurre antes de intentar conectar.
         */
        CONFIGURATION_ERROR,

        /**
         * Se agotaron los intentos de reconexión configurados.
         * El túnel pasa a estado ERROR y no intentará reconectar más.
         */
        MAX_RECONNECT_ATTEMPTS_REACHED
    }

    private final Type type;
    private final String message;
    private final Throwable cause;

    /**
     * @param type    Tipo de error
     * @param message Descripción legible del error
     * @param cause   Excepción original que causó el error, puede ser null
     */
    public TunnelError(Type type, String message, Throwable cause) {
        this.type = type;
        this.message = message;
        this.cause = cause;
    }

    public TunnelError(Type type, String message) {
        this(type, message, null);
    }

    public Type getType() { return type; }
    public String getMessage() { return message; }
    public Throwable getCause() { return cause; }

    @Override
    public String toString() {
        return "TunnelError{type=" + type + ", message='" + message + "'}";
    }
}