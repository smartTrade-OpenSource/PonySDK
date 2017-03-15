package com.ponysdk.core.terminal.ui;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.client.Browser;

public class PTBrowser extends AbstractPTObject {
    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.WINDOW_LOCATION_REPLACE.ordinal() == modelOrdinal) {
            Browser.getWindow().getLocation().replace(binaryModel.getStringValue());
            return true;
        } else {
            return false;
        }
    }
}
