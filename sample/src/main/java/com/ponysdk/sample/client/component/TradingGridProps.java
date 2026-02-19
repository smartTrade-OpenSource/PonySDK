package com.ponysdk.sample.client.component;

import java.util.List;

/**
 * Props for the TradingGrid React component.
 */
public record TradingGridProps(
    List<StockData> stocks,
    String title,
    boolean showStats
) {
    public TradingGridProps(List<StockData> stocks) {
        this(stocks, "Trading Grid", true);
    }
}
