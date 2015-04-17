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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

public class PWindow extends PObject {

    public static final long MAIN = -1L;

    private final List<Runnable> postedCommands = new ArrayList<Runnable>();
    private final ListenerCollection<PCloseHandler> closeHandlers = new ListenerCollection<PCloseHandler>();

    private final boolean loaded = false;

    public PWindow(final String url, final String name, final String features) {
        super(new EntryInstruction(PROPERTY.URL, url), new EntryInstruction(PROPERTY.NAME, name), new EntryInstruction(PROPERTY.FEATURES, features));
    }

    public void open() {
        final Update update = new Update(getID());
        update.put(PROPERTY.OPEN, true);
        Txn.get().getTxnContext().save(update);
    }

    public void close() {
        final Update update = new Update(getID());
        update.put(PROPERTY.CLOSE, true);
        Txn.get().getTxnContext().save(update);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WINDOW;
    }

    @Override
    public void onClientData(final JSONObject instruction) throws JSONException {
        if (instruction.has(HANDLER.KEY)) {
            if (HANDLER.KEY_.CLOSE_HANDLER.equals(instruction.getString(HANDLER.KEY))) {
                fireClose();
                return;
            }
        }

        super.onClientData(instruction);
    }

    public void addCloseHandler(final PCloseHandler handler) {
        closeHandlers.add(handler);
    }

    public void removeCloseHandler(final PCloseHandler handler) {
        closeHandlers.remove(handler);
    }

    private void fireClose() {
        final PCloseEvent e = new PCloseEvent(this);
        for (final PCloseHandler h : closeHandlers) {
            h.onClose(e);
        }
    }

    // private void process(final UIContext uiContext, final JSONObject jsoObject) throws JSONException {
    // if (jsoObject.has(APPLICATION.INSTRUCTIONS)) {
    // final JSONArray instructions = jsoObject.getJSONArray(APPLICATION.INSTRUCTIONS);
    // for (int i = 0; i < instructions.length(); i++) {
    // JSONObject jsonObject = null;
    // try {
    // jsonObject = instructions.getJSONObject(i);
    // uiContext.fireClientData(jsonObject);
    // } catch (final Throwable e) {
    // log.error("Failed to process instruction: " + jsonObject, e);
    // }
    // }
    // }
    // }

    protected void postOpenerCommand(final Runnable runnable) {
        postedCommands.add(runnable);
    }

    //
    // private void executePostedCommand() {
    // for (final Runnable r : postedCommands) {
    // try {
    // r.run();
    // } catch (final Throwable e) {
    // log.error("Failed to execute command: " + r, e);
    // }
    // }
    // postedCommands.clear();
    // }

    public boolean isLoaded() {
        return loaded;
    }

    public void addWidget(final IsPWidget widget) {
        PRootLayoutPanel.get().add(widget);
    }

    protected void onLoad() {}

}
