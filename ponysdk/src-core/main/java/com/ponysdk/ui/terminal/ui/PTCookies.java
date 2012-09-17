
package com.ponysdk.ui.terminal.ui;

import java.util.Date;

import com.google.gwt.user.client.Cookies;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTCookies extends AbstractPTObject {

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final String name = update.getString(PROPERTY.NAME);
        if (update.containsKey(PROPERTY.ADD)) {
            final String value = update.getString(PROPERTY.VALUE);
            if (update.containsKey(PROPERTY.COOKIE_EXPIRE)) {
                final Date date = new Date(update.getLong(PROPERTY.COOKIE_EXPIRE));
                Cookies.setCookie(name, value, date);
            } else {
                Cookies.setCookie(name, value);
            }
        } else if (update.containsKey(PROPERTY.REMOVE)) {
            Cookies.removeCookie(name);
        }
    }

}
