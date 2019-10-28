package com.ponysdk.test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.writer.ModelWriter;

public class ModelWriterForTest extends ModelWriter {
    private PWindow window;
    private int lastObjectID;

    public ModelWriterForTest() {
        super(null);
    }

    @Override
    public void beginObject(PWindow window) {
        this.window = window;
    }

    @Override
    public void endObject() {
    }

    @Override
    public PWindow getCurrentWindow() {
        return window;
    }

    @Override
    public void write(ServerToClientModel model) {
        write(model, null);
    }

    @Override
    public void write(ServerToClientModel model, Object value) {
        if (model == ServerToClientModel.TYPE_CREATE || model == ServerToClientModel.TYPE_UPDATE) {
            lastObjectID = (int) value;
        } else if (model == ServerToClientModel.OPEN) {
            PObject object = UIContext.get().getObject(lastObjectID);
            if (object instanceof PWindow) {
                PEmulator.windowOpened(object);
            }
        } else if (model == ServerToClientModel.CLOSE) {
            PObject object = UIContext.get().getObject(lastObjectID);
            if (object instanceof PWindow) {
                PEmulator.windowClosed(object);
            }
        } else if (model == ServerToClientModel.HISTORY_FIRE_EVENTS) {
            if (Boolean.TRUE.equals(value)) {
                UIContext.get().getHistory().fireHistoryChanged(UIContext.get().getHistory().getToken());
            }
        }
    }
}
