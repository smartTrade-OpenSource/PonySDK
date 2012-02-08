/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.server.basic;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Remove;

public class PCookies {

    // private static final Logger log = LoggerFactory.getLogger(PCookies.class);

    // public static SecretKey key;
    // static {
    // try {
    // final DESKeySpec KEY = new DESKeySpec("PONY_COOKIE".getBytes("UTF8"));
    // final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    // key = keyFactory.generateSecret(KEY);
    // } catch (final Exception e) {
    // throw new RuntimeException("Error occured when instantiating key", e);
    // }
    // }

    private final Map<String, String> cachedCookies = new ConcurrentHashMap<String, String>();

    public PCookies(Map<String, String> cookies) throws Exception {
        for (final Entry<String, String> entry : cookies.entrySet()) {
            // final String name = decode(entry.getKey());
            // final String value = decode(entry.getValue());

            cachedCookies.put(entry.getKey(), entry.getValue());
        }
    }

    public String getCookie(final String name) {
        return cachedCookies.get(name);
    }

    public String removeCookie(String name) {
        final Remove remove = new Remove();
        final Property property = new Property(PropertyKey.COOKIE, name);
        remove.setMainProperty(property);
        PonySession.getCurrent().stackInstruction(remove);
        return cachedCookies.remove(name);
    }

    public void setCookie(String name, String value) {
        setCookie(name, value, null);
    }

    public void setCookie(String name, String value, Date expires) {
        try {
            // final byte[] clearName = name.getBytes("UTF8");
            // final byte[] clearValue = value.getBytes("UTF8");
            // final Cipher cipher = Cipher.getInstance("DES");
            // cipher.init(Cipher.ENCRYPT_MODE, key);
            cachedCookies.put(name, value);

            // final String encryptedName = new String(cipher.doFinal(clearName));
            // final String encryptedValue = new String(cipher.doFinal(clearValue));

            final long ID = PonySession.getCurrent().nextID();
            final Create create = new Create(ID, WidgetType.COOKIE);
            final Property property = new Property();
            property.setProperty(PropertyKey.NAME, name);
            property.setProperty(PropertyKey.VALUE, value);
            if (expires != null) {
                property.setProperty(PropertyKey.COOKIE_EXPIRE, expires.getTime());
            }
            create.setMainProperty(property);

            PonySession.getCurrent().stackInstruction(create);
        } catch (final Exception e) {
            throw new RuntimeException("encoding failure", e);
        }
    }

    // private String decode(String source) {
    // String value = null;
    // try {
    // // final Cipher cipher = Cipher.getInstance("DES");// cipher is not thread safe
    // // cipher.init(Cipher.DECRYPT_MODE, key);
    // value = new String(cipher.doFinal(source.getBytes()));
    // } catch (final Exception e) {
    // log.warn("Cannot decode cookie # " + source);
    // }
    // return value;
    // }
}
