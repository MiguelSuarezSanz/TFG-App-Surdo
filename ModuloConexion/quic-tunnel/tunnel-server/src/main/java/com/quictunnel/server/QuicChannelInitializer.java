package com.quictunnel.server;

import com.quictunnel.core.TunnelListener;
import io.netty.channel.ChannelInitializer;
import io.netty.incubator.codec.quic.QuicChannel;

import java.util.concurrent.Executor;

/**
 * Inicializador de canales QUIC.
 * Extraído como clase separada para evitar un bug del compilador
 * de Java 23 con clases anónimas genéricas de Netty.
 */
public class QuicChannelInitializer extends ChannelInitializer<QuicChannel> {

    private final ServerConnectionManager connectionManager;
    private final TunnelListener listener;
    private final Executor callbackExecutor;

    public QuicChannelInitializer(
            ServerConnectionManager connectionManager,
            TunnelListener listener,
            Executor callbackExecutor) {
        this.connectionManager = connectionManager;
        this.listener = listener;
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    protected void initChannel(QuicChannel quicChannel) {
        quicChannel.pipeline().addLast(
                new QuicConnectionHandler(
                        connectionManager,
                        listener,
                        callbackExecutor
                )
        );
    }
}