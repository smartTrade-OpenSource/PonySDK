
package com.ponysdk.core.internalization;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ponysdk.core.server.application.UIContext;

public class PString {

    private static final String MESSAGES_CORE = "conf/MessagesCore";

    private ResourceBundle coreResourceBundle;

    private PString() {
        coreResourceBundle = ResourceBundle.getBundle(MESSAGES_CORE);
    }

    private static PString get() {
        final UIContext session = UIContext.get();
        PString s = session.getAttribute(PString.class.getName());
        if (s == null) {
            s = new PString();
            session.setAttribute(PString.class.getName(), s);
        }
        return s;
    }

    private ResourceBundle core() {
        return coreResourceBundle;
    }

    private ResourceBundle _switchLocal(final Locale locale) {
        return coreResourceBundle = ResourceBundle.getBundle(MESSAGES_CORE, locale);
    }

    public static String get(final java.lang.String key) {
        return get().core().getString(key);
    }

    public static String get(final String key, final Object... params) {
        return MessageFormat.format(get(key), params);
    }

    public static ResourceBundle switchLocal(final Locale locale) {
        return get()._switchLocal(locale);
    }
}
