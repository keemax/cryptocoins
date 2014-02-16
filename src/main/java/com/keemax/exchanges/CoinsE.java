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
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/6/14
 * Time: 10:17 PM
 * To change this template use File | Settings | File Templates.
 */

public class CoinsE extends Exchange {

    public CoinsE(MarketConst mkt) {
        mkts.put(MarketConst.DOGE_BTC, "DOGE_BTC");
        mkts.put(MarketConst.LTC_BTC, "LTC_BTC");
        mkts.put(MarketConst.FTC_BTC, "FTC_BTC");

        setMarket(mkt);
    }

    @Override
    public Order getLowestSell() throws IOException {
        List<Order> sellOrders = getAllSellOrders();
        return sellOrders.get(0);
    }

    @Override
    public Order getHighestBuy() throws IOException {
        List<Order> buyOrders = getAllBuyOrders();
        return buyOrders.get(0);
    }

    @Override
    public String placeBuyOrder(Order order) throws IOException {
        return placeOrder(order, "buy");
    }

    @Override
    public String placeSellOrder(Order order) throws IOException {
        return placeOrder(order, "sell");
    }

    //this returns unauthorized??
    @Override
    public boolean cancelOrder(String orderId) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("order_id", orderId));

        Map resp = authenticatedHTTPRequest("/market/" + market + "/", "cancelorder", params);

        return resp != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getOpenOrders() throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("filter", "active"));

        Map resp = authenticatedHTTPRequest("/market/" + market + "/", "listorders", params);

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
        Map balanceBtc = (Map) walletsCache.get("BTC");
        if (balanceBtc == null) {
            return 0;
        }
        return Double.parseDouble((String) balanceBtc.get("a"));
    }

    @Override
    public double getBalanceDoge() throws IOException {
        if (walletsCache == null) {
            forceUpdateWallets();
        }
        Map balanceDoge = (Map) walletsCache.get("DOGE");
        if (balanceDoge == null) {
            return 0;
        }
        return Double.parseDouble((String) balanceDoge.get("a"));
    }

    @Override
    public String getName() {
        return "coins-e";
    }

    @Override
    void updateWalletCache() {
        try {
            Map resp = authenticatedHTTPRequest("/wallet/all/", "getwallets", null);
            walletsCache = (Map) resp.get("wallets");
        } catch (IOException ioe) {
            System.err.println("unable to update wallets on " + getName());
            ioe.printStackTrace();
        }
    }

    //TODO: check for private method
    @Override
    void updateDepthCache() {
        try {
            depthCache = (Map) httpRequest("depth").get("marketdepth");
        } catch (IOException ioe) {
            System.err.println("unable to update depth on " + getName());
            ioe.printStackTrace();
        }
    }

    private String placeOrder(Order order, String orderType) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("order_type", orderType));
        params.add(new BasicNameValuePair("rate", Double.toString(order.getRate())));
        params.add(new BasicNameValuePair("quantity", Double.toString(order.getQuantity())));

        Map resp = authenticatedHTTPRequest("/market/" + market + "/", "neworder", params);

        Map orderDetails = (Map) resp.get("order");

        return (String) orderDetails.get("id");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllSellOrders() throws IOException {
        if (depthCache == null) {
            forceUpdateDepth();
        }
        return mapOrdersForDepth((List<Map>) depthCache.get("asks"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllBuyOrders() throws IOException {
        if (depthCache == null) {
            forceUpdateDepth();
        }
        return mapOrdersForDepth((List<Map>) depthCache.get("bids"));
    }

    //maps json orders from depth response to order objects
    private List<Order> mapOrdersForDepth(List<Map> rawOrders) {
        List<Order> orders = new ArrayList<Order>();
        for (Map rawOrder : rawOrders) {
            if ((Double) rawOrder.get("n") > 0) {
                Order thisOrder = new Order();
                thisOrder.setRate(Double.parseDouble((String) rawOrder.get("r")));
                thisOrder.setQuantity(Double.parseDouble((String) rawOrder.get("q")));
                thisOrder.setTotal(thisOrder.getQuantity() * thisOrder.getRate());
//                thisOrder.setTotal(Double.parseDouble((String) rawOrder.get("cq")) - cumulativeGlitch);
                if (thisOrder.getQuantity() > MIN_TRADE_QUANTITY)
                    orders.add(thisOrder);
            }
        }
//        cleanOrders(orders);
        return orders;
    }

    //maps json orders from list order response to order objects
    private List<Order> mapOrdersForListOrders(List<Map> rawOrders) {
        List<Order> orders = new ArrayList<Order>();
        for (Map rawOrder : rawOrders) {
            Order thisOrder = new Order();
            thisOrder.setRate(Double.parseDouble((String) rawOrder.get("rate")));
            thisOrder.setQuantity(Double.parseDouble((String) rawOrder.get("quantity_remaining")));
            thisOrder.setTotal(thisOrder.getQuantity() * thisOrder.getRate());
            orders.add(thisOrder);
        }
        return orders;
    }

    private Map authenticatedHTTPRequest(String path, String method, List<NameValuePair> params) throws IOException {
        HttpPost post = new HttpPost(props.get("coinse.url.private") + path);

        if( params == null) {
            params = new ArrayList<NameValuePair>();
        }

        params.add(new BasicNameValuePair("method", method));
        params.add(new BasicNameValuePair("nonce", Long.toString(++nonce)));

        String postData = HttpUtil.getStringFromPostParams(params);

        post.addHeader("key", props.get("coinse.key.public"));
        post.addHeader("sign", HttpUtil.encryptWithHmacSha512(postData, props.get("coinse.key.private")));

        UrlEncodedFormEntity requestBody = new UrlEncodedFormEntity(params, Consts.UTF_8);
        post.setEntity(requestBody);

        return executeRequest(post, "status", true);
    }

    private Map httpRequest(String method) throws IOException {
        HttpGet get = new HttpGet(props.get("coinse.url.public") + "/market" + "/" + market + "/" + method + "/");
        return executeRequest(get, "status", true);
    }
}
