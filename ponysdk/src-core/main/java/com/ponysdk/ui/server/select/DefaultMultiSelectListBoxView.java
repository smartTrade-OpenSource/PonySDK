
package com.ponysdk.ui.server.select;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.addon.PAttachedPopupPanel;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class DefaultMultiSelectListBoxView implements PMultiSelectListBoxView {

    private final PHorizontalPanel panel = new PHorizontalPanel();

    private final PVerticalPanel listBox = new PVerticalPanel();

    private final PFlowPanel flowPanel = new PFlowPanel();

    private final Map<String, PAnchor> buttonByItem = new HashMap<String, PAnchor>();

    private final Map<String, PHTML> htmlByItem = new HashMap<String, PHTML>();

    public DefaultMultiSelectListBoxView() {
        panel.setWidth("300px");
        panel.setStyleProperty("border", "1px solid red");
        flowPanel.setWidth("300px");
        panel.add(flowPanel);

        PButton button = new PButton("+");
        panel.add(button);

        final PAttachedPopupPanel popup = new PAttachedPopupPanel(true, panel);
        popup.setStyleName(PonySDKTheme.ORACLE_POPUP_PANEL);

        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                if (popup.isShowing()) popup.hide();
                else popup.show();
            }
        });

        popup.setWidget(listBox);
    }

    @Override
    public void addItem(final String item) {
        final PHTML html = new PHTML(item);
        listBox.add(html);

        htmlByItem.put(item, html);
        buttonByItem.put(item, new PAnchor(" - " + item + " - "));
    }

    @Override
    public void unSelectItem(String item) {
        buttonByItem.get(item).removeFromParent();
        listBox.add(htmlByItem.get(item));
    }

    @Override
    public void selectItem(String item) {
        flowPanel.add(buttonByItem.get(item));
        htmlByItem.get(item).removeFromParent();
    }

    @Override
    public PWidget asWidget() {
        return panel;
    }

    @Override
    public void addShownItemClickHandler(String item, PClickHandler clickHandler) {
        htmlByItem.get(item).addClickHandler(clickHandler);
    }

    @Override
    public void addSelectedClickHandler(String item, PClickHandler clickHandler) {
        buttonByItem.get(item).addClickHandler(clickHandler);
    }
}
