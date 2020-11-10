package com.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class RouterConnection extends Thread {
    private List<Handler> componentList;
    private ArrayList<String> messages = new ArrayList<String>();

    private String hostIP = "127.0.0.1";
    private int port;

    private int brokerID = 10000;
    private int marketID = 50000;

    private int maxBrokerCount = 50000;
    private int maxMarketCount = 100000;

    public Handler socketHandlerAsync;

    private TheComp component;

    public ServerSocketChannel serverSocketChannel;
    public SocketChannel socketChannel;
    public Selector selector;

    public BufferedReader bufferedReader;

    private String componentId;

    public RouterConnection(int port, TheComp component) {
        this.port = port;
        this.component = component;
        this.componentList = new ArrayList<Handler>();
    }

    private String assignIdToComponent(TheComp component) {
        boolean hasError = false;
        int id = -1;

        if (component == TheComp.Broker) {
            id = this.brokerID++;
            if (id >= maxBrokerCount)
                hasError = true;
        } else if (component == TheComp.Market) {
            id = this.marketID++;
            if (id >= maxMarketCount)
                hasError = true;
        }

        if (hasError) {
            System.out.println(String.format("There are too many %s connections", component.toString()));
        }

        return Integer.toString(id);
    }

    private void runServer() {
        this.componentId = assignIdToComponent(this.component);

        try {
            serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress(this.hostIP, this.port));

            System.out.println(String.format("Server started on port: %d", this.port));

            while (true) {
                socketChannel = serverSocketChannel.accept();
                socketHandlerAsync = new Handler(socketChannel, this.componentList.size(), messages, this.port, this.componentId, this.component.toString());
                System.out.println(String.format("%s connected, Id: %s", this.component.toString(), this.componentId));
                this.componentList.add(socketHandlerAsync);
                this.socketHandlerAsync.start();
            }
        } catch (IOException e) {
            System.out.println("Disconnected");
        }
    }

    public void sendMessage(String str) {
        this.socketHandlerAsync.sendMessage(str);
    }

    public String getMessages() {
        return this.socketHandlerAsync.getMessages();
    }

    // return this server's ID
    public String getComponentId() {
        return this.componentId;
    }

    @Override
    public void run() {
        runServer();
    }
}
