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
    private List<Handler> _componentList;
    private ArrayList<String> _messages = new ArrayList<String>();

    private String _hostIP = "127.0.0.1";
    private int _port;

    private int _brokerID = 10000;
    private int _marketID = 50000;

    private int _maxBrokerCount = 50000;
    private int _maxMarketCount = 100000;

    public Handler socketHandlerAsync;

    private TheComp _component;

    public ServerSocketChannel serverSocketChannel;
    public SocketChannel socketChannel;
    public Selector selector;

    public BufferedReader bufferedReader;

    private String _componentId;

    public RouterConnection(int port, TheComp component)
    {
        this._port = port;
        this._component= component;
        this._componentList = new ArrayList<Handler>();
    }

    private String assignIdToComponent(TheComp component)
    {
        boolean hasError = false;
        int id = -1;

        if (component == TheComp.Broker)
        {
            id = this._brokerID++;

            if (id >= _maxBrokerCount)
            {
                hasError = true;
            }
        }
        else if (component == TheComp.Market)
        {
            id = this._marketID++;

            if (id >= _maxMarketCount)
            {
                hasError = true;
            }
        }

        if (hasError)
        {
            System.out.println(String.format("There are too many %s connections",
                    component.toString()));
        }

        return Integer.toString(id);
    }

    private void runServer()
    {
        this._componentId = assignIdToComponent(this._component);

        try
        {
            serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress(this._hostIP, this._port));

            System.out.println(String.format("Server started on port: %d", this._port));

            while(true)
            {
                socketChannel = serverSocketChannel.accept();
                socketHandlerAsync = new Handler(socketChannel, this._componentList.size(),
                        _messages, this._port, this._componentId, this._component.toString());

                System.out.println(String.format("%s connected, Id: %s", this._component.toString(), this._componentId));

                this._componentList.add(socketHandlerAsync);
                this.socketHandlerAsync.start();
            }
        }
        catch (IOException e)
        {
            System.out.println("Disconnected");
        }
    }

    public void sendMessage(String str)
    {
        this.socketHandlerAsync.sendMessage(str);
    }

    public String getMessages()
    {
        return this.socketHandlerAsync.getMessages();
    }

    // return this server's ID
    public String getComponentId()
    {
        return this._componentId;
    }

    @Override
    public void run()
    {
        runServer();
    }
}
