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

package com.ponysdk.core.terminal.ui;

import java.util.Collection;
import java.util.Date;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Cookies;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTCookies extends AbstractPTObject {

    public PTCookies(final UIBuilder uiBuilder) {
        final Collection<String> cookieNames = Cookies.getCookieNames();
        final JSONArray cookies = new JSONArray();
        if (cookieNames != null) {
            int i = 0;
            for (final String cookie : cookieNames) {
                final JSONObject jsoObject = new JSONObject();
                jsoObject.put(ClientToServerModel.COOKIE_NAME.toStringValue(), new JSONString(cookie));
                jsoObject.put(ClientToServerModel.COOKIE_VALUE.toStringValue(), new JSONString(Cookies.getCookie(cookie)));
                cookies.set(i++, jsoObject);
            }
        }

        final PTInstruction eventInstruction = new PTInstruction(getObjectID());
        eventInstruction.put(ClientToServerModel.COOKIES, cookies);
        uiBuilder.sendDataToServer(eventInstruction);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.ADD_COOKIE.ordinal() == modelOrdinal) {
            final String name = binaryModel.getStringValue();
            // ServerToClientModel.VALUE
            final String value = buffer.readBinaryModel().getStringValue();

            final BinaryModel expire = buffer.readBinaryModel();
            if (ServerToClientModel.COOKIE_EXPIRE.equals(expire.getModel())) {
                final Date date = new Date(expire.getLongValue());
                Cookies.setCookie(name, value, date);
            } else {
                buffer.rewind(expire);
                Cookies.setCookie(name, value);
            }
            return true;
        } else if (ServerToClientModel.REMOVE_COOKIE.ordinal() == modelOrdinal) {
            Cookies.removeCookie(binaryModel.getStringValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
