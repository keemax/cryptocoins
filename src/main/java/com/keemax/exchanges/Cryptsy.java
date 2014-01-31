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
        checkUpdateWallets();
        String balanceString = (String) walletsCache.get("BTC");
        if (balanceString == null) {
            return 0;
        }
        return Double.parseDouble(balanceString);
    }

    @Override
    public double getBalanceDoge() throws IOException {
        checkUpdateWallets();
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

    //update wallet cache
    public void checkUpdateWallets() throws IOException {
        if (!walletsIsFresh()) {
            Map resp = authenticatedHTTPRequest("getinfo", null);
            updateWallets((Map) ((Map) resp.get("return")).get("balances_available"));
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
    private List<Order> getAllSellOrders() throws IOException {
        checkUpdateDepth();
        return mapOrdersForDepth((List) depthCache.get("sellorders"), true);

    }
    @SuppressWarnings("unchecked")
    private List<Order> getAllBuyOrders() throws IOException {
        checkUpdateDepth();
        return mapOrdersForDepth((List) depthCache.get("buyorders"), false);

    }

    //update market depth cache
    private void checkUpdateDepth() throws IOException {
        if (!depthIsFresh()) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("marketid", market));
            updateDepth((Map)authenticatedHTTPRequest("marketorders", params).get("return"));
        }

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
        cleanOrders(orders);
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

    //tries to get rid of outliers because sometimes old orders stick around but can't be bought/sold
    private void cleanOrders(List<Order> orders) {
        boolean clean = false;
        while (!clean) {
            clean = true;
            for (int i = 0; i < 5 && i < orders.size() - 1; i++) {
//                System.out.println("checking rate " + orders.get(i + 1).getRate() + " vs rate " + orders.get(i).getRate());
                double rateSpread = Math.abs(orders.get(i + 1).getRate() - orders.get(i).getRate());
                double percentChange = rateSpread / orders.get(i + 1).getRate();
                if (percentChange > 0.05) {
//                    System.out.println("removing order: " + orders.get(i).toString() + " because change is " + percentChange * 100 + "%");
                    orders.remove(i);
                    clean = false;
                    break;
                }
            }
        }
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
