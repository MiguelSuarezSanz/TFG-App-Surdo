package com.quictunnel.core;

/**
 * Excepción propia del túnel.
 * Se usa para no exponer excepciones internas de Netty o quiche
 * hacia el código del juego.
 *
 * Por ejemplo, si Netty lanza una excepción interna al intentar
 * enviar un datagrama, el túnel la captura y lanza una
 * TunnelException con un mensaje claro, ocultando los detalles
 * internos de la implementación.
 *
 * Esto es importante porque si el compañero del servidor ve una
 * excepción de Netty, no sabe qué hacer con ella. Si ve una
 * TunnelException sabe exactamente qué ocurrió en términos del túnel.
 */
public class TunnelException extends Exception {

    private final TunnelError.Type errorType;

    /**
     * @param errorType Tipo de error que causó la excepción
     * @param message   Descripción legible del error
     */
    public TunnelException(TunnelError.Type errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * @param errorType Tipo de error que causó la excepción
     * @param message   Descripción legible del error
     * @param cause     Excepción original que causó este error
     */
    public TunnelException(TunnelError.Type errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    /**
     * Devuelve el tipo de error para que el juego pueda
     * reaccionar de forma diferente según el caso.
     *
     * @return El tipo de error.
     */
    public TunnelError.Type getErrorType() {
        return errorType;
    }
}