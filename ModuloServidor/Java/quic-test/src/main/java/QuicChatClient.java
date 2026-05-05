import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.*;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class QuicChatClient {

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 9999;

        // Configurar contexto SSL confiando en cualquier certificado (solo para pruebas)
        QuicSslContext sslContext = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols("chat-protocol")
                .build();

        EventLoopGroup group = new NioEventLoopGroup(1);

        try {
            // Configurar el Codec del Cliente QUIC
            ChannelHandler codec = new QuicClientCodecBuilder()
                    .sslContext(sslContext)
                    .maxIdleTimeout(10, TimeUnit.MINUTES)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .build();

            Bootstrap b = new Bootstrap();
            Channel channel = b.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(0).sync().channel();

            // Conectar al servidor
            QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                    .streamHandler(new ChannelInboundHandlerAdapter()) // Handler base para la conexión
                    .remoteAddress(new InetSocketAddress(host, port))
                    .connect()
                    .get();

            System.out.println("¡Conectado al servidor QUIC!");

            // Crear un Stream bidireccional para el chat
            QuicStreamChannel streamChannel = quicChannel.createStream(QuicStreamType.BIDIRECTIONAL,
                    new ChannelInitializer<QuicStreamChannel>() {
                        @Override
                        protected void initChannel(QuicStreamChannel ch) {
                            ch.pipeline().addLast(new StringDecoder(), new StringEncoder());
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    System.out.println("[Servidor responde]: " + msg);
                                }
                            });
                        }
                    }).get();

            // Bucle para leer de la consola y enviar al servidor
            @SuppressWarnings("resource")
            Scanner scanner = new Scanner(System.in);
            System.out.println("Escribe tus mensajes (escribe 'salir' para terminar):");
            for (;;) {
                String input = scanner.nextLine();
                if ("salir".equalsIgnoreCase(input)) {
                    break;
                }
                
                try {
                    // .sync() es la clave: si falla, lanzará una excepción aquí mismo
                    streamChannel.writeAndFlush(input).sync();
                    System.out.println("[Debug] Mensaje enviado a la capa de red.");
                } catch (Exception e) {
                    System.err.println("¡Explotó al enviar el mensaje!");
                    e.printStackTrace();
                }
            }

            streamChannel.close().sync();
            quicChannel.close().sync();

        } finally {
            group.shutdownGracefully();
        }
    }
}