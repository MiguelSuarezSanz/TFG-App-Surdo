## Resumen de todo lo decidido

### Arquitectura general
```
[Servidor Java - PC]  ←→  [Interfaz web del juego]
         ↕  QUIC Datagrams (UDP)
[App Android 1]  [App Android 2]  [App Android 3]
```

### Decisiones técnicas
| Aspecto | Decisión |
|---|---|
| Estructura | Monorepo Gradle |
| QUIC Servidor | Netty QUIC |
| QUIC Android | quiche (Cloudflare) - pendiente |
| Seguridad | mTLS con CA propia |
| Autorización | Certificado compartido ahora, único por instalación en el futuro 📌 |

### Formato del datagrama
```
┌──────────┬──────────┬─────────────────────────────────┐
│ version  │  length  │           payload               │
│  1 byte  │ 2 bytes  │        (opaco al túnel)         │
└──────────┴──────────┴─────────────────────────────────┘
```
- Versión desconocida → se entrega igualmente, el juego decide
- Length incorrecto → descarta + `onError()`

### Ciclo de vida
- Conexión **explícita** con `connect()`
- Keepalive via **QUIC nativo**
- Reconexión **automática** con backoff exponencial
- Estados: `IDLE` → `CONNECTING` → `CONNECTED` → `DISCONNECTED` → `RECONNECTING`

### Modelo de hilos
- Callbacks → **Executor configurable** por el juego
- `send()` → **thread-safe** desde cualquier hilo

### Estructura del proyecto
```
quic-tunnel/
├── tunnel-core/     # Interfaces comunes → QuicTunnel, TunnelListener,
│                    # TunnelConnection, TunnelConfig, TunnelException
├── tunnel-server/   # Netty QUIC, lado PC
├── tunnel-client/   # Android + quiche, lado móvil
└── tunnel-ca/       # Herramientas para gestionar certificados
```

### Archivos de tunnel-core (en progreso)
```
com/quictunnel/core/
├── TunnelListener.java   ✅ hecho
├── TunnelConnection.java
├── TunnelConfig.java
├── QuicTunnel.java
└── TunnelError.java
```

### Lo que el servidor debe tener en cuenta
- Proporcionar `server.crt`, `server.key`, `ca.crt`
- Implementar `TunnelListener`
- Gestionar las `TunnelConnection` activas
- Interpretar el payload
- Gestionar `TunnelErrorType`
- `send()` es thread-safe

### Lo que el cliente Android debe tener en cuenta
- Proporcionar `client.crt`, `client.key`, `ca.crt`
- Obtener IP/puerto del servidor por el medio que elija
- Implementar `TunnelListener`
- No tocar UI directamente desde callbacks salvo que configure el executor con el hilo principal
- Interpretar el payload
- Gestionar `TunnelErrorType`

---

¿Seguimos con `TunnelError.java`?
