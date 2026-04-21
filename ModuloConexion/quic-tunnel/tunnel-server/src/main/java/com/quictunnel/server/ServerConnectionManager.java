package com.quictunnel.server;

import com.quictunnel.core.TunnelConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona todas las TunnelConnection activas en el servidor.
 * Cuando un móvil se conecta, se registra aquí.
 * Cuando se desconecta, se elimina.
 *
 * Es thread-safe porque el servidor puede recibir conexiones
 * y desconexiones desde múltiples hilos de Netty simultáneamente.
 */
public class ServerConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ServerConnectionManager.class);

    /**
     * Mapa de conexiones activas.
     * Clave: ID de la conexión (String único por móvil)
     * Valor: la TunnelConnection correspondiente
     *
     * ConcurrentHashMap porque múltiples hilos de Netty pueden
     * añadir y eliminar conexiones al mismo tiempo.
     */
    private final ConcurrentHashMap<String, TunnelConnection> connections = new ConcurrentHashMap<>();

    /**
     * Registra una nueva conexión cuando un móvil completa el handshake mTLS.
     *
     * @param connection La conexión recién establecida.
     */
    public void add(TunnelConnection connection) {
        connections.put(connection.getId(), connection);
        log.info("Conexión registrada: {}. Total activas: {}", connection.getId(), connections.size());
    }

    /**
     * Elimina una conexión cuando el móvil se desconecta.
     *
     * @param connectionId El ID de la conexión a eliminar.
     */
    public void remove(String connectionId) {
        TunnelConnection removed = connections.remove(connectionId);
        if (removed != null) {
            log.info("Conexión eliminada: {}. Total activas: {}", connectionId, connections.size());
        }
    }

    /**
     * Devuelve una conexión por su ID.
     * Útil para enviar datos a un jugador concreto.
     *
     * @param connectionId El ID de la conexión.
     * @return La conexión, o null si no existe.
     */
    public TunnelConnection get(String connectionId) {
        return connections.get(connectionId);
    }

    /**
     * Devuelve todas las conexiones activas.
     * La colección es de solo lectura para evitar modificaciones externas.
     * Útil para broadcast: enviar a todos los jugadores a la vez.
     *
     * @return Colección inmutable de conexiones activas.
     */
    public Collection<TunnelConnection> getAll() {
        return Collections.unmodifiableCollection(connections.values());
    }

    /**
     * Devuelve el número de conexiones activas.
     */
    public int size() {
        return connections.size();
    }

    /**
     * Cierra todas las conexiones activas.
     * Se llama cuando el servidor se detiene con stop().
     */
    public void closeAll() {
        log.info("Cerrando {} conexiones activas...", connections.size());
        connections.values().forEach(TunnelConnection::close);
        connections.clear();
    }
}