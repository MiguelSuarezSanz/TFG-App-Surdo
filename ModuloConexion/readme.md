## Perfecto, resumen de decisiones tomadas

| Aspecto | Decisión |
|---|---|
| Estructura | Monorepo |
| Build | Gradle unificado |
| QUIC Servidor | Netty QUIC |
| QUIC Android | Por decidir más adelante |
| Seguridad | mTLS con CA propia |
| Autorización | Certificado único por instalación |

La seguridad que tienes planteada es sólida para empezar, siempre se puede refinar más adelante.


## Estructura del monorepo
```
quic-tunnel/
├── build.gradle              # Build raíz
├── settings.gradle           # Declara los módulos
├── tunnel-core/              # Interfaz común (QuicTunnel, TunnelConnection...)
│   └── build.gradle
├── tunnel-server/            # Netty QUIC, lado PC
│   └── build.gradle
├── tunnel-client/            # Android, lado móvil
│   └── build.gradle
└── tunnel-ca/                # Herramientas para gestionar certificados
    └── build.gradle
```
