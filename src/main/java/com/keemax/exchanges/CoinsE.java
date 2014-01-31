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
        checkUpdateWallets();
        Map balanceBtc = (Map) walletsCache.get("BTC");
        if (balanceBtc == null) {
            return 0;
        }
        return Double.parseDouble((String) balanceBtc.get("a"));
    }

    @Override
    public double getBalanceDoge() throws IOException {
        checkUpdateWallets();
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

    private String placeOrder(Order order, String orderType) throws IOException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("order_type", orderType));
        params.add(new BasicNameValuePair("rate", Double.toString(order.getRate())));
        params.add(new BasicNameValuePair("quantity", Double.toString(order.getQuantity())));

        Map resp = authenticatedHTTPRequest("/market/" + market + "/", "neworder", params);

        Map orderDetails = (Map) resp.get("order");

        return (String) orderDetails.get("id");
    }

    //update wallet cache
    public void checkUpdateWallets() throws IOException {
        if (!walletsIsFresh()) {
            Map resp = authenticatedHTTPRequest("/wallet/all/", "getwallets", null);
            updateWallets((Map) resp.get("wallets"));
        }

    }
    @SuppressWarnings("unchecked")
    private List<Order> getAllSellOrders() throws IOException {
        checkUpdateDepth();
        return mapOrdersForDepth((List<Map>) depthCache.get("asks"));

    }
    @SuppressWarnings("unchecked")
    private List<Order> getAllBuyOrders() throws IOException {
        checkUpdateDepth();
        return mapOrdersForDepth((List<Map>) depthCache.get("bids"));

    }

    //TODO: check for private method
    //update market depth cache
    private void checkUpdateDepth() throws IOException {
        if (!depthIsFresh()) {
            updateDepth((Map)httpRequest("depth").get("marketdepth"));
        }
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
        cleanOrders(orders);
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

    //sometimes there's old/glitchy orders with ridiculous rates that you can't trade for
    //this tries to weed them out
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
