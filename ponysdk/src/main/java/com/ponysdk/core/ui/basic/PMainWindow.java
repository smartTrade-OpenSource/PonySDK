
package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.writer.ModelWriter;

public class PMainWindow extends PWindow {

    @Override
    void init() {
        final ModelWriter writer = Txn.getWriter();
        writer.beginObject();
        writer.write(ServerToClientModel.TYPE_CREATE, ID);
        writer.write(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
        writer.endObject();
        UIContext.get().registerObject(this);
        initialized = true;

        UIContext.get().addUIContextListener(uiContext -> onDestroy());
    }

    @Override
    public void open() {
        // Already open
    }

    @Override
    public void close() {
        // should destroy the main window ??
    }

    @Override
    public void print() {
        PScript.execute(this, "window.print()");
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.BROWSER;
    }

}
