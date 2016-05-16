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

package com.ponysdk.core.stm;

import javax.json.JsonObject;

import com.ponysdk.core.Application;
import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.core.useragent.UserAgent;

public interface TxnContext {

    void flush();

    Parser getParser();

    JsonObject getJsonObject();

    UserAgent getUserAgent();

    String getRemoteAddr();

    Application getApplication();

    void setApplication(Application application);

    void setAttribute(String name, Object value);

    Object getAttribute(String name);

    void setRequest(Request request);

    void setResponse(Response response);

    int getSeqNum();

    String getHistoryToken();

    UIContext getUIContext();

    void setUIContext(UIContext uiContext);

    void sendHeartBeat();

}
