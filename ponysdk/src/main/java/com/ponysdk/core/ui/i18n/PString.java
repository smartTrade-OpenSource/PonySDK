/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.i18n;

import com.ponysdk.core.server.application.UIContext;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

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

    public static String get(final java.lang.String key) {
        return get().core().getString(key);
    }

    public static String get(final String key, final Object... params) {
        return MessageFormat.format(get(key), params);
    }

    public static ResourceBundle switchLocal(final Locale locale) {
        return get()._switchLocal(locale);
    }

    private ResourceBundle core() {
        return coreResourceBundle;
    }

    private ResourceBundle _switchLocal(final Locale locale) {
        return coreResourceBundle = ResourceBundle.getBundle(MESSAGES_CORE, locale);
    }
}
