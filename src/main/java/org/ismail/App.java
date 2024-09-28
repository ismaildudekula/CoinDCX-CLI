package org.ismail;//package org.ismail;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Scanner;

public class App {

    private static double buyTriggerPrice;
    private static double sellTriggerPrice;
    private static String currentBuyOrderId;
    private static String currentSellOrderId;
    private static Socket socket;

    public static void main(String[] args) {

        // Get trigger prices from user
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Buy Trigger Price: ");
        buyTriggerPrice = scanner.nextDouble();
        System.out.print("Enter Sell Trigger Price: ");
        sellTriggerPrice = scanner.nextDouble();
        scanner.close();

        try {
            // Initialize socket connection
            socket = IO.socket("wss://stream.coindcx.com", new IO.Options() {{
                transports = new String[] {"websocket"};
            }});


            // On connection, join the orderbook channel
            socket.on(Socket.EVENT_CONNECT, args1 -> {
                log("INFO", "Connected to CoinDCX WebSocket.");
                JSONObject joinMessage = new JSONObject();
                joinMessage.put("channelName", "B-BTC_USDT@orderbook@10");
                socket.emit("join", joinMessage);
            });

            // On receiving depth-update, process the response
            socket.on("depth-update", objects -> {
                log("INFO", "Received depth-update data.");

                JSONObject response = (JSONObject) objects[0];
                String dataString = response.getString("data");
                JSONObject dataJson = new JSONObject(dataString);

                if(dataJson.has("asks")){
                    processDepthUpdate(dataJson);
                }else{
                    log("DATA","No Asks Found");
                }

            });

            // On disconnect
            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    log("INFO", "Disconnected from CoinDCX WebSocket.");
                }
            });

            //On error
            socket.on(Socket.EVENT_CONNECT_ERROR,args123 -> {
                    System.out.println("Connection Error: " + args123[0]);
            });

            log("INFO", "Attempting to connect to CoinDCX WebSocket...");
            socket.connect();

        } catch (URISyntaxException e) {
            log("ERROR", "Error in WebSocket connection: " + e.getMessage());
        }
    }

    // handles the depth update whenever we receive depth update
    private static void processDepthUpdate(JSONObject data) {
        try {
            JSONObject asks = data.getJSONObject("asks");
            JSONObject bids = data.getJSONObject("bids");

            // Log and process best ask price
            double bestAskPrice = findBestPrice(asks, true);
            log("INFO", "Best Ask Price: " + bestAskPrice);

            // Log and process best bid price
            double bestBidPrice = findBestPrice(bids, false);
            log("INFO", "Best Bid Price: " + bestBidPrice);

            // Check for buy/sell trigger conditions
            checkTriggers(bestBidPrice, bestAskPrice);

        } catch (Exception e) {
            log("ERROR", "Error while processing depth-update: " + e.getMessage());
        }
    }

    // Helper function to find the best price from a map (lowest ask, highest bid)
    private static double findBestPrice(JSONObject orders, boolean isAsk) {
        double bestPrice = isAsk ? Double.MAX_VALUE : 0.0;
        for (String price : orders.keySet()) {
            double priceValue = Double.parseDouble(price);
            double quantity = orders.getDouble(price);

            // Skip zero quantity orders
            if (quantity > 0) {
                if (isAsk) {
                    bestPrice = Math.min(bestPrice, priceValue);
                } else {
                    bestPrice = Math.max(bestPrice, priceValue);
                }
            }
        }
        return bestPrice;
    }

    // Check if market prices hit buy/sell triggers based on best bid and ask prices
    private static void checkTriggers(double bestBidPrice, double bestAskPrice) {

        log("INFO","Checking triggers...");
        // Check if best ask price is <= buy trigger price
        if (bestAskPrice <= buyTriggerPrice && currentBuyOrderId == null) {
            log("INFO", "Buy trigger hit! Preparing BUY order payload...");
            currentBuyOrderId = prepareBuyOrderPayload(bestBidPrice);
        }

        // Check if best bid price is >= sell trigger price
        if (bestBidPrice >= sellTriggerPrice && currentSellOrderId == null) {
            log("INFO", "Sell trigger hit! Preparing SELL order payload...");
            currentSellOrderId = prepareSellOrderPayload(bestAskPrice);
        }

        // Simulate order cancellation
        if (currentBuyOrderId != null && bestAskPrice > buyTriggerPrice) {
            log("INFO", "Buy price exceeded! Simulating cancellation of BUY order...");
            cancelOrder(currentBuyOrderId);
            currentBuyOrderId = null;
        }

        if (currentSellOrderId != null && bestBidPrice < sellTriggerPrice) {
            log("INFO", "Sell price fell below trigger! Simulating cancellation of SELL order...");
            cancelOrder(currentSellOrderId);
            currentSellOrderId = null;
        }
    }

    // Prepare a simulated buy order payload
    private static String prepareBuyOrderPayload(double price) {
        JSONObject buyOrder = new JSONObject();
        buyOrder.put("side", "buy");
        buyOrder.put("order_type", "limit_order");
        buyOrder.put("market", "BTCUSDT");
        buyOrder.put("price_per_unit", price);
        buyOrder.put("total_quantity", "0.001");

        long timestamp = System.currentTimeMillis() / 1000L;
        buyOrder.put("timestamp", timestamp);

        String orderId = java.util.UUID.randomUUID().toString();
        buyOrder.put("client_order_id", orderId);

        log("INFO", "Prepared Buy Order Payload: " + buyOrder);
        return orderId;
    }

    // Prepare a simulated sell order payload
    private static String prepareSellOrderPayload(double price) {
        JSONObject sellOrder = new JSONObject();
        sellOrder.put("side", "sell");
        sellOrder.put("order_type", "limit_order");
        sellOrder.put("market", "BTCUSDT");
        sellOrder.put("price_per_unit", price);
        sellOrder.put("total_quantity", "0.001");

        long timestamp = System.currentTimeMillis() / 1000L;
        sellOrder.put("timestamp", timestamp);

        String orderId = java.util.UUID.randomUUID().toString();
        sellOrder.put("client_order_id", orderId);

        log("INFO", "Prepared Sell Order Payload: " + sellOrder);
        return orderId;
    }

    // Simulate order cancellation
    private static void cancelOrder(String orderId) {
        log("INFO", "Order Cancelled. Order ID: " + orderId);
    }

    // Log method for consistent log messages
    private static void log(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }
}






//
//import io.socket.client.IO;
//import io.socket.client.Socket;
//import io.socket.emitter.Emitter;
//import java.net.URISyntaxException;
//
//public class App {
//    private Socket socket;
//
//    public App() {
//        try {
//            socket = IO.socket("wss://stream.coindcx.com", new IO.Options() {{
//                transports = new String[] {"websocket"};
//            }});
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void connect() {
//        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
//            @Override
//            public void call(Object... args) {
//                System.out.println("Connected!");
//            }
//        }).on("event", new Emitter.Listener() {
//            @Override
//            public void call(Object... args) {
//                System.out.println("Event received: " + args[0]);
//            }
//        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
//            @Override
//            public void call(Object... args) {
//                System.out.println("Disconnected! "+ args[0]);
//            }
//        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
//            @Override
//            public void call(Object... args) {
//                System.out.println("Error! "+ args[0]);
//            }
//        });
//
//        socket.connect();
//    }
//
//    public static void main(String[] args) {
//        App client = new App();
//        client.connect();
//    }
//}
//
