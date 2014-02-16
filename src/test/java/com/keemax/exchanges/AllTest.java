package com.keemax.exchanges;

import com.keemax.consts.MarketConst;
import com.keemax.model.Order;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
        Bter bter = new Bter(MarketConst.DOGE_BTC);
        List<Exchange> exchanges = new ArrayList<Exchange>();
        exchanges.add(cryptsy);
        exchanges.add(coinsE);
        exchanges.add(bter);

        System.out.println("****** balances *******");
        System.out.println("\nbtc");
        double totalBtc = 0;
        for (Exchange exchange : exchanges) {
            double btc = exchange.getBalanceBtc();
            System.out.println("\t" + exchange.getName() + ": " + btc);
            totalBtc += btc;
        }
        System.out.println("\tTOTAL: " + totalBtc);

        System.out.println("\ndoge");
        double totalDoge = 0;
        for (Exchange exchange : exchanges) {
            double doge = exchange.getBalanceDoge();
            System.out.println("\t" + exchange.getName() + ": " + doge);
            totalDoge += doge;
        }
        System.out.println("\tTOTAL: " + totalDoge + "\n");
    }

    public void xtestGetArbitrage() throws IOException {
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

    public void xtestPlaceAsyncOrders() throws ExecutionException, InterruptedException {
        Bter bter = new Bter(MarketConst.DOGE_BTC);
        CoinsE coinsE = new CoinsE(MarketConst.DOGE_BTC);
        Cryptsy cryptsy = new Cryptsy(MarketConst.DOGE_BTC);

        Order testOrder = new Order();
        testOrder.setRate(0.00000002);
        testOrder.setQuantity(500000);

        long start = System.currentTimeMillis();
        Future<String> orderNum1 = bter.placeBuyOrderAsync(testOrder);
        Future<String> orderNum2 = coinsE.placeBuyOrderAsync(testOrder);
        Future<String> orderNum3 = cryptsy.placeBuyOrderAsync(testOrder);
        System.out.println("time to place orders " + (System.currentTimeMillis() - start));

        System.out.println("bter order num: " + orderNum1.get());
        System.out.println("coins-e order num: " + orderNum2.get());
        System.out.println("cryptsy order num: " + orderNum3.get());


    }
}
