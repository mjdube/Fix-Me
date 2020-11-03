package com.main;

import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class Main {
    public static Selector selector = null;

    public static void main(String[] args) {
        int ports[] = {5000, 5001};
        final String host = "localhost";

        try {
            selector = Selector.open();
            for (int port: ports) {
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                ServerSocket channel = serverChannel.socket();
                channel.bind(new InetSocketAddress(host, port));
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            }
            System.out.println("Router ready for connection...");
            while (true){
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()){
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable())
                        connectedPort(key, selector);
                    else if (key.isReadable())
                        handleRead(key, selector);
                }
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void connectedPort(SelectionKey key, Selector selector) throws IOException {
        System.out.println("Connecting...");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);

        if (server.socket().getLocalPort() == 5000)
            System.out.println("Broker has connected, Port 5000");
        else if (server.socket().getLocalPort() == 5001)
            System.out.println("Market has connected, Port 5001");
    }

    public static void handleRead(SelectionKey key, Selector select) throws IOException {
        System.out.println("Reading...");
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.flip();
        buffer.clear();

        if (client.socket().getLocalPort() == 5000){
            brokerToMarket(buffer, client);
            client.register(select, SelectionKey.OP_READ);
        }
        else if (client.socket().getLocalPort() == 5001){

            client.register(select, SelectionKey.OP_READ);
        }
    }

    public static void brokerToMarket(ByteBuffer buffer, SocketChannel channel) throws IOException{
        String msg = "Broker testing...";

        buffer.flip();
        buffer.clear();
        buffer.put(msg.getBytes());
        buffer.flip();
        buffer.rewind();
        channel.write(buffer);
    }
}
