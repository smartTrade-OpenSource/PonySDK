
package com.ponysdk.sample.trading.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.SessionManager;
import com.ponysdk.sample.trading.client.activity.MarketData;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.basic.PPusher.PusherState;

public class TradingServiceImpl /** implements TradingService **/
{

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

                for (final Application application : SessionManager.get().getApplications()) {
                    final Collection<UIContext> uiContexts = application.getUIContexts();
                    for (final UIContext uiContext : uiContexts) {
                        final PPusher pusher = uiContext.getPusher();
                        if (pusher != null) {
                            if (pusher.getPusherState() == PusherState.STARTED) pusher.pushToClient(price);
                        }
                    }
                }
            }
        }, 1000, 200);
    }
    //
    // @Override
    // public List<MarketData> findCurrencies() throws Exception {
    // return marketDatas;
    // }

}
