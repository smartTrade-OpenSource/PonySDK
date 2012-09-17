
package com.ponysdk.core.internalization;

import java.util.Locale;
import java.util.ResourceBundle;

import com.ponysdk.core.UIContext;

public class PString {

    private ResourceBundle coreResourceBundle;

    private PString() {
        coreResourceBundle = ResourceBundle.getBundle("Core");
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
        return coreResourceBundle = ResourceBundle.getBundle("Core", locale);
    }

    public static java.lang.String get(final java.lang.String key) {
        return get().core().getString(key);
    }

    public static ResourceBundle switchLocal(final Locale locale) {
        return get()._switchLocal(locale);
    }
}
