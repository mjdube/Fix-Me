package com.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.CRC32;

public class Main {
    private static BufferedReader bufferedReader = null;
    private static ByteBuffer byteBuffer = null;
    protected SocketChannel client;
    protected ArrayList<String> messages = new ArrayList<>();
    public static final String host = "127.0.0.1";
    public static final int port = 5000;

    public static String ID = "";
    public static int targetID = 0;

    private static int time = 10;
    public static int choice = 1;

    public static void main(String[] args) throws Exception {


        Selector selector = Selector.open();

        SocketChannel socketChannel = SocketChannel.open();

        socketChannel.configureBlocking(false);

        socketChannel.connect(new InetSocketAddress(host, port));

        socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));


        while (true) {

            if (selector.select() > 0) {
                Iterator i = selector.selectedKeys().iterator();
                SelectionKey key = null;
                while (i.hasNext()) {
                    key = (SelectionKey) i.next();
                    i.remove();
                }
                if (processKey(key)) {
                    break;
                }
            }
        }
        socketChannel.close();
    }

    public static Boolean processKey(SelectionKey key) throws Exception {
        if (key.isConnectable()) {
            if (!processConnection(key)) {
                return true;
            }
        }
        if (key.isReadable()) {
            readableKey(key);
        }
        return false;
    }

    public static void readableKey(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        byteBuffer = ByteBuffer.allocate(1024);
        socketChannel.read(byteBuffer);
        String routerOutput = new String(byteBuffer.array()).trim();

        if (ID.isEmpty()) {
            ID = routerOutput;
            System.out.println(" Broker ID: " + routerOutput);
            Menu(socketChannel);
        } else {
            System.out.println(" Server response: " + routerOutput);
            setTime(routerOutput);
        }
        Menu(socketChannel);
    }

    public static Boolean processConnection(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try {
            while (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
            }
        } catch (IOException e) {
            key.cancel();
            return false;
        }
        return true;
    }

    public static void Menu(SocketChannel socketChannel) {
        while (true) {
            try {
                System.out.println(" Would you like to buy or sell 1 unit of time? \n Enter : 1 to buy or 2 to sell ");
                // get choice
                String input = bufferedReader.readLine();

                if (input.equalsIgnoreCase("1") || (input.equalsIgnoreCase("2") && time > 0)) {
                    input = setFix(input);
                    socketChannel.write(ByteBuffer.wrap(input.getBytes()));
                    return;
                } else if (input.equalsIgnoreCase("2") && time <= 0) {
                    System.out.println(" Nothing to sell ");
                } else {
                    System.out.println(" invalid input ");
                }

            } catch (IOException e) {
                System.out.println("IO Exception caught: " + e);
            }
        }
    }

    public static String setFix(String choice) {
        String fixed = ID + "|" + targetID + "|" + choice + "|" + Checksum(choice) + "|";
        return (fixed);
    }

    public static long Checksum(String fixedBody) {
        byte[] bytes = fixedBody.getBytes();
        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    public static void setTime(String routerOutput) {

        // split message to validate order status
        String[] routerOutputSplit = routerOutput.split("\\|");

        // if order succeful update time
        if (routerOutputSplit[0].equals("accepted")) {
            if (routerOutputSplit[3].equals("1")) {
                // if order way buy, increase time by one unit
                time++;
                System.out.println("time: " + time);
            } else {
                // if order was sell, decrease order by one unit
                time--;
                System.out.println("time: " + time);
            }
            System.out.println(" order was sucessful and completed ");
        } else {
            System.out.println(" order was unsuccessful ");
        }
    }

}

