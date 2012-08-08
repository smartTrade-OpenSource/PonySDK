
package com.ponysdk.sample.trading.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.command.PushListener;
import com.ponysdk.core.command.PushListenerMap;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.sample.trading.client.activity.MarketData;
import com.ponysdk.sample.trading.service.trading.TradingService;

public class TradingServiceImpl implements TradingService {

    private static Logger log = LoggerFactory.getLogger(TradingServiceImpl.class);

    private final PushListenerMap<String, MarketData> listenerMap = new PushListenerMap<String, MarketData>();

    private final List<MarketData> marketDatas = new ArrayList<MarketData>();

    public TradingServiceImpl() {
        final Random rdm = new Random();

        marketDatas.add(new MarketData("EurUSD", 0, 0));
        marketDatas.add(new MarketData("EurUSD1", 0, 0));
        marketDatas.add(new MarketData("EurUSD2", 0, 0));
        marketDatas.add(new MarketData("EurTKY", 0, 0));
        marketDatas.add(new MarketData("EurTKY1", 0, 0));
        marketDatas.add(new MarketData("EurTKY2", 0, 0));
        marketDatas.add(new MarketData("EurJP", 0, 0));
        marketDatas.add(new MarketData("EurJP1", 0, 0));
        marketDatas.add(new MarketData("EurJP2", 0, 0));
        marketDatas.add(new MarketData("EurCA", 0, 0));
        marketDatas.add(new MarketData("EurCA1", 0, 0));
        marketDatas.add(new MarketData("EurCA2", 0, 0));
        marketDatas.add(new MarketData("EurNY", 0, 0));
        marketDatas.add(new MarketData("EurNY1", 0, 0));
        marketDatas.add(new MarketData("EurNY2", 0, 0));

        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                final MarketData market = marketDatas.get(rdm.nextInt(marketDatas.size()));
                final MarketData price = new MarketData(market.getCurrency(), (int) (Math.random() * 99), (int) (Math.random() * 99));
                final List<PushListener<MarketData>> currencyListener = listenerMap.get(market.getCurrency());
                if (currencyListener == null) return;

                for (final PushListener<MarketData> listener : currencyListener) {
                    listener.onMessage(price);
                }
            }
        }, 1000, 10);
    }

    @Override
    public HandlerRegistration priceRegistration(final String currency, final PushListener<MarketData> listener) {
        return listenerMap.register(currency, listener);
    }

    @Override
    public List<MarketData> findCurrencies() throws Exception {
        return marketDatas;
    }

}
