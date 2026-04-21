package com.quictunnel.server;

import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelListener;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

public class TunnelServerTest {

    public static void main(String[] args) throws Exception {

        SelfSignedCertificate cert = new SelfSignedCertificate();
        System.out.println("Certificado generado para: " + cert.cert().getSubjectX500Principal());

        // Construimos el SSL context directamente con el objeto cert
        // en lugar de pasar por ficheros
        QuicSslContextBuilder builder = QuicSslContextBuilder.forServer(
                cert.key(),   // PrivateKey
                null,         // contraseña (null = sin contraseña)
                cert.cert()   // X509Certificate
        );
        builder.clientAuth(io.netty.handler.ssl.ClientAuth.REQUIRE);
        builder.trustManager(cert.cert());
        builder.applicationProtocols("quic-tunnel");
        QuicSslContext sslContext = builder.build();

        TunnelConfig config = TunnelConfig.builder()
                .port(4242)
                .caCert("in-memory")
                .cert("in-memory")
                .key("in-memory")
                .build();

        QuicTunnelServer server = new QuicTunnelServer(config, sslContext);

        server.setListener(new TunnelListener() {
            @Override
            public void onConnected(TunnelConnection connection) {
                System.out.println("[SERVER] Cliente conectado: " + connection.getId());
            }
            @Override
            public void onDataReceived(TunnelConnection connection, byte[] payload) {
                System.out.println("[SERVER] Datos recibidos: " + new String(payload));
            }
            @Override
            public void onDisconnected(TunnelConnection connection) {
                System.out.println("[SERVER] Cliente desconectado: " + connection.getId());
            }
            @Override
            public void onError(TunnelConnection connection, TunnelError error) {
                System.out.println("[SERVER] Error: " + error.getType()
                        + " - " + error.getMessage());
            }
        });

        server.start();
        System.out.println("[SERVER] Escuchando en puerto 4242...");
        System.out.println("[SERVER] Pulsa Enter para detener.");
        System.in.read();

        server.stop();
        System.out.println("[SERVER] Detenido.");
    }
}