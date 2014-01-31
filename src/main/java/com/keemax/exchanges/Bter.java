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
        return allSellOrders.get(allSellOrders.size() - 1);
    }

    @Override
    public Order getHighestBuy() throws IOException {
        List<Order> allBuyOrders = getAllBuyOrders();
        return allBuyOrders.get(0);
    }

    //TODO
    @Override
    public String placeBuyOrder(Order order) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
    @Override
    public String placeSellOrder(Order order) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
    @Override
    public boolean cancelOrder(String orderId) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
    @Override
    public List<Order> getOpenOrders() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return Double.parseDouble((String) walletsCache.get("BTC"));
    }

    @Override
    public double getBalanceDoge() throws IOException {
        checkUpdateWallets();
        return Double.parseDouble((String) walletsCache.get("DOGE"));
    }

    @Override
    public String getName() {
        return "bter";
    }

    private void checkUpdateWallets() throws IOException {
        if (!walletsIsFresh()) {
            Map resp = authenticatedHTTPRequest("getfunds", null);
            updateWallets((Map) resp.get("available_funds"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Order> getAllSellOrders() throws IOException {
        Map depth = getDepth();
        return mapOrders((List<List<Object>>) depth.get("asks"));

    }
    @SuppressWarnings("unchecked")
    private List<Order> getAllBuyOrders() throws IOException {
        Map depth = getDepth();
        return mapOrders((List<List<Object>>) depth.get("bids"));

    }

    private Map getDepth() throws IOException {
        return httpRequest("depth");
    }

    private List<Order> mapOrders(List<List<Object>> rawOrders) {
        List<Order> orders = new ArrayList<Order>();
        for (List<Object> rawOrder: rawOrders) {
            try {
                Order order = new Order();
                order.setRate(Double.parseDouble((String) rawOrder.get(0)));
                order.setQuantity((Double) rawOrder.get(1));
                order.setTotal(order.getRate() * order.getQuantity());
                orders.add(order);
            } catch (ClassCastException cce) {
                System.err.println("class cast exception");
            }
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

        return executeRequest(post, "result", "true");
    }

    private Map httpRequest(String method) throws IOException {
        HttpGet get = new HttpGet(props.get("bter.url.public") + "/" + method + "/" + market);
        return executeRequest(get, "result", "true");
    }
}
