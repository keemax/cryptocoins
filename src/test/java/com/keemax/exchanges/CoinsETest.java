package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/6/14
 * Time: 10:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoinsETest {

    public void xtestGetMarketData() {
        CoinsE coinsE = new CoinsE(MarketConst.DOGE_BTC);
//        coinsE.setMarket(CoinsE.MKT_FTC_BTC);

        try {
//            System.out.println("trying authenticated req");
//            coinsE.getAllWallets();
            System.out.println("******* DOGE_BTC ON COINS-E ********");

            System.out.println("lowest sell");
            Order lowestSell = coinsE.getLowestSell();
            System.out.println(lowestSell.toString());

            System.out.println("highest buy");
            Order highestBuy = coinsE.getHighestBuy();
            System.out.println(highestBuy.toString());
//            coinsE.getAllWallets();

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void xtestPlaceOrder() throws IOException {
        CoinsE coinsE = new CoinsE(MarketConst.DOGE_BTC);

        Order buyOrder = new Order();
        //buy 100 doge @ 0.00000010 btc ea.
        buyOrder.setRate(0.00000010);
        buyOrder.setQuantity(100);
        String orderId = coinsE.placeBuyOrder(buyOrder);
        System.out.println("buy order: " + orderId);

        Order sellOrder = new Order();
        //sell 100 doge at 0.01 btc ea. (one day!)
        sellOrder.setRate(0.01);
        sellOrder.setQuantity(100);
        orderId = coinsE.placeSellOrder(sellOrder);
        System.out.println("sell order: " + orderId);
    }

    public void xtestCancelOrder() throws IOException {
        CoinsE coinsE = new CoinsE(MarketConst.DOGE_BTC);
        coinsE.cancelOrder("B/0.00000010/6668092889366528");
        coinsE.cancelOrder("S/0.01000000/5609725169238016");
    }

    public void xtestGetBalances() throws IOException {
        CoinsE coinsE = new CoinsE(MarketConst.DOGE_BTC);
        System.out.println("Balance doge: " + coinsE.getBalanceDoge());
        System.out.println("Balanace btc: " + coinsE.getBalanceBtc());
    }

    public void testGetOpenOrders() throws IOException {
        CoinsE coinsE = new CoinsE(MarketConst.DOGE_BTC);
        List<Order> orders = coinsE.getOpenOrders();
        System.out.println(orders.size());
        System.out.println(orders.toString());
    }
}

