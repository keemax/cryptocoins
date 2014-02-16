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
 * Date: 1/8/14
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class CryptoTrade extends Exchange {

    public CryptoTrade(MarketConst mkt) {
        mkts.put(MarketConst.LTC_BTC, "ltc_btc");
        mkts.put(MarketConst.BTC_USD, "btc_usd");
        mkts.put(MarketConst.LTC_USD, "ltc_usd");
        mkts.put(MarketConst.DOGE_BTC, "doge_usd");

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

    //TODO
    @Override
    public String placeBuyOrder(Order order) throws IOException {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
    @Override
    public String placeSellOrder(Order order) throws IOException {
        return "";  //To change body of implemented methods use File | Settings | File Templates.
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

    //TODO
    @Override
    public double getBuyFee() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
    @Override
    public double getSellFee() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
    @Override
    public double getBalanceBtc() throws IOException {
        Map getInfoResp = authenticatedHTTPRequest("getinfo", null);
        return Double.parseDouble((String)((Map) getInfoResp.get("funds")).get("btc"));
    }

    //TODO
    @Override
    public double getBalanceDoge() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return "crypto-trade";
    }

    @Override
    void updateWalletCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void updateDepthCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllSellOrders() throws IOException {
        Map depth = getDepth();
        return mapOrders((List<List<String>>) depth.get("asks"));

    }
    @SuppressWarnings("unchecked")
    @Override
    public List<Order> getAllBuyOrders() throws IOException {
        Map depth = getDepth();
        return mapOrders((List<List<String>>) depth.get("bids"));

    }

    private Map getDepth() throws IOException {
        return httpRequest("depth");
    }

    private List<Order> mapOrders(List<List<String>> rawOrders) {
        List<Order> orders = new ArrayList<Order>();
        for (List<String> rawOrder: rawOrders) {
            Order order = new Order();
            order.setRate(Double.parseDouble(rawOrder.get(0)));
            order.setQuantity(Double.parseDouble(rawOrder.get(1)));
            order.setTotal(order.getRate() * order.getQuantity());
            orders.add(order);
        }
        return orders;
    }

    private Map authenticatedHTTPRequest(String method, List<NameValuePair> params) throws IOException {
        HttpPost post = new HttpPost(props.get("cryptotrade.url.private") + "/" + method);

        if( params == null) {
            params = new ArrayList<NameValuePair>();
        }

        params.add(new BasicNameValuePair("nonce", Long.toString(++nonce)));

        String postData = HttpUtil.getStringFromPostParams(params);

        post.addHeader("AuthKey", props.get("cryptotrade.key.public"));
        post.addHeader("AuthSign", HttpUtil.encryptWithHmacSha512(postData, props.get("cryptotrade.key.private")));

        UrlEncodedFormEntity requestBody = new UrlEncodedFormEntity(params, Consts.UTF_8);
        post.setEntity(requestBody);

        Map respMap = executeRequest(post, "status", "success");
        if (respMap == null) {
            return null;
        }
        else {
            return (Map) respMap.get("data");
        }
    }

    private Map httpRequest(String method) throws IOException {
        HttpGet get = new HttpGet(props.get("cryptotrade.url.public") + "/" + method + "/" + market);
        return executeRequest(get, "status", null);
    }

}
