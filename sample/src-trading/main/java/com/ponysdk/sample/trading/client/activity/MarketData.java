
package com.ponysdk.sample.trading.client.activity;

public class MarketData {

    final String currency;
    final int buy;
    final int sell;

    public MarketData(final String currency, final int buy, final int sell) {
        this.currency = currency;
        this.buy = buy;
        this.sell = sell;
    }

    public String getCurrency() {
        return currency;
    }

    public int getBuy() {
        return buy;
    }

    public int getSell() {
        return sell;
    }

}
