package com.main;

import com.sun.tools.jdeprscan.scan.Scan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
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

        // selects between channels
        Selector selector = Selector.open();
        // create new channel for this connection
        SocketChannel socketChannel = SocketChannel.open();
        // set channel to be non-blocking
        socketChannel.configureBlocking(false);
        // associate newly created channel to the server on the router
        socketChannel.connect(new InetSocketAddress(host, port));
        // register this channel with the selector and specify the operations this
        // channel should be associated with
        socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        // initialise a reader for user input
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        // loop broker options until connection is closed
        while (true) {
            // get keys representing channels ready for IO
            if (selector.select() > 0) {
                // if a channel ready for IO exists, set iterator for keys
                Iterator i = selector.selectedKeys().iterator();
                // define selection key to hold selected key from selectedKeys
                SelectionKey key = null;

                // romove excess keys from list
                while (i.hasNext()) {
                    key = (SelectionKey) i.next();
                    i.remove();
                }

                // process key, if connection failed/ended exit
                if (processKey(key)) {
                    break;
                }
            }
        }
        // once connection is closed, close this channel
        socketChannel.close();
    }

    public static Boolean processKey(SelectionKey key) throws Exception {
        // if not connected already, finish connection
        if (key.isConnectable()) {
            // if connection failed/ended exit
            if (!processConnection(key)) {
                return true;
            }
        }

        // once connected, test if this channel is ready for reading
        if (key.isReadable()) {
            // if key is readable, carry out operations
            readableKey(key);
        }
        // return false to loop through next key set
        return false;
    }

    public static void readableKey(SelectionKey key) throws IOException {
        // get channel associated with this key
        SocketChannel socketChannel = (SocketChannel) key.channel();
        // initialise buffer to read from channel
        byteBuffer = ByteBuffer.allocate(1024);

        // very first message is the ID sent by router
        socketChannel.read(byteBuffer);
        String routerOutput = new String(byteBuffer.array()).trim();

        // if ID is not set, then this is the first message received ie.:ID
        if(ID.isEmpty()) {
            ID = routerOutput;
            System.out.println(" Broker ID: " + routerOutput);
            Menu(socketChannel);
        }
        else {
            // any message after ID is set is sale related
            System.out.println(" Server response: " + routerOutput);
            // update time
            setTime(routerOutput);
        }
        Menu(socketChannel);
    }

    // continually try to finish this channels connection
    public static Boolean processConnection(SelectionKey key) {
        // get channel associated with this key
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try {
            // try to finnish connection
            while (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
            }
        } catch (IOException e) {
            // deregister this key from selector
            key.cancel();
            return false;
        }
        return true;
    }

    // get user choice and write fixed choice to channels output stream (server that lives on the router)
    public static void Menu(SocketChannel socketChannel)
    {
        while (true) {
            try{
                System.out.println(" Would you like to buy or sell 1 unit of time? \n Enter : 1 to buy or 2 to sell ");
                // get choice
                String input = bufferedReader.readLine();

                if (input.equalsIgnoreCase("1") || (input.equalsIgnoreCase("2") && time > 0)) {
                    // set string as fix like notation
                    input = setFix(input);
                    // wrap input's byte array and write to channel's output stream
                    socketChannel.write(ByteBuffer.wrap(input.getBytes()));
                    return;
                } else if (input.equalsIgnoreCase("2") && time <= 0) {
                    System.out.println(" Nothing to sell ");
                } else {
                    System.out.println(" invalid input ");
                }

            } catch (IOException e){
                System.out.println("IO Exception caught: " + e);
            }
        }
    }

    // set string in fix like notation
    public static String setFix(String choice) {
        // ID = broker ID, targetID = market ID, choice = buy/sell
        String fixed = ID+"|"+targetID+"|"+choice+"|"+Checksum(choice)+"|";
        return (fixed);
    }

    // get checksum to attach to message
    public static long Checksum(String fixedBody) {
        // get byte array of string to be encoded
        byte[] bytes = fixedBody.getBytes();
        // initialise new crc32 object
        CRC32 crc32 = new CRC32();
        // update the new crc32 object with the string's array
        // starting at the first element and ending at its last
        crc32.update(bytes, 0, bytes.length);
        // return checksum value
        return crc32.getValue();
    }

    // update time after sale or purchase
    public static void setTime(String routerOutput){

        // split message to validate order status
        String[] routerOutputSplit = routerOutput.split("\\|");

        // if order succeful update time
        if(routerOutputSplit[0].equals("accepted")){
            if (routerOutputSplit[3].equals("1")){
                // if order way buy, increase time by one unit
                time++;
                System.out.println("time: "+time);
            } else {
                // if order was sell, decrease order by one unit
                time--;
                System.out.println("time: "+time);
            }
            System.out.println(" order was sucessful and completed ");
        } else {
            System.out.println(" order was unsuccessful ");
        }
    }

}

