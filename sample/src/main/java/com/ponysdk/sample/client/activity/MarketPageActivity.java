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

package com.ponysdk.sample.client.activity;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.DataListener;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragLeaveHandler;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDragStartHandler;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PDropHandler;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.core.ui.rich.PNotificationManager;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.server.TradingServiceImpl;

public class MarketPageActivity extends PageActivity {

    private static Logger log = LoggerFactory.getLogger(MarketPageActivity.class);

    protected PFlowPanel currentDrag;

    protected final PFlowPanel boxContainer = Element.newPFlowPanel();

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
        // final FindCurrenciesCommand currenciesCommand = new
        // FindCurrenciesCommand();
        //
        // for (final MarketData md : currenciesCommand.execute()) {
        // boxContainer.add(buildFXBox(md.currency));
        // }

        final PScrollPanel scrollPanel = Element.newPScrollPanel();
        scrollPanel.setWidget(boxContainer);
        scrollPanel.setSizeFull();

        view.setWidget(scrollPanel);

        final List<String> asList = Arrays.asList("EURUSD", "USDEUR", "EURAUD");
        for (int i = 0; i < 1; i++) {
            for (final String currency : asList) {
                final PFlowPanel box = Element.newPFlowPanel();
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
        final PFlowPanel background = Element.newPFlowPanel();
        background.addStyleName("background");
        box.add(background);

        final PFlowPanel headInline = Element.newPFlowPanel();
        headInline.addStyleName("head_inline");

        final PFlowPanel icon = Element.newPFlowPanel();
        icon.addStyleName("icon");
        headInline.add(icon);

        final PLabel header = Element.newPLabel(currency);
        header.addStyleName("header");
        headInline.add(header);

        final PAnchor close = Element.newPAnchor();
        close.addStyleName("close");
        headInline.add(close);

        box.add(headInline);

        final PHTML buy = Element.newPHTML("<div></div>");
        buy.addStyleName("buy");
        buy.addClickHandler(clickEvent -> {
            PNotificationManager.showHumanizedNotification(getView().asWidget().getWindowID(), "Buy clicked!");
        });
        box.add(buy);

        final PLabel buyPipHead = Element.newPLabel("buy");
        buyPipHead.addStyleName("buy_pip_head");
        box.add(buyPipHead);

        final PLabel buyNum = Element.newPLabel("1.22");
        buyNum.addStyleName("buy_num");
        box.add(buyNum);

        final PFlowPanel buyPipNum = Element.newPFlowPanel();
        buyPipNum.addStyleName("buy_pip_num");

        final PElement buyPipNumStrong = Element.newPElement("strong");
        buyPipNum.add(buyPipNumStrong);

        box.add(buyPipNum);

        final PFlowPanel buyDirection = Element.newPFlowPanel();
        buyDirection.addStyleName("buy_direction");
        box.add(buyDirection);

        final PHTML sell = Element.newPHTML("<div></div>");
        sell.addStyleName("sell");
        sell.addClickHandler(
            (clickEvent) -> PNotificationManager.showHumanizedNotification(getView().asWidget().getWindowID(), "Sell clicked!"));
        box.add(sell);

        final PLabel sellPipHead = Element.newPLabel("offer");
        sellPipHead.addStyleName("sell_pip_head");
        box.add(sellPipHead);

        final PLabel sellNum = Element.newPLabel("1.45");
        sellNum.addStyleName("sell_num");
        box.add(sellNum);

        final PFlowPanel sellPipNum = Element.newPFlowPanel();
        sellPipNum.addStyleName("sell_pip_num");

        final PElement sellPipNumStrong = Element.newPElement("strong");
        sellPipNum.add(sellPipNumStrong);

        box.add(sellPipNum);

        final PLabel amtLabel = Element.newPLabel("EUR");
        amtLabel.addStyleName("amtlabel");
        box.add(amtLabel);

        final PFlowPanel sellDirection = Element.newPFlowPanel();
        sellDirection.addStyleName("sell_direction");
        box.add(sellDirection);

        final PLabel spread = Element.newPLabel();
        spread.addStyleName("spread");
        box.add(spread);

        final PTextBox textBox = Element.newPTextBox();
        textBox.setStyleName("input");

        final PAnchor selector = Element.newPAnchor();
        selector.addStyleName("selector");

        box.addDomHandler((PDragStartHandler) event -> {
        }, PDragStartEvent.TYPE);

        box.addDomHandler((PDropHandler) event -> {
            box.removeStyleName("dragenter");
            final PWidget source = event.getDragSource();
            if (source != null && source != box) {
                final int dropIndex = boxContainer.getWidgetIndex(box);
                boxContainer.remove(source);
                boxContainer.insert(source, dropIndex);
            }
        }, PDropEvent.TYPE);

        box.addDomHandler((PDragEnterHandler) event -> {
            if (currentDrag == null || !currentDrag.equals(box)) {
                box.addStyleName("dragenter");
                if (currentDrag != null) currentDrag.removeStyleName("dragenter");
                currentDrag = box;
            }
        }, PDragEnterEvent.TYPE);

        box.addDomHandler((PDragLeaveHandler) event -> {
            if (!currentDrag.equals(box)) {
                box.removeStyleName("dragenter");
            }
        }, PDragLeaveEvent.TYPE);

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

        close.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                box.removeFromParent();
            }
        });

        return box;
    }

}
