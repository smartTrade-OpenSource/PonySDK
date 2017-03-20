
package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.WebsocketEncoder;
import com.ponysdk.core.server.stm.Txn;

public class PMainWindow extends PWindow {

    @Override
    void init() {
        final WebsocketEncoder parser = Txn.get().getEncoder();
        parser.beginObject();
        parser.encode(ServerToClientModel.TYPE_CREATE, ID);
        parser.encode(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
        parser.endObject();
        UIContext.get().registerObject(this);
        initialized = true;
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
