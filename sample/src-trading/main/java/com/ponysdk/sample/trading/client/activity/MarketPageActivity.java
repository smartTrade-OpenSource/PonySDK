
package com.ponysdk.sample.trading.client.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.basic.DataListener;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDragEnterEvent;
import com.ponysdk.ui.server.basic.event.PDragEnterHandler;
import com.ponysdk.ui.server.basic.event.PDragLeaveEvent;
import com.ponysdk.ui.server.basic.event.PDragLeaveHandler;
import com.ponysdk.ui.server.basic.event.PDragStartEvent;
import com.ponysdk.ui.server.basic.event.PDragStartHandler;
import com.ponysdk.ui.server.basic.event.PDropEvent;
import com.ponysdk.ui.server.basic.event.PDropHandler;

public class MarketPageActivity extends PageActivity {

    private static Logger log = LoggerFactory.getLogger(MarketPageActivity.class);

    protected PFlowPanel currentDrag;

    protected final PFlowPanel boxContainer = new PFlowPanel();

    public MarketPageActivity() {
        super("Markets", "Trading");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onFirstShowPage() {
        log.info("Showing market page");
        //
        // final FindCurrenciesCommand currenciesCommand = new FindCurrenciesCommand();
        //
        // for (final MarketData md : currenciesCommand.execute()) {
        // boxContainer.add(buildFXBox(md.currency));
        // }

        final PScrollPanel scrollPanel = new PScrollPanel();
        scrollPanel.setWidget(boxContainer);
        scrollPanel.setSizeFull();

        pageView.setWidget(scrollPanel);
    }

    @Override
    protected void onShowPage(final Place place) {}

    @Override
    protected void onLeavingPage() {}

    private PWidget buildFXBox(final String currency) {

        final PFlowPanel box = new PFlowPanel();
        box.addStyleName("widget");
        final PFlowPanel background = new PFlowPanel();
        background.addStyleName("background");
        final PFlowPanel headInline = new PFlowPanel();
        headInline.addStyleName("head_inline");
        final PFlowPanel icon = new PFlowPanel();
        icon.addStyleName("icon");
        final PLabel header = new PLabel(currency);
        header.addStyleName("header");
        final PAnchor close = new PAnchor();
        close.addStyleName("close");
        final PHTML buy = new PHTML("<div></div>");
        buy.addStyleName("buy");
        buy.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                PNotificationManager.showHumanizedNotification("Buy clicked!");
            }
        });

        final PLabel buyPipHead = new PLabel("buy");
        buyPipHead.addStyleName("buy_pip_head");
        final PLabel buyNum = new PLabel("1.22");
        buyNum.addStyleName("buy_num");
        final PFlowPanel buyPipNum = new PFlowPanel();
        buyPipNum.addStyleName("buy_pip_num");
        final PElement buyPipNumStrong = new PElement("strong");
        final PFlowPanel buyDirection = new PFlowPanel();
        buyDirection.addStyleName("buy_direction");
        final PHTML sell = new PHTML("<div></div>");
        sell.addStyleName("sell");
        sell.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                PNotificationManager.showHumanizedNotification("Sell clicked!");
            }
        });
        final PLabel sellPipHead = new PLabel("offer");
        sellPipHead.addStyleName("sell_pip_head");
        final PLabel sellNum = new PLabel("1.45");
        sellNum.addStyleName("sell_num");
        final PElement sellPipNumStrong = new PElement("strong");
        final PFlowPanel sellPipNum = new PFlowPanel();
        sellPipNum.addStyleName("sell_pip_num");
        final PLabel amtLabel = new PLabel("EUR");
        amtLabel.addStyleName("amtlabel");
        final PFlowPanel sellDirection = new PFlowPanel();
        sellDirection.addStyleName("sell_direction");
        final PLabel spread = new PLabel();
        spread.addStyleName("spread");
        final PTextBox textBox = new PTextBox();
        textBox.setStyleName("input");
        final PAnchor selector = new PAnchor();
        selector.addStyleName("selector");

        box.add(background);
        box.add(headInline);
        headInline.add(icon);
        headInline.add(header);
        headInline.add(close);
        box.add(buy);
        box.add(buyPipHead);
        box.add(buyNum);
        box.add(buyPipNum);
        buyPipNum.add(buyPipNumStrong);
        box.add(buyDirection);
        box.add(sell);
        box.add(sellPipHead);
        box.add(sellNum);
        box.add(sellPipNum);
        sellPipNum.add(sellPipNumStrong);
        box.add(amtLabel);
        box.add(sellDirection);
        box.add(spread);

        box.addDomHandler(new PDragStartHandler() {

            @Override
            public void onDragStart(final PDragStartEvent event) {}
        }, PDragStartEvent.TYPE);

        box.addDomHandler(new PDropHandler() {

            @Override
            public void onDrop(final PDropEvent event) {
                box.removeStyleName("dragenter");
                final PWidget source = event.getDragSource();
                if (source != null && source != box) {
                    final int dropIndex = boxContainer.getWidgetIndex(box);
                    boxContainer.remove(source);
                    boxContainer.insert(source, dropIndex);
                }
            }
        }, PDropEvent.TYPE);

        box.addDomHandler(new PDragEnterHandler() {

            @Override
            public void onDragEnter(final PDragEnterEvent event) {
                if (currentDrag == null || !currentDrag.equals(box)) {
                    box.addStyleName("dragenter");
                    if (currentDrag != null) currentDrag.removeStyleName("dragenter");
                    currentDrag = box;
                }
            }
        }, PDragEnterEvent.TYPE);

        box.addDomHandler(new PDragLeaveHandler() {

            @Override
            public void onDragLeave(final PDragLeaveEvent event) {
                if (!currentDrag.equals(box)) {
                    box.removeStyleName("dragenter");
                }
            }
        }, PDragLeaveEvent.TYPE);

        PPusher.get().addDataListener(new DataListener() {

            private int lastBuy;
            private int lastSell;

            @Override
            public void onData(final Object data) {
                if (data instanceof MarketData) {
                    final MarketData msg = (MarketData) data;
                    final int spreadValue = Math.abs(msg.sell - msg.buy);

                    if (lastBuy < msg.buy) {
                        buyDirection.removeStyleName("down");
                        buyDirection.addStyleName("up");
                    } else {
                        buyDirection.removeStyleName("up");
                        buyDirection.addStyleName("down");
                    }
                    if (lastSell < msg.sell) {
                        sellDirection.removeStyleName("down");
                        sellDirection.addStyleName("up");
                    } else {
                        sellDirection.removeStyleName("up");
                        sellDirection.addStyleName("down");
                    }

                    lastBuy = msg.buy;
                    lastSell = msg.sell;
                    buyPipNumStrong.setInnerText(lastBuy + "");
                    sellPipNumStrong.setInnerText(lastSell + "");
                    spread.setText(spreadValue + "");
                }
            }
        });

        // PPusher.get().addConnectionListener(new ConnectionListener() {
        //
        // @Override
        // public void onOpen() {
        // priceCommand.execute();
        // }
        //
        // @Override
        // public void onClose() {
        // // if (registration != null) registration.removeHandler();
        // }
        // });
        //
        // // HandlerRegistration registration = null;
        // if (PPusher.get().getPusherState() == PusherState.STARTED) priceCommand.execute();
        // //
        // // close.addClickHandler(new PClickHandler() {
        // //
        // // @Override
        // // public void onClick(final PClickEvent event) {
        // // box.removeFromParent();
        // // registration.removeHandler();
        // // }
        // // });

        return box;
    }

}
