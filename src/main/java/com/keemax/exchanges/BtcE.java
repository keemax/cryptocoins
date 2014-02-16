package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;
import com.keemax.exchanges.util.HttpUtil;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/7/14
 * Time: 6:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class BtcE extends Exchange {

    public BtcE(MarketConst mkt) {
        mkts.put(MarketConst.BTC_USD, "btc_usd");
        mkts.put(MarketConst.LTC_BTC, "ltc_btc");
        mkts.put(MarketConst.LTC_USD, "ltc_usd");

        setMarket(mkt);
    }

    @Override
    public Order getLowestSell() throws IOException {
        return getAllSellOrders().get(0);
    }

    @Override
    public Order getHighestBuy() throws IOException {
        return getAllBuyOrders().get(0);
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
        Map getInfoResp = authenticatedHTTPRequest("getInfo", null);
        return (Double)((Map) getInfoResp.get("funds")).get("btc");
    }

    //TODO
    @Override
    public double getBalanceDoge() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return "btc-e";
    }

    @Override
    void updateWalletCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    void updateDepthCache() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public Double getBalanceUSD() throws IOException {
        Map getInfoResp = authenticatedHTTPRequest("getInfo", null);
        return (Double)((Map) getInfoResp.get("funds")).get("usd");
    }

    @SuppressWarnings("unchecked")
    public List<Order> getAllSellOrders() throws IOException {
        Map depth = getDepth();
        return mapOrders((List<List<Double>>) depth.get("asks"));

    }
    @SuppressWarnings("unchecked")
    public List<Order> getAllBuyOrders() throws IOException {
        Map depth = getDepth();
        return mapOrders((List<List<Double>>) depth.get("bids"));

    }

    private Map getDepth() throws IOException {
        String depthResp = httpRequest("depth");
        return gson.fromJson(depthResp, Map.class);
    }

    private List<Order> mapOrders(List<List<Double>> rawOrders) {
        List<Order> orders = new ArrayList<Order>();
        for (List<Double> rawOrder: rawOrders) {
            Order order = new Order();
            order.setRate(rawOrder.get(0));
            order.setQuantity(rawOrder.get(1));
            order.setTotal(order.getRate() * order.getQuantity());
            orders.add(order);
        }
        return orders;
    }

    private Map authenticatedHTTPRequest(String method, List<NameValuePair> params) throws IOException {
        HttpPost post = new HttpPost(props.get("btce.url.private"));

        if( params == null) {  // If the user provided no arguments, just create an empty argument array.
            params = new ArrayList<NameValuePair>();
        }

        params.add(new BasicNameValuePair("method", method));  // Add the method to the post data.
        params.add(new BasicNameValuePair("nonce", Long.toString(++nonce)));  // Add the nonce.

        String postData = HttpUtil.getStringFromPostParams(params);

        post.addHeader("Key", props.get("btce.key.public"));
        post.addHeader("Sign", HttpUtil.encryptWithHmacSha512(postData, props.get("btce.key.private")));

        UrlEncodedFormEntity requestBody = new UrlEncodedFormEntity(params, Consts.UTF_8);
        post.setEntity(requestBody);

        Map respMap = executeRequest(post, "success", 1d);
        if (respMap == null) {
            return null;
        }
        else {
            return (Map) respMap.get("return");
        }

    }

    private String httpRequest(String method) throws IOException {
        HttpGet get = new HttpGet(props.get("btce.url.public") + "/" + market + "/" + method);
        CloseableHttpResponse resp = client.execute(get);

        try {
            return EntityUtils.toString(resp.getEntity(), Consts.UTF_8);
        } finally {
            resp.close();
        }
    }
}
