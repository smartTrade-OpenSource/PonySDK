/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.sample.trading.client.activity;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.trading.server.TradingServiceImpl;
import com.ponysdk.ui.server.basic.DataListener;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDragEnterEvent;
import com.ponysdk.ui.server.basic.event.PDragLeaveEvent;
import com.ponysdk.ui.server.basic.event.PDragStartEvent;
import com.ponysdk.ui.server.basic.event.PDropEvent;

public class MarketPageActivity extends PageActivity {

    private static Logger log = LoggerFactory.getLogger(MarketPageActivity.class);

    protected PFlowPanel currentDrag;

    protected final PFlowPanel boxContainer = new PFlowPanel();

    public MarketPageActivity() {
        super("Markets", "Trading");
    }

    @Override
    protected void onInitialization() {
    }

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

        view.setWidget(scrollPanel);

        final List<String> asList = Arrays.asList("EURUSD", "USDEUR", "EURAUD");
        for (int i = 0; i < 1; i++) {
            for (final String currency : asList) {
                final PFlowPanel box = new PFlowPanel();
                box.addStyleName("widget");
                boxContainer.add(box);

                buildFXBox(box, currency);
            }
        }

        final TradingServiceImpl t = new TradingServiceImpl();
        t.start();
    }

    @Override
    protected void onShowPage(final Place place) {
    }

    @Override
    protected void onLeavingPage() {
    }

    private PWidget buildFXBox(final PFlowPanel box, final String currency) {
        final PFlowPanel background = new PFlowPanel();
        background.addStyleName("background");
        box.add(background);

        final PFlowPanel headInline = new PFlowPanel();
        headInline.addStyleName("head_inline");

        final PFlowPanel icon = new PFlowPanel();
        icon.addStyleName("icon");
        headInline.add(icon);

        final PLabel header = new PLabel(currency);
        header.addStyleName("header");
        headInline.add(header);

        final PAnchor close = new PAnchor();
        close.addStyleName("close");
        headInline.add(close);

        box.add(headInline);

        final PHTML buy = new PHTML("<div></div>");
        buy.addStyleName("buy");
        buy.addClickHandler(clickEvent -> {
            PNotificationManager.showHumanizedNotification("Buy clicked!");
        });
        box.add(buy);

        final PLabel buyPipHead = new PLabel("buy");
        buyPipHead.addStyleName("buy_pip_head");
        box.add(buyPipHead);

        final PLabel buyNum = new PLabel("1.22");
        buyNum.addStyleName("buy_num");
        box.add(buyNum);

        final PFlowPanel buyPipNum = new PFlowPanel();
        buyPipNum.addStyleName("buy_pip_num");

        final PElement buyPipNumStrong = new PElement("strong");
        buyPipNum.add(buyPipNumStrong);

        box.add(buyPipNum);

        final PFlowPanel buyDirection = new PFlowPanel();
        buyDirection.addStyleName("buy_direction");
        box.add(buyDirection);

        final PHTML sell = new PHTML("<div></div>");
        sell.addStyleName("sell");
        sell.addClickHandler((clickEvent) -> PNotificationManager.showHumanizedNotification("Sell clicked!"));
        box.add(sell);

        final PLabel sellPipHead = new PLabel("offer");
        sellPipHead.addStyleName("sell_pip_head");
        box.add(sellPipHead);

        final PLabel sellNum = new PLabel("1.45");
        sellNum.addStyleName("sell_num");
        box.add(sellNum);

        final PFlowPanel sellPipNum = new PFlowPanel();
        sellPipNum.addStyleName("sell_pip_num");

        final PElement sellPipNumStrong = new PElement("strong");
        sellPipNum.add(sellPipNumStrong);

        box.add(sellPipNum);

        final PLabel amtLabel = new PLabel("EUR");
        amtLabel.addStyleName("amtlabel");
        box.add(amtLabel);

        final PFlowPanel sellDirection = new PFlowPanel();
        sellDirection.addStyleName("sell_direction");
        box.add(sellDirection);

        final PLabel spread = new PLabel();
        spread.addStyleName("spread");
        box.add(spread);

        final PTextBox textBox = new PTextBox();
        textBox.setStyleName("input");

        final PAnchor selector = new PAnchor();
        selector.addStyleName("selector");

        box.addDomHandler(event -> {
        } , PDragStartEvent.TYPE);

        box.addDomHandler(event -> {
            box.removeStyleName("dragenter");
            final PWidget source = event.getDragSource();
            if (source != null && source != box) {
                final int dropIndex = boxContainer.getWidgetIndex(box);
                boxContainer.remove(source);
                boxContainer.insert(source, dropIndex);
            }
        } , PDropEvent.TYPE);

        box.addDomHandler(event -> {
            if (currentDrag == null || !currentDrag.equals(box)) {
                box.addStyleName("dragenter");
                if (currentDrag != null) currentDrag.removeStyleName("dragenter");
                currentDrag = box;
            }
        } , PDragEnterEvent.TYPE);

        box.addDomHandler(event -> {
            if (!currentDrag.equals(box)) {
                box.removeStyleName("dragenter");
            }
        } , PDragLeaveEvent.TYPE);

        UIContext.get().addDataListener(new DataListener() {

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
        close.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                box.removeFromParent();
            }
        });

        return box;
    }

}
