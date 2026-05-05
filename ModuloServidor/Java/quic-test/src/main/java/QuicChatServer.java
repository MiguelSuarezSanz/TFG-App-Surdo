import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.*;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class QuicChatServer {

    public static void main(String[] args) throws Exception {
        int port = 9999;

        // Generar un certificado autofirmado para pruebas (QUIC requiere TLS)
        SelfSignedCertificate cert = new SelfSignedCertificate();
        QuicSslContext sslContext = QuicSslContextBuilder.forServer(cert.privateKey(), null, cert.certificate())
                .applicationProtocols("chat-protocol")
                .build();

        EventLoopGroup group = new NioEventLoopGroup(1);

        try {
        	
            //Configurar el Codec del Servidor QUIC
            ChannelHandler codec = new QuicServerCodecBuilder()
                    .sslContext(sslContext)
                    .maxIdleTimeout(10, TimeUnit.MINUTES) 
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .initialMaxStreamDataBidirectionalRemote(1000000)
                    .initialMaxStreamsBidirectional(100)
                    // Configurar qué pasa cuando un cliente abre un Stream
                    .streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                        @Override
                        protected void initChannel(QuicStreamChannel ch) {
                            // Añadimos decodificadores/codificadores de texto
                            ch.pipeline().addLast(new StringDecoder(), new StringEncoder());
                            // Lógica de nuestro chat
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    System.out.println("[Servidor] ¡Un cliente ha abierto un Stream con éxito!");
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    System.out.println("[Servidor] Cliente dice: " + msg);
                                    // Responder al cliente
                                    ctx.writeAndFlush("Servidor recibió: " + msg + "\n");
                                }
                            });
                        }
                    }).build();

            //Levantar el servidor en UDP
            Bootstrap b = new Bootstrap();
            Channel channel = b.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(new InetSocketAddress(port)).sync().channel();

            System.out.println("Servidor de Chat QUIC iniciado en el puerto UDP: " + port);
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}