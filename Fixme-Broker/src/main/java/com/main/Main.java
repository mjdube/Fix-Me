package com.main;

import com.sun.tools.jdeprscan.scan.Scan;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner input = new Scanner(System.in);
            String message = "";
            SocketChannel broker = SocketChannel.open(new InetSocketAddress("localhost",5000));
            broker.configureBlocking(false);

            message = input.next();

            getRespondsFromServer(message, broker);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getRespondsFromServer(String msg, SocketChannel channel) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.flip();
        buffer.clear();
        buffer.put(msg.getBytes());
        buffer.flip();
        channel.write(buffer);
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
    }
}
