package com.ponysdk.core.terminal.ui.elemental;

import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.PTObject;

import elemental.client.Browser;

public class DivElemental extends AbstractElemental {

    @Override
    public void create(ReaderBuffer buffer, int objectId, UIBuilder uiService) {
        element = Browser.getDocument().createDivElement();
        ID = objectId;
    }

    @Override
    public boolean update(ReaderBuffer buffer, BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.TEXT == model) {
            element.setTextContent(binaryModel.getStringValue());
            return true;
        }
        return false;
    }

    @Override
    public void add(ReaderBuffer buffer, PTObject object) {
        element.appendChild(object.asNode());
    }

    @Override
    public void remove(ReaderBuffer buffer, PTObject object) {
        element.removeChild(object.asNode());
    }

    @Override
    public void addHandler(ReaderBuffer buffer, HandlerModel handlerModel) {

    }

    @Override
    public void removeHandler(ReaderBuffer buffer, HandlerModel handlerModel) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public int getObjectID() {
        return ID;
    }
}
