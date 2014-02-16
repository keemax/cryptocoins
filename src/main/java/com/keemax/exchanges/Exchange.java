package com.keemax.exchanges;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.keemax.consts.MarketConst;
import com.keemax.model.Order;
import com.keemax.consts.ExchangeProperties;
import org.apache.http.Consts;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/6/14
 * Time: 6:38 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Exchange {

    //minimum quantity for trade to be considered
    //I added this because trades with very small volume are likely to disappear before the bot can place an order
    //also even if the order is placed in time, trades will occur at the best possible rate
    final static double MIN_TRADE_QUANTITY = 10000;
    //retries for http requests
    final static int NUM_RETRIES = 8;

    CloseableHttpClient client;
    static long nonce;
    Gson gson;
    String market;
    ExchangeProperties props;
    Map<MarketConst, String> mkts;

    Map depthCache;
    Long depthUpdated;

    Map walletsCache;
    Long walletsUpdated;

    private final static ExecutorService orderPlacerPool = Executors.newFixedThreadPool(2);

    public Exchange() {
        client = HttpClients.createDefault();
        nonce = System.currentTimeMillis() / 1000;
        gson = new Gson();
        market = null;
        props = ExchangeProperties.getInstance();
        mkts = new HashMap<MarketConst, String>();

        depthUpdated = System.currentTimeMillis();
        walletsUpdated = System.currentTimeMillis();
    }

    public abstract Order getLowestSell() throws IOException;

    public abstract List<Order> getAllSellOrders() throws IOException;

    public abstract Order getHighestBuy() throws IOException;

    public abstract List<Order> getAllBuyOrders() throws IOException;

    public abstract String placeBuyOrder(Order order) throws IOException;

    public abstract String placeSellOrder(Order order) throws IOException;

    public abstract boolean cancelOrder(String orderId) throws IOException;

    public abstract List<Order> getOpenOrders() throws IOException;

    public abstract double getBuyFee();

    public abstract double getSellFee();

    public abstract double getBalanceBtc() throws IOException;

    public abstract double getBalanceDoge() throws IOException;

    public abstract String getName();

    abstract void updateWalletCache();

    abstract void updateDepthCache();

    public Future<String> placeSellOrderAsync(final Order order) {
        return orderPlacerPool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return placeSellOrder(order);
            }
        });
    }

    public Future<String> placeBuyOrderAsync(final Order order) {
        return orderPlacerPool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return placeBuyOrder(order);
            }
        });
    }

    public void setMarket(MarketConst market) {
        String marketVal = mkts.get(market);
        if (marketVal == null) {
            System.err.println("exchange does not support this market");
        }
        this.market = marketVal;
    }

    public void updateWallets(CountDownLatch latch) {
        new Thread(new WalletUpdater(latch)).start();
    }

    public void updateDepth(CountDownLatch latch) {
        new Thread(new DepthUpdater(latch)).start();
    }

    void forceUpdateWallets() {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new WalletUpdater(latch)).start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void forceUpdateDepth() {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(new DepthUpdater(latch)).start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void clearDepthCache() {
        depthCache = null;
    }

    public void clearWalletsCache() {
        walletsCache = null;
    }

    //tries to get rid of outliers because sometimes old orders stick around but can't be bought/sold
//    void cleanOrders(List<Order> orders) {
//        boolean clean = false;
//        while (!clean) {
//            clean = true;
//            for (int i = 0; i < 5 && i < orders.size() - 1; i++) {
////                System.out.println("checking rate " + orders.get(i + 1).getRate() + " vs rate " + orders.get(i).getRate());
//                double rateSpread = Math.abs(orders.get(i + 1).getRate() - orders.get(i).getRate());
//                double percentChange = rateSpread / orders.get(i + 1).getRate();
//                if (percentChange > 0.05) {
//                    System.out.println("removing order: " + orders.get(i).toString() + " because change is " + percentChange * 100 + "%");
//                    orders.remove(i);
//                    clean = false;
//                    break;
//                }
//            }
//        }
//    }

    Map executeRequest(HttpUriRequest request, String statusKey, Object equalsOnSuccess) throws IOException {
        CloseableHttpResponse resp = null;

        boolean wentThrough = false;
        int numRetries = 0;
        while(!wentThrough) {
            wentThrough = true;
            try {
                resp = client.execute(request);

                String respString = EntityUtils.toString(resp.getEntity(), Consts.UTF_8);
//                System.out.println(respString);
                Map respMap = gson.fromJson(respString, Map.class);

                if (equalsOnSuccess == null || respMap.get(statusKey).equals(equalsOnSuccess)) {
                    return respMap;
                }
                else {
                    System.err.println("bad request to " + getName() + ", response: " + respString);
                    return null;
                }
            } catch(Exception exception) {
                wentThrough = false;
                numRetries++;
                if (numRetries > NUM_RETRIES) {
                    System.err.println("request won't go through after " + NUM_RETRIES + " retries");
                    break;
                }
                long sleepTime = new Double(1000 * Math.pow(2, numRetries)).longValue();
                String errorMsg;
                if (exception instanceof NoHttpResponseException) {
                    errorMsg = "no response from server.";
                }
                else if (exception instanceof JsonSyntaxException) {
                    errorMsg = "response was not valid json.";
                }
                else {
                    errorMsg = "unknown error.";
                    exception.printStackTrace();
                }
                System.err.println(errorMsg + " retrying in " + sleepTime);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    System.err.println("too much coffee");
                }
            } finally {
                if (resp != null) {
                    resp.close();
                }
            }
        }
        return null;

    }

    class WalletUpdater implements Runnable {
        CountDownLatch latch;

        public WalletUpdater(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            updateWalletCache();
            latch.countDown();
        }
    }

    class DepthUpdater implements Runnable {
        CountDownLatch latch;

        public DepthUpdater(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            updateDepthCache();
            latch.countDown();
        }
    }


}
