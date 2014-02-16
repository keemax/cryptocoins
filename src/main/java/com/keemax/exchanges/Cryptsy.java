package com.keemax.exchanges;


import com.keemax.consts.MarketConst;
import com.keemax.model.Order;
import com.keemax.exchanges.util.HttpUtil;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/6/14
 * Time: 6:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class Cryptsy extends Exchange {

    //need to keep track of an extra identifier for markets because cryptsy uses numerical market IDs instead of
    //string currency pairs
    private Map<String, String> mktKeyMap;

    public Cryptsy(MarketConst mkt) {
        mkts.put(MarketConst.DOGE_BTC, "132");
        mkts.put(MarketConst.LTC_BTC, "3");

        mktKeyMap = new HashMap<String, String>();
        mktKeyMap.put("132", "DOGE");
        mktKeyMap.put("3", "LTC");

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
        return placeOrder(order, "Buy");
    }

    @Override
    public String placeSellOrder(Order order) throws IOException {
        return placeOrder(order, "Sell");
    }

    @Override
    public boolean cancelOrder(String orderId) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("orderid", orderId));

        Map resp = authenticatedHTTPRequest("cancelorder", params);

        return resp != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getOpenOrders() throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("marketid", market));

        Map resp = authenticatedHTTPRequest("myorders", params);

        List<Map> rawOrders = (List<Map>) resp.get("return");

        return mapOrdersForListOrders(rawOrders);
    }

    @Override
    public double getBuyFee() {
        return 0.002;
    }

    @Override
    public double getSellFee() {
        return 0.003;
    }

    @Override
    public double getBalanceBtc() throws IOException {
        if (walletsCache == null) {
            forceUpdateWallets();
        }
        String balanceString = (String) walletsCache.get("BTC");
        if (balanceString == null) {
            return 0;
        }
        return Double.parseDouble(balanceString);
    }

    @Override
    public double getBalanceDoge() throws IOException {
        if (walletsCache == null) {
            forceUpdateWallets();
        }
        String balanceString = (String) walletsCache.get("DOGE");
        if (balanceString == null) {
            return 0;
        }
        return Double.parseDouble(balanceString);
    }

    @Override
    public String getName() {
        return "cryptsy";
    }

    @Override
    void updateWalletCache() {
        try {
            Map resp = authenticatedHTTPRequest("getinfo", null);
            walletsCache = (Map) ((Map) resp.get("return")).get("balances_available");
        } catch (IOException ioe) {
            System.err.println("unable to update wallets on " + getName());
            ioe.printStackTrace();
        }
    }

    @Override
    void updateDepthCache() {
        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("marketid", market));
            depthCache = (Map)authenticatedHTTPRequest("marketorders", params).get("return");
        } catch (IOException ioe) {
            System.err.println("unable to update depth on " + getName());
            ioe.printStackTrace();
        }
    }

    private String placeOrder(Order order, String orderType) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("marketid", market));
        params.add(new BasicNameValuePair("ordertype", orderType));
        params.add(new BasicNameValuePair("quantity", Double.toString(order.getQuantity())));
        params.add(new BasicNameValuePair("price", Double.toString(order.getRate())));

        Map resp = authenticatedHTTPRequest("createorder", params);
        String orderId = (String) resp.get("orderid");

        return orderId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllSellOrders() throws IOException {
        if (depthCache == null) {
            forceUpdateDepth();
        }
        return mapOrdersForDepth((List) depthCache.get("sellorders"), true);

    }
    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllBuyOrders() throws IOException {
        if (depthCache == null) {
            forceUpdateDepth();
        }
        return mapOrdersForDepth((List) depthCache.get("buyorders"), false);

    }

    //maps json response for market depth to order objects
    private List<Order> mapOrdersForDepth(List<Map> rawOrders, boolean isSellOrder) {
        List<Order> orders = new ArrayList<Order>();
        for (Map rawOrder : rawOrders) {

            Order thisOrder = new Order();
            if (isSellOrder) {
                thisOrder.setRate(Double.parseDouble((String) rawOrder.get("sellprice")));
            }
            else {
                thisOrder.setRate(Double.parseDouble((String) rawOrder.get("buyprice")));
            }
            thisOrder.setQuantity(Double.parseDouble((String) rawOrder.get("quantity")));
            thisOrder.setTotal(Double.parseDouble((String) rawOrder.get("total")));
            if (thisOrder.getQuantity() > MIN_TRADE_QUANTITY) {
                orders.add(thisOrder);
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
            thisOrder.setRate(Double.parseDouble((String) rawOrder.get("price")));
            thisOrder.setQuantity(Double.parseDouble((String) rawOrder.get("quantity")));
            thisOrder.setTotal(Double.parseDouble((String) rawOrder.get("total")));
            orders.add(thisOrder);
        }
        return orders;
    }

    private Map authenticatedHTTPRequest(String method, List<NameValuePair> params) throws IOException {
        HttpPost post = new HttpPost(props.get("cryptsy.url.private"));

        if( params == null) {
            params = new ArrayList<NameValuePair>();
        }

        params.add(new BasicNameValuePair("nonce", Long.toString(++nonce)));
        params.add(new BasicNameValuePair("method", method));

        String postData = HttpUtil.getStringFromPostParams(params);

        post.addHeader("Key", props.get("cryptsy.key.public"));
        post.addHeader("Sign", HttpUtil.encryptWithHmacSha512(postData, props.get("cryptsy.key.private")));

        UrlEncodedFormEntity requestBody = new UrlEncodedFormEntity(params, Consts.UTF_8);
        post.setEntity(requestBody);

        return executeRequest(post, "success", "1");
    }

//    private Map httpRequest(String method) throws IOException {
//        HttpGet get = new HttpGet(props.get("cryptsy.url.public") + "?method=" + method + "&marketid=" + market);
//        Map respMap = executeRequest(get, "success", 1d);
//        if (respMap == null) {
//            return respMap;
//        }
//        else {
//            return (Map) respMap.get("return");
//        }
//    }

}
