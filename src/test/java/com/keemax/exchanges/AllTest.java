package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/22/14
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class AllTest {

    public void testPrintBalances() throws IOException {
        Cryptsy cryptsy = new Cryptsy(MarketConst.DOGE_BTC);
        CoinsE coinsE = new CoinsE(MarketConst.DOGE_BTC);


//        while(true) {
        double cryptsyBtc = cryptsy.getBalanceBtc();
        double cryptsyDoge = cryptsy.getBalanceDoge();
        double coinseBtc = coinsE.getBalanceBtc();
        double coinseDoge = coinsE.getBalanceDoge();
        System.out.println("\n\n\n\n");
        System.out.println("*************************");
        System.out.println("*   STARTING BALANCES   *");
        System.out.println("*************************");
        System.out.println("\n** BTC **");
        System.out.println("cryptsy: " + cryptsyBtc);
        System.out.println("coins-e: " + coinseBtc);
        System.out.println("  TOTAL: " + (cryptsyBtc + coinseBtc));
        System.out.println("\n** DOGE **");
        System.out.println("cryptsy: " + cryptsyDoge);
        System.out.println("coins-e: " + coinseDoge);
        System.out.println("  TOTAL: " + (cryptsyDoge + coinseDoge));
        System.out.println("\n*************************\n\n\n");
    }

    public void testGetArbitrage() throws IOException {
        List<Exchange> exchanges = new ArrayList<Exchange>();
        List<Order> sells = new ArrayList<Order>();
        List<Order> buys = new ArrayList<Order>();

        exchanges.add(new BtcE(MarketConst.LTC_BTC));
        exchanges.add(new Bter(MarketConst.LTC_BTC));
        exchanges.add(new CoinsE(MarketConst.LTC_BTC));
        exchanges.add(new CryptoTrade(MarketConst.LTC_BTC));
        exchanges.add(new Cryptsy(MarketConst.LTC_BTC));

        for (Exchange exchange : exchanges) {
            System.out.println(exchange.getName());
            sells.add(exchange.getLowestSell());
            buys.add(exchange.getHighestBuy());
        }

        for (int i = 0; i < exchanges.size(); i++) {
            for (int j = 0; j < exchanges.size(); j++) {
                System.out.println("buy on " + exchanges.get(i).getName() + " sell on " + exchanges.get(j).getName() +
                                   " " + (buys.get(i).getRate() - sells.get(j).getRate()));
            }
        }
//        for (Order buy : buys) {
//            for (Order sell : sells) {
//                System.out.println(sell.getRate() - buy.getRate());
//            }
//        }
    }
}
