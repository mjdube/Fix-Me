package com.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Main {
    public static void main(String[] args) {
        try {
            SocketChannel market = SocketChannel.open(new InetSocketAddress("localhost",5001));
            market.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(1024);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
