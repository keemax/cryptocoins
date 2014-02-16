package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;
import com.keemax.exchanges.util.HttpUtil;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/7/14
 * Time: 10:48 PM
 * To change this template use File | Settings | File Templates.
 */

//bter trade api is currently borked
public class Bter extends Exchange {

    public Bter(MarketConst mkt) {
        mkts.put(MarketConst.BTC_USD, "btc_usd");
        mkts.put(MarketConst.LTC_BTC, "ltc_btc");
        mkts.put(MarketConst.DOGE_BTC, "doge_btc");

        setMarket(mkt);
    }

    @Override
    public Order getLowestSell() throws IOException {
        List<Order> allSellOrders = getAllSellOrders();
        return allSellOrders.get(0);
    }

    @Override
    public Order getHighestBuy() throws IOException {
        List<Order> allBuyOrders = getAllBuyOrders();
        return allBuyOrders.get(0);
    }

    @Override
    public String placeBuyOrder(Order order) throws IOException {
        return placeOrder(order, "BUY");
    }

    @Override
    public String placeSellOrder(Order order) throws IOException {
        return placeOrder(order, "SELL");
    }

    @Override
    public boolean cancelOrder(String orderId) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("order_id", orderId));

        Map resp = authenticatedHTTPRequest("cancelorder", params);

        return resp.get("msg").equals("Success");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getOpenOrders() throws IOException {
        Map resp = authenticatedHTTPRequest("orderlist", null);

        return mapOrdersForListOrders((List<Map>) resp.get("orders"));
    }

    @Override
    public double getBuyFee() {
        return 0.002;
    }

    @Override
    public double getSellFee() {
        return 0.002;
    }

    @Override
    public double getBalanceBtc() throws IOException {
        if (walletsCache == null) {
            forceUpdateWallets();
        }
        String stringValue = (String) walletsCache.get("BTC");
        if (stringValue == null) {
            return 0;
        }
        return Double.parseDouble(stringValue);
    }

    @Override
    public double getBalanceDoge() throws IOException {
        if (walletsCache == null) {
            forceUpdateWallets();
        }
        String stringValue = (String) walletsCache.get("DOGE");
        if (stringValue == null) {
            return 0;
        }
        return Double.parseDouble(stringValue);
    }

    @Override
    public String getName() {
        return "bter";
    }

    @Override
    void updateWalletCache() {
        try {
            Map resp = authenticatedHTTPRequest("getfunds", null);
            walletsCache = (Map) resp.get("available_funds");
        } catch (IOException ioe) {
            System.err.println("cannot update wallet for " + getName());
            ioe.printStackTrace();
        }
    }

    @Override
    void updateDepthCache() {
        try {
            depthCache = httpRequest("depth");
        } catch (IOException ioe) {
            System.err.println("can't update depth on " + getName());
            ioe.printStackTrace();
        }
    }

    private String placeOrder(Order order, String orderType) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("pair", market));
        params.add(new BasicNameValuePair("type", orderType));
        params.add(new BasicNameValuePair("rate", Double.toString(order.getRate())));
        params.add(new BasicNameValuePair("amount", Double.toString(order.getQuantity())));

        Map resp = authenticatedHTTPRequest("placeorder", params);
        String orderNumString = resp.get("order_id").toString();
        return orderNumString.substring(0, orderNumString.length() - 2);
    }



    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllSellOrders() throws IOException {
        if (depthCache == null) {
            forceUpdateDepth();
        }
        List<Order> allSellOrders = mapOrdersForDepth((List<List<Object>>) depthCache.get("asks"));
        Collections.reverse(allSellOrders);
        return allSellOrders;

    }
    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllBuyOrders() throws IOException {
        if (depthCache == null) {
            forceUpdateDepth();
        }
        return mapOrdersForDepth((List<List<Object>>) depthCache.get("bids"));

    }

    private List<Order> mapOrdersForDepth(List<List<Object>> rawOrders) {
        List<Order> orders = new ArrayList<Order>();
        for (List<Object> rawOrder: rawOrders) {
            try {
                Order order = new Order();
                order.setRate(Double.parseDouble((String) rawOrder.get(0)));
                order.setQuantity((Double) rawOrder.get(1));
                order.setTotal(order.getRate() * order.getQuantity());
                if (order.getQuantity() > MIN_TRADE_QUANTITY) {
                    orders.add(order);
                }
            } catch (ClassCastException cce) {
                System.err.println("class cast exception");
            }
        }
//        cleanOrders(orders);
        return orders;
    }

    //maps json response for order list to order objects
    private List<Order> mapOrdersForListOrders(List<Map> rawOrders) {
        List<Order> orders = new ArrayList<Order>();
        for (Map rawOrder : rawOrders) {
            Order thisOrder = new Order();
            if (rawOrder.get("sell_type").equals("BTC")) {
                thisOrder.setQuantity(Double.parseDouble((String) rawOrder.get("buy_amount")));
                thisOrder.setTotal(Double.parseDouble((String) rawOrder.get("sell_amount")));
            }
            else {
                thisOrder.setQuantity(Double.parseDouble((String) rawOrder.get("sell_amount")));
                thisOrder.setTotal(Double.parseDouble((String) rawOrder.get("buy_amount")));
            }
            thisOrder.setRate(thisOrder.getTotal() / thisOrder.getQuantity());
            orders.add(thisOrder);
        }
        return orders;
    }

    private Map authenticatedHTTPRequest(String method, List<NameValuePair> params) throws IOException {
        HttpPost post = new HttpPost(props.get("bter.url.private") + "/" + method);

        if( params == null) {
            params = new ArrayList<NameValuePair>();
        }

        params.add(new BasicNameValuePair("nonce", Long.toString(++nonce)));

        String postData = HttpUtil.getStringFromPostParams(params);

        post.addHeader("Key", props.get("bter.key.public"));
        post.addHeader("Sign", HttpUtil.encryptWithHmacSha512(postData, props.get("bter.key.private")));

        UrlEncodedFormEntity requestBody = new UrlEncodedFormEntity(params, Consts.UTF_8);
        post.setEntity(requestBody);

        Object checkAgainst;
        //sometimes value of result is a string, sometimes it's a boolean -_-
        if (method.equals("orderlist") || method.equals("placeorder")) {
            checkAgainst = true;
        }
        else {
            checkAgainst = "true";
        }
        return executeRequest(post, "result", checkAgainst);
    }

    private Map httpRequest(String method) throws IOException {
        HttpGet get = new HttpGet(props.get("bter.url.public") + "/" + method + "/" + market);
        return executeRequest(get, "result", "true");
    }
}
