package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/7/14
 * Time: 7:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class BtcETest {

    public void testGetFunds() throws IOException {
        BtcE btcE = new BtcE(MarketConst.LTC_BTC);
        try {
            System.out.println("trying authenticated req");
            System.out.println(btcE.getBalanceUSD());
            System.out.println("******* LTC_USD ON BTC-E ********");

            System.out.println("lowest sell");
            Order lowestSell = btcE.getLowestSell();
            System.out.println(lowestSell.toString());

            System.out.println("highest buy");
            Order highestBuy = btcE.getHighestBuy();
            System.out.println(highestBuy.toString());

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
