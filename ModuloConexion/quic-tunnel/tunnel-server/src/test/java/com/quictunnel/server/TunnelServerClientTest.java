package com.quictunnel.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicClientCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.quictunnel.core.TunnelConfig;
import com.quictunnel.core.TunnelConnection;
import com.quictunnel.core.TunnelError;
import com.quictunnel.core.TunnelListener;

/**
 * Prueba de integración end-to-end.
 * Arranca el servidor y un cliente Netty QUIC en el mismo proceso
 * para verificar que se conectan e intercambian datagramas.
 *
 * Este cliente es temporal, solo para pruebas. El cliente real
 * será Android con quiche.
 */
public class TunnelServerClientTest {

    // Versión del protocolo del túnel
    private static final byte PROTOCOL_VERSION = 0x01;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Prueba end-to-end servidor + cliente Netty ===");

        // Generamos el certificado autofirmado compartido
        SelfSignedCertificate cert = new SelfSignedCertificate();
        System.out.println("Certificado generado para: "
                + cert.cert().getSubjectX500Principal());

        // ─────────────────────────────────────────────
        // Arrancamos el servidor
        // ─────────────────────────────────────────────
        QuicSslContextBuilder serverBuilder = QuicSslContextBuilder.forServer(
                cert.key(), null, cert.cert()
        );
        serverBuilder.clientAuth(io.netty.handler.ssl.ClientAuth.REQUIRE);
        serverBuilder.trustManager(cert.cert());
        serverBuilder.applicationProtocols("quic-tunnel");
        QuicSslContext serverSslContext = serverBuilder.build();

        TunnelConfig serverConfig = TunnelConfig.builder()
                .port(4243)
                .caCert("in-memory")
                .cert("in-memory")
                .key("in-memory")
                .build();

        QuicTunnelServer server = new QuicTunnelServer(serverConfig, serverSslContext);
        server.setListener(new TunnelListener() {
            @Override
            public void onConnected(TunnelConnection connection) {
                System.out.println("[SERVER] Cliente conectado: " + connection.getId());
            }
            @Override
            public void onDataReceived(TunnelConnection connection, byte[] payload) {
                System.out.println("[SERVER] Datos recibidos: " + new String(payload));

                // Respondemos al cliente
                try {
                    connection.send("Hola cliente!".getBytes());
                } catch (Exception e) {
                    System.out.println("[SERVER] Error al responder: " + e.getMessage());
                }
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
        System.out.println("[SERVER] Escuchando en puerto 4243...");

        // Pequeña espera para que el servidor esté listo
        Thread.sleep(500);

        // ─────────────────────────────────────────────
        // Arrancamos el cliente Netty QUIC
        // ─────────────────────────────────────────────

        // El cliente acepta cualquier certificado para pruebas
        // (InsecureTrustManagerFactory)
        QuicSslContextBuilder clientBuilder = QuicSslContextBuilder.forClient();
        clientBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        clientBuilder.keyManager(cert.key(), null, cert.cert());
        clientBuilder.applicationProtocols("quic-tunnel");
        QuicSslContext clientSslContext = clientBuilder.build();

        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            io.netty.channel.ChannelHandler clientCodec = new QuicClientCodecBuilder()
                    .sslContext(clientSslContext)
                    .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                    .maxRecvUdpPayloadSize(65535)
                    .maxSendUdpPayloadSize(65535)
                    .datagram(65535, 65535)
                    .build();

            Bootstrap clientBootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(clientCodec);

            // Conectamos al servidor
            Channel channel = clientBootstrap
                    .bind(new InetSocketAddress(0))
                    .sync()
                    .channel();

            QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                    .handler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            if (msg instanceof ByteBuf) {
                                ByteBuf buf = (ByteBuf) msg;
                                if (buf.readableBytes() >= 3) {
                                    buf.readByte();  // version
                                    buf.readShort(); // length
                                    byte[] payload = new byte[buf.readableBytes()];
                                    buf.readBytes(payload);
                                    System.out.println("[CLIENT] Respuesta del servidor: "
                                            + new String(payload));
                                }
                                buf.release();
                            }
                        }
                    })
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 4243))
                    .connect()
                    .get();

            System.out.println("[CLIENT] Conectado al servidor.");

            // Enviamos un datagrama con cabecera [version|length|payload]
            String mensaje = "Hola servidor desde cliente Netty!";
            byte[] payload = mensaje.getBytes();
            ByteBuf datagram = Unpooled.buffer(3 + payload.length);
            datagram.writeByte(PROTOCOL_VERSION);
            datagram.writeShort(payload.length);
            datagram.writeBytes(payload);

            quicChannel.writeAndFlush(datagram).sync();
            System.out.println("[CLIENT] Datagrama enviado: " + mensaje);

            // Esperamos a recibir respuesta
            Thread.sleep(2000);

            // Cerramos
            quicChannel.close().sync();
            channel.close().sync();
            System.out.println("[CLIENT] Desconectado.");

        } finally {
            group.shutdownGracefully();
            server.stop();
            System.out.println("[SERVER] Detenido.");
            System.out.println("=== Prueba completada ===");
            System.exit(0);
        }
    }
}