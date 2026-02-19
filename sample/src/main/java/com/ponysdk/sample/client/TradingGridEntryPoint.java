package com.ponysdk.sample.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.sample.client.component.PTradingGrid;
import com.ponysdk.sample.client.component.StockData;

/**
 * Demo entry point showing PTradingGrid with real-time updates.
 */
public class TradingGridEntryPoint implements EntryPoint {

    private static final String[] RACES = {
        "Arabian", "Thoroughbred", "Quarter Horse", "Appaloosa",
        "Morgan", "Mustang", "Clydesdale", "Friesian",
        "Andalusian", "Lipizzan", "Percheron", "Shire"
    };

    private final Random random = new Random();
    private PTradingGrid tradingGrid;
    private List<StockData> stocks;

    @Override
    public void start(final UIContext uiContext) {
        // Create main layout
        final PFlowPanel mainPanel = Element.newPFlowPanel();
        mainPanel.setStyleProperty("padding", "20px");
        mainPanel.setStyleProperty("background", "#1a1a2e");
        mainPanel.setStyleProperty("minHeight", "100vh");

        // Header
        final PLabel title = Element.newPLabel("🐴 PonySDK Trading Grid Demo");
        title.setStyleProperty("color", "#e94560");
        title.setStyleProperty("fontSize", "24px");
        title.setStyleProperty("marginBottom", "10px");
        mainPanel.add(title);

        final PLabel subtitle = Element.newPLabel("Real-time updates from Java server → React AG Grid");
        subtitle.setStyleProperty("color", "#888");
        subtitle.setStyleProperty("marginBottom", "20px");
        mainPanel.add(subtitle);

        // Create trading grid component
        tradingGrid = new PTradingGrid();
        tradingGrid.setTitle("Pony Stock Exchange");
        tradingGrid.attach(PWindow.getMain());

        // Initialize with sample data
        stocks = generateInitialStocks(50);
        tradingGrid.setStocks(stocks);

        // Handle row clicks
        tradingGrid.onRowClick(stock -> {
            System.out.println("Clicked on: " + stock.race() + " - $" + stock.price());
        });

        // Add grid container to main panel
        final PFlowPanel gridContainer = Element.newPFlowPanel();
        gridContainer.setStyleProperty("height", "600px");
        gridContainer.setAttribute("id", "trading-grid-container");
        mainPanel.add(gridContainer);

        PWindow.getMain().add(mainPanel);

        // Start real-time updates
        startPriceUpdates(uiContext);
    }

    private List<StockData> generateInitialStocks(int count) {
        List<StockData> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new StockData(
                i + 1,
                RACES[i % RACES.length],
                100 + random.nextDouble() * 900,
                random.nextInt(500) + 10
            ));
        }
        return result;
    }

    private void startPriceUpdates(UIContext uiContext) {
        // Schedule rapid updates every 100ms
        PScheduler.scheduleAtFixedRate(uiContext, () -> {
            // Update 3-8 random stocks per tick
            int updateCount = random.nextInt(6) + 3;
            
            for (int i = 0; i < updateCount; i++) {
                int idx = random.nextInt(stocks.size());
                StockData stock = stocks.get(idx);
                
                // Random price change (-10 to +10)
                double priceChange = (random.nextDouble() - 0.5) * 20;
                double newPrice = Math.max(1, stock.price() + priceChange);
                
                // Update stock
                StockData updated = stock.withPrice(newPrice);
                stocks.set(idx, updated);
                tradingGrid.updateStock(updated);
            }
        }, Duration.ofMillis(100));
    }
}
