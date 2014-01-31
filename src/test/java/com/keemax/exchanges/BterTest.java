package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/7/14
 * Time: 11:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class BterTest {

    public void xtestGetMarketInfo() {

        Bter bter = new Bter(MarketConst.DOGE_BTC);
//        bter.setMarket();

        try {

            System.out.println("******* LTC_USD ON BTER ********");

            System.out.println("lowest sell");
            Order lowestSell = bter.getLowestSell();
            System.out.println(lowestSell.toString());

            System.out.println("highest buy");
            Order highestBuy = bter.getHighestBuy();
            System.out.println(highestBuy.toString());

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public void testGetBalances() throws IOException {
        Bter bter = new Bter(MarketConst.DOGE_BTC);

        System.out.println("doge: " + bter.getBalanceDoge());
        System.out.println("btc: " + bter.getBalanceBtc());
    }
}
