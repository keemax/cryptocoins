package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/7/14
 * Time: 11:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class BterTest {

    public void testGetMarketInfo() {

        Bter bter = new Bter(MarketConst.DOGE_BTC);
//        bter.setMarket();

        try {
//            long startTime = System.currentTimeMillis();
//            bter.updateDepthCache();

            System.out.println("lowest sell");
            Order lowestSell = bter.getLowestSell();
            System.out.println(lowestSell);

            System.out.println("highest buy");
            Order highestBuy = bter.getHighestBuy();
            System.out.println(highestBuy);

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public void xtestGetBalances() throws IOException {
        Bter bter = new Bter(MarketConst.DOGE_BTC);
        try {
            System.out.println("btc: " + bter.getBalanceBtc());
            System.out.println("doge: " + bter.getBalanceDoge());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void xtestGetOpenOrders() throws IOException {
        Bter bter = new Bter(MarketConst.DOGE_BTC);
        List<Order> orders = bter.getOpenOrders();
        System.out.println("open orders:");
        for (Order order : orders) {
            System.out.println(order);
        }
    }

    public void xtestPlaceOrder() throws IOException {
        Bter bter = new Bter(MarketConst.DOGE_BTC);
        Order testOrder = new Order();
        testOrder.setRate(0.00000001);
        testOrder.setQuantity(500000);
        System.out.println("order id: " + bter.placeBuyOrder(testOrder));

    }
}
