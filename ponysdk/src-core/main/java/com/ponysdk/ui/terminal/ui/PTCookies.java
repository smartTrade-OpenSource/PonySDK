
package com.ponysdk.ui.terminal.ui;

import java.util.Date;

import com.google.gwt.user.client.Cookies;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTCookies extends AbstractPTObject {

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.ADD.equals(binaryModel.getModel())) {
            final String name = binaryModel.getStringValue();
            // Model.VALUE
            final String value = buffer.getBinaryModel().getStringValue();

            // Model.VALUE
            final BinaryModel expire = buffer.getBinaryModel();
            if (Model.COOKIE_EXPIRE.equals(expire)) {
                final Date date = new Date(expire.getLongValue());
                Cookies.setCookie(name, value, date);
            } else {
                Cookies.setCookie(name, value);
                buffer.rewind(expire);
            }
            return true;
        }

        if (Model.REMOVE.equals(binaryModel.getModel())) {
            Cookies.removeCookie(binaryModel.getStringValue());
            return true;
        }

        return super.update(buffer, binaryModel);
    }

}
