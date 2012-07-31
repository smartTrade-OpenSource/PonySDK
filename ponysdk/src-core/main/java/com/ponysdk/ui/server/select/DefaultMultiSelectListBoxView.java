
package com.ponysdk.ui.server.select;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PAttachedPopupPanel;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PTextArea;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PBlurHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PFocusHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;

public class DefaultMultiSelectListBoxView extends PFlowPanel implements PMultiSelectListBoxView, PFocusHandler, PBlurHandler {

    private final PImage button = new PImage("images/disclosure_openned.png", 0, 0, 14, 14);

    // private final PFlowPanel panel = new PFlowPanel();

    private final PElement itemsSelectionPanel = new PElement("ul");

    private final PFlowPanel selectedItemsPabel = new PFlowPanel();

    private final Map<String, PHTML> buttonByItem = new HashMap<String, PHTML>();

    private final Map<String, PAnchor> htmlByItem = new HashMap<String, PAnchor>();

    private final PAttachedPopupPanel attachedPopup;

    // private final PImage cursor = new PImage("images/caret.gif");
    private final PTextArea cursor = new PTextArea();

    DefaultMultiSelectListBoxView() {
        setStyleName(PonySDKTheme.MULTISELECTLISTBOX);

        // setWidget(panel);

        // panel.setStyleName(PonySDKTheme.MULTISELECTLISTBOX_SELECTED_PANEL);
        selectedItemsPabel.setStyleName(PonySDKTheme.MULTISELECTLISTBOX_SELECTED_ITEMS_PANEL);
        itemsSelectionPanel.setStyleName(PonySDKTheme.MULTISELECTLISTBOX_ITEMS_SELECTION_PANEL);
        button.setStyleName(PonySDKTheme.MULTISELECTLISTBOX_SHOW_SELECTION_PANEL_BUTTON);
        cursor.setStyleName(PonySDKTheme.MULTISELECTLISTBOX_CARET);

        // addFocusHandler(this);
        // addBlurHandler(this);
        add(cursor);

        add(selectedItemsPabel);

        add(button);

        attachedPopup = new PAttachedPopupPanel(true, this);

        attachedPopup.setStyleName(PonySDKTheme.MULTISELECTLISTBOX_POPUP_PANEL);

        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                if (attachedPopup.isShowing()) attachedPopup.hide();
                else attachedPopup.show();
            }
        });

        attachedPopup.setWidget(itemsSelectionPanel);

    }

    @Override
    public void addItem(final String item) {
        final PElement listItem = new PElement("li");
        final PAnchor anchor = new PAnchor(item);
        listItem.add(anchor);

        final PHTML buttonItem = new PHTML("<span><span class=" + PonySDKTheme.MULTISELECTLISTBOX_VALUE_TEXT + ">" + item + "</span></span>");
        buttonItem.addStyleName(PonySDKTheme.MULTISELECTLISTBOX_VALUE_ITEM);

        itemsSelectionPanel.add(listItem);
        htmlByItem.put(item, anchor);
        buttonByItem.put(item, buttonItem);
    }

    @Override
    public void unSelectItem(final String item) {
        buttonByItem.get(item).removeFromParent();

        final PElement listItem = new PElement("li");
        listItem.add(htmlByItem.get(item));

        itemsSelectionPanel.add(listItem);
        attachedPopup.repaint();
    }

    @Override
    public void selectItem(final String item) {
        selectedItemsPabel.add(buttonByItem.get(item));
        htmlByItem.get(item).getParent().removeFromParent();

        if (!itemsSelectionPanel.iterator().hasNext()) {
            attachedPopup.hide();
        } else {
            attachedPopup.repaint();
        }
    }

    @Override
    public void addShownItemClickHandler(final String item, final PClickHandler clickHandler) {
        htmlByItem.get(item).addClickHandler(clickHandler);
    }

    @Override
    public void addSelectedClickHandler(final String item, final PClickHandler clickHandler) {
        buttonByItem.get(item).addClickHandler(clickHandler);
    }

    @Override
    public void onBlur(final PBlurEvent event) {
        // if (cursor.getParent() != null) cursor.removeFromParent();
    }

    @Override
    public void onFocus(final PFocusEvent event) {
        // System.err.println("focus");
        // if (cursor.getParent() == null) selectedItemsPabel.add(cursor);
        // cursor.setFocus(true);
    }

    @Override
    public void focusSelectedItem(final String item) {
        buttonByItem.get(item).addStyleName("toto");
    }

    @Override
    public void blurSelectedItem(final String item) {
        buttonByItem.get(item).removeStyleName("toto");
    }

    @Override
    public HandlerRegistration addKeyUpHandler(final PKeyUpHandler handler) {
        return cursor.addKeyUpHandler(handler);
    }

    @Override
    public Collection<PKeyUpHandler> getKeyUpHandlers() {
        return cursor.getKeyUpHandlers();
    }

    @Override
    public HandlerRegistration addFocusHandler(final PFocusHandler handler) {
        return cursor.addFocusHandler(handler);
    }

    @Override
    public HandlerRegistration addBlurHandler(final PBlurHandler handler) {
        return cursor.addBlurHandler(handler);
    }
}
