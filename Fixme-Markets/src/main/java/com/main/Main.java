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
import java.util.Scanner;
import java.util.zip.CRC32;

public class Main {
    private static ByteBuffer byteBuffer = null;

    static String message = null;

    public static int time = 100;
    public static String ID = "";

    public static final String host = "127.0.0.1";
    public static final int port = 5001;

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
        if (ID.isEmpty()) {
            ID = routerOutput;
            System.out.println(" Market ID: " + routerOutput);

        } else {
            // this is a message from the broker (request)
            System.out.println(" Message from broker: " + routerOutput);
            handleRequest(key, routerOutput);
        }
    }

    public static void handleRequest(SelectionKey key, String brokerRequest)
    {
        // get the channel associated with this key
        SocketChannel socketChannel = (SocketChannel) key.channel();
        // split the brokerRequest string (currently in fix format)
        String[] requestSplit = brokerRequest.split("\\|");
        // get broker request (buy/sell)
        String choice = requestSplit[2];

        // pre-allocate brokerRequest status (if market is able/unable to fulfill request)
        String requestStatus = "accepted";
        // if brokerRequest was buy
        if (choice.equals("1")) {
            // check that market has sufficient quantity available to sell
            if (time > 0) {
                // if sufficient, decrement as unit has been sold. BrokerRequestStatus pre-allocated 'accepted'
                time--;
            } else {
                // if market possesses insufficient quantity, change pre-allocated status to 'rejected'
                requestStatus = "rejected";
            }
            // if brokerRequest was sell
        } else {
            // increment market quantity, brokerRequestStatus already pre-allocated 'accepted'
            time++;
        }
        // append brokerRequestStatus to message for broker display
        String marketReturn = requestStatus +"|"+ brokerRequest;
        // wrap market response to a buffer for socket
        byteBuffer = ByteBuffer.wrap(marketReturn.getBytes());

        try{
            // write marketReturn to socket
            socketChannel.write(byteBuffer);
            System.out.println(" Message from market: " +marketReturn +"\n Completed");
            System.out.println(" market time: " +time);

        }
        catch(IOException e)
        {
            System.out.println("request failed");
        }
    }
}
