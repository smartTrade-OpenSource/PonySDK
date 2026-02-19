package com.ponysdk.sample.client.component;

/**
 * Stock data record for the trading grid.
 * This is the props type sent to the React component.
 */
public record StockData(
    long id,
    String race,
    double price,
    int count,
    double change
) {
    public StockData(long id, String race, double price, int count) {
        this(id, race, price, count, 0.0);
    }
    
    public StockData withPrice(double newPrice) {
        return new StockData(id, race, newPrice, count, newPrice - price);
    }
    
    public StockData withCount(int newCount) {
        return new StockData(id, race, price, newCount, change);
    }
}
