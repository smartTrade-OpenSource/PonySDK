package com.ponysdk.core.ui.component;

import com.ponysdk.core.model.ServerToClientModel;

/**
 * A minimal PComponent that wraps text content for use in Web Component slots.
 * <p>
 * This component exists solely to bridge the gap between text content and the
 * slot system, which requires PComponent instances. It renders as a text node
 * in the browser without any wrapper element.
 * </p>
 */
public class PTextComponent extends PComponent<PTextComponent.EmptyProps> {

    /**
     * Empty props record - text content is managed separately via TEXT protocol.
     */
    public record EmptyProps() {}

    private String text;

    /**
     * Creates a new PTextComponent with the given text content.
     *
     * @param text the initial text content
     */
    public PTextComponent(final String text) {
        super(new EmptyProps(), FrameworkType.WEB_COMPONENT);
        this.text = text != null ? text : "";
        sendTextUpdate();
    }

    /**
     * Updates the text content and notifies the client.
     *
     * @param newText the new text content
     */
    public void setText(final String newText) {
        this.text = newText != null ? newText : "";
        sendTextUpdate();
    }

    /**
     * Gets the current text content.
     *
     * @return the current text
     */
    public String getText() {
        return text;
    }

    @Override
    protected Class<EmptyProps> getPropsClass() {
        return EmptyProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "PTextComponent";
    }

    /**
     * Sends the text content to the client using the TEXT protocol.
     */
    private void sendTextUpdate() {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.TYPE_UPDATE);
            writer.write(ServerToClientModel.TEXT, text);
        });
    }
}
