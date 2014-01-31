package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/6/14
 * Time: 8:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CryptsyTest {


    public void xtestGetMarketInfo() {
        Cryptsy cryptsy = new Cryptsy(MarketConst.DOGE_BTC);

        try {
            System.out.println("******* DOGE_BTC ON CRYPTSY ********");

            System.out.println("lowest sell");
            Order lowestSell = cryptsy.getLowestSell();
            System.out.println(lowestSell.toString());

            System.out.println("highest buy");
            Order highestBuy = cryptsy.getHighestBuy();
            System.out.println(highestBuy.toString());

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void xtestPlaceOrder() {
        Cryptsy c = new Cryptsy(MarketConst.DOGE_BTC);
        Order o = new Order();
        o.setQuantity(100);
        o.setRate(0.01);
        try {
            String orderNo = c.placeSellOrder(o);
            System.out.println(orderNo);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void xtestCancelOrder() {
        Cryptsy c = new Cryptsy(MarketConst.DOGE_BTC);
        try {
            c.cancelOrder("30628876");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void xtestGetBalances() throws IOException {
        Cryptsy cryptsy = new Cryptsy(MarketConst.DOGE_BTC);
        System.out.println("doge balance: " + cryptsy.getBalanceDoge());
        System.out.println("btc balance: " + cryptsy.getBalanceBtc());
    }

    public void testGetOpenOrders() throws IOException {
        Cryptsy cryptsy = new Cryptsy(MarketConst.DOGE_BTC);
        List<Order> orders = cryptsy.getOpenOrders();
        System.out.println(orders.size());
        System.out.println(orders.toString());
    }


}
