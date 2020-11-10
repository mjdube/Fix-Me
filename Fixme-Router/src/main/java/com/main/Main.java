package com.main;

public class Main {
    public static final int brokerPort = 5000;
    public static final int marketPort = 5001;

    private static String brokerMessages = "";
    private static String marketMessages = "";

    public static void main(String[] args) {
        // new Servers to connect market and broker to router
        RouterConnection brokerServer = new RouterConnection(brokerPort, TheComp.Broker);
        RouterConnection marketServer = new RouterConnection(marketPort, TheComp.Market);

        // start new threads for market and broker servers
        // allows market and broker servers to receive messages concurrently
        brokerServer.start();
        marketServer.start();

        // while threads do not return interrupted Thread exception, continually check for messages
        while (true) {
            try {
                // get messages that broker client has sent to brokerServer
                brokerMessages = brokerServer.getMessages();

                if (brokerMessages.isEmpty())
                    System.out.println("No messages");
                else {
                    // split broker message (broker request should be in fix format)
                    String[] arr = brokerMessages.split("\\|");
                    // get ID assigned to current market
                    String targetID = marketServer.getComponentId();
                    // insert Market ID as Broker Request targetID
                    String brokerMessageTargeted = arr[0] + "|" + targetID + "|" + arr[2] + "|" + arr[3] + "|";
                    marketServer.sendMessage(brokerMessageTargeted);
                    brokerMessages = "";
                }
                marketMessages = marketServer.getMessages();
                if (marketMessages.isEmpty())
                    brokerServer.sendMessage(marketMessages);
                else {
                    brokerServer.sendMessage(marketMessages);
                    marketMessages = "";
                    System.out.println("Order processed");
                }
            } catch (Exception e) {

            }
        }
    }
}
