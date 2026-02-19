package com.ponysdk.sample.client.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.json.JsonObject;

import com.ponysdk.core.ui.component.PReactComponent;

/**
 * Server-side trading grid component.
 * Controls a React AG Grid component on the client.
 */
public class PTradingGrid extends PReactComponent<TradingGridProps> {

    private final Map<Long, StockData> stocksById = new ConcurrentHashMap<>();
    private Consumer<StockData> onRowClickHandler;
    private String title = "Trading Grid";
    private boolean showStats = true;

    public PTradingGrid() {
        super(new TradingGridProps(List.of()));
    }

    @Override
    protected Class<TradingGridProps> getPropsClass() {
        return TradingGridProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "trading-grid";
    }

    /**
     * Set all stocks at once (full replace).
     */
    public void setStocks(List<StockData> stocks) {
        stocksById.clear();
        for (StockData stock : stocks) {
            stocksById.put(stock.id(), stock);
        }
        pushUpdate();
    }

    /**
     * Add or update a single stock.
     */
    public void updateStock(StockData stock) {
        stocksById.put(stock.id(), stock);
        pushUpdate();
    }

    /**
     * Update stock price by ID.
     */
    public void updatePrice(long id, double newPrice) {
        StockData existing = stocksById.get(id);
        if (existing != null) {
            stocksById.put(id, existing.withPrice(newPrice));
            pushUpdate();
        }
    }

    /**
     * Update stock count by ID.
     */
    public void updateCount(long id, int newCount) {
        StockData existing = stocksById.get(id);
        if (existing != null) {
            stocksById.put(id, existing.withCount(newCount));
            pushUpdate();
        }
    }

    /**
     * Remove a stock by ID.
     */
    public void removeStock(long id) {
        if (stocksById.remove(id) != null) {
            pushUpdate();
        }
    }

    /**
     * Set the grid title.
     */
    public void setTitle(String title) {
        this.title = title;
        pushUpdate();
    }

    /**
     * Show or hide stats footer.
     */
    public void setShowStats(boolean showStats) {
        this.showStats = showStats;
        pushUpdate();
    }

    /**
     * Register handler for row click events from client.
     */
    public void onRowClick(Consumer<StockData> handler) {
        this.onRowClickHandler = handler;
        onEvent("rowClick", payload -> {
            if (payload != null && payload.containsKey("id")) {
                long id = payload.getJsonNumber("id").longValue();
                StockData stock = stocksById.get(id);
                if (stock != null && onRowClickHandler != null) {
                    onRowClickHandler.accept(stock);
                }
            }
        });
    }

    private void pushUpdate() {
        List<StockData> stockList = new ArrayList<>(stocksById.values());
        setProps(new TradingGridProps(stockList, title, showStats));
    }
}
