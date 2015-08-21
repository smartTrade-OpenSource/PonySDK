
package com.ponysdk.ui.terminal.ui;

import java.util.Date;

import com.google.gwt.user.client.Cookies;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTCookies extends AbstractPTObject {

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final String name = update.getString(Model.NAME);
        if (update.containsKey(Model.ADD)) {
            final String value = update.getString(Model.VALUE);
            if (update.containsKey(Model.COOKIE_EXPIRE)) {
                final Date date = new Date(update.getLong(Model.COOKIE_EXPIRE));
                Cookies.setCookie(name, value, date);
            } else {
                Cookies.setCookie(name, value);
            }
        } else if (update.containsKey(Model.REMOVE)) {
            Cookies.removeCookie(name);
        }
    }

}
