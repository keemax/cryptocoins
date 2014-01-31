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
public class CryptoTradeTest {

    public void testCryptoTrade() {

        CryptoTrade cryptoTrade = new CryptoTrade(MarketConst.LTC_BTC);

        try {
            System.out.println(cryptoTrade.getBalanceBTC());
            System.out.println(cryptoTrade.getB);

            System.out.println("lowest sell");
            Order lowestSell = cryptoTrade.getLowestSell();
            System.out.println(lowestSell.toString());

            System.out.println("highest buy");
            Order highestBuy = cryptoTrade.getHighestBuy();
            System.out.println(highestBuy.toString());

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
