package com.keemax.exchanges;

import com.google.gson.Gson;
import com.keemax.consts.MarketConst;
import com.keemax.model.Order;
import com.keemax.consts.ExchangeProperties;
import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    final static double MIN_TRADE_QUANTITY = 0;
    //retries for http requests
    final static int NUM_RETRIES = 3;

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

    public abstract Order getHighestBuy() throws IOException;

    public abstract String placeBuyOrder(Order order) throws IOException;

    public abstract String placeSellOrder(Order order) throws IOException;

    public abstract boolean cancelOrder(String orderId) throws IOException;

    public abstract List<Order> getOpenOrders() throws IOException;

    public abstract double getBuyFee();

    public abstract double getSellFee();

    public abstract double getBalanceBtc() throws IOException;

    public abstract double getBalanceDoge() throws IOException;

    public abstract String getName();

//    public abstract String getName();

    public void setMarket(MarketConst market) {
        String marketVal = mkts.get(market);
        if (marketVal == null) {
            System.err.println("exchange does not support this market");
        }
        this.market = marketVal;
    }

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

            } catch(IllegalStateException ise) {
                System.err.println("response was not valid json");
                return null;
            } catch(Exception exception) {
                wentThrough = false;
                numRetries++;
                if (numRetries > 3) {
                    System.err.println("request won't go through after 3 retries");
                    break;
                }
                System.err.println("something went wrong with the req/resp, retrying");
                exception.printStackTrace();
                try {
                    Thread.sleep(5000);
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

    void updateDepth(Map depth) {
        depthCache = depth;
        depthUpdated = System.currentTimeMillis();
    }

    boolean depthIsFresh() {
        return !(System.currentTimeMillis() - depthUpdated > 3000 || depthCache == null);
    }

    void updateWallets(Map wallets) {
        walletsCache = wallets;
        walletsUpdated = System.currentTimeMillis();
    }

    boolean walletsIsFresh() {
        return !(System.currentTimeMillis() - walletsUpdated > 3000 || walletsCache == null);
    }
}
