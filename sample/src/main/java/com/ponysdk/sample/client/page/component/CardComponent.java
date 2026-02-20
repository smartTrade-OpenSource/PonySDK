package com.ponysdk.sample.client.page.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.component.PTemplateComponent;

/**
 * Example of a template-based component using HTML templates.
 * The template is defined once on the client side in JavaScript.
 * The server only sends JSON props updates via WebSocket.
 */
public class CardComponent extends PTemplateComponent<CardProps> {

    private static final Logger log = LoggerFactory.getLogger(CardComponent.class);

    public CardComponent() {
        this("Card Title", "Card content goes here", "#3498db", false);
    }

    public CardComponent(final String title, final String content, final String color, final boolean highlighted) {
        super(new CardProps(title, content, color, highlighted));
        
        // Register event handlers
        onEvent("cardClick", payload -> {
            log.info("Card clicked: {}", title);
            toggleHighlight();
        });
        
        onEvent("deleteClick", payload -> {
            log.info("Delete clicked for card: {}", title);
            // Handle delete action
        });
    }

    @Override
    protected Class<CardProps> getPropsClass() {
        return CardProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "card-component";
    }

    // Public API methods
    
    public void setTitle(final String title) {
        final CardProps current = getCurrentProps();
        setProps(new CardProps(title, current.content(), current.color(), current.highlighted()));
    }

    public void setContent(final String content) {
        final CardProps current = getCurrentProps();
        setProps(new CardProps(current.title(), content, current.color(), current.highlighted()));
    }

    public void setColor(final String color) {
        final CardProps current = getCurrentProps();
        setProps(new CardProps(current.title(), current.content(), color, current.highlighted()));
    }

    public void toggleHighlight() {
        final CardProps current = getCurrentProps();
        setProps(new CardProps(current.title(), current.content(), current.color(), !current.highlighted()));
    }

    public boolean isHighlighted() {
        return getCurrentProps().highlighted();
    }
}
