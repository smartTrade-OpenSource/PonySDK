package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;
import jakarta.json.JsonObject;

public abstract class PAddOnComposite<T extends PWidget> extends PAddOn implements IsPWidget {

    protected T widget;

    protected PAddOnComposite(final T widget, final JsonObject args) {
        super(args);
        this.widget = widget;
        this.widget.bindAddon(this);

        if (null != widget.getWindow()) attach(widget.getWindow(), widget.getFrame());
        else widget.addInitializeListener(object -> attach(widget.getWindow(), widget.getFrame()));
    }

    protected PAddOnComposite(final T widget) {
        this(widget, null);
    }

    @Override
    public boolean attach(final PWindow window, final PFrame frame) {
        this.frame = frame;

        if (this.window == null && window != null) {
            this.window = window;
            init();
            return true;
        } else if (this.window != window) {
            throw new IllegalAccessError(
                    "Widget already attached to an other window, current window : #" + this.window + ", new window : #" + window);
        }
        return false;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON_COMPOSITE;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.WIDGET_ID, widget.asWidget().getID());
    }

    @Override
    public T asWidget() {
        return widget;
    }

}
