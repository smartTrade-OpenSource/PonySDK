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

package com.ponysdk.ui.terminal;

import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.PTObject;

public interface UIService {

    public void flushEvents();

    public void stackInstrution(PTInstruction instruction);

    public void sendDataToServer(PTInstruction instruction);

    public void sendDataToServer(Element source, PTInstruction instruction);

    public void sendDataToServer(Widget source, PTInstruction instruction);

    public PTObject getPTObject(int ID);

    public PTObject getPTObject(UIObject uiObject);

    public void registerUIObject(int ID, UIObject uiObject);

    public PTObject unRegisterObject(int ID);

    public void update(JSONObject data);

    public void processInstruction(final PTInstruction instruction) throws Exception;

    public void stackError(final PTInstruction currentInstruction, final Throwable e);

    public void onCommunicationError(final Throwable exception);

    public Map<String, JavascriptAddOnFactory> getJavascriptAddOnFactory();
}
