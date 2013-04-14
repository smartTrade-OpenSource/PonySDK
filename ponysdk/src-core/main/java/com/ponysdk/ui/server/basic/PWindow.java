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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

public class PWindow extends PObject implements PNativeHandler {

    private static Logger log = LoggerFactory.getLogger(PWindow.class);

    private long sendSeqNum = 0;

    private JSONObject out;
    private UIContext context;
    private List<Instruction> popupstacker;
    private List<Instruction> mainStacker;

    private final List<Runnable> postedCommands = new ArrayList<Runnable>();

    public PWindow(final String url, final String name, final String features) {
        super();
        if (url != null && !url.isEmpty()) create.put(PROPERTY.URL, url);
        if (name != null && !name.isEmpty()) create.put(PROPERTY.NAME, name);
        if (features != null && !features.isEmpty()) create.put(PROPERTY.FEATURES, features);

        addNativeHandler(this);
    }

    public void open() {
        final Update update = new Update(getID());
        update.put(PROPERTY.OPEN, true);
        getUIContext().stackInstruction(update);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WINDOW;
    }

    @Override
    public void onNativeEvent(final PNativeEvent event) {
        acquire();
        try {
            final String in = event.getJsonObject().getString(PROPERTY.DATA);
            final JSONObject inputData = new JSONObject(new JSONTokener(in));
            if (inputData.has(APPLICATION.KEY)) {
                sendSeqNum = 0;
                onLoad();
                out.put(APPLICATION.VIEW_ID, 0);
            } else {
                process(context, inputData);
            }
            flush();
        } catch (final Throwable e) {
            log.error("", e);
        } finally {
            release();
        }

        if (!postedCommands.isEmpty()) executePostedCommand();
    }

    public void acquire() {
        out = new JSONObject();
        context = UIContext.get();
        popupstacker = new ArrayList<Instruction>();
        mainStacker = context.setCurrentStacker(popupstacker);
    }

    public void flush() {
        try {
            out.put(APPLICATION.INSTRUCTIONS, popupstacker);
            out.put(APPLICATION.SEQ_NUM, sendSeqNum);
            sendSeqNum++;
        } catch (final JSONException e) {
            log.error("Cannot flush window", e);
        }
    }

    public void release() {
        context.setCurrentStacker(mainStacker);
        final Update update = new Update(ID);
        update.put(PROPERTY.TEXT, out.toString());
        UIContext.get().stackInstruction(update);
    }

    private void process(final UIContext uiContext, final JSONObject jsoObject) throws JSONException {
        if (jsoObject.has(APPLICATION.INSTRUCTIONS)) {
            final JSONArray instructions = jsoObject.getJSONArray(APPLICATION.INSTRUCTIONS);
            for (int i = 0; i < instructions.length(); i++) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = instructions.getJSONObject(i);
                    uiContext.fireClientData(jsonObject);
                } catch (final Throwable e) {
                    log.error("Failed to process instruction: " + jsonObject, e);
                }
            }
        }
    }

    protected void postOpenerCommand(final Runnable runnable) {
        postedCommands.add(runnable);
    }

    private void executePostedCommand() {
        for (final Runnable r : postedCommands) {
            try {
                r.run();
            } catch (final Throwable e) {
                log.error("Failed to execute command: " + r, e);
            }
        }
        postedCommands.clear();
    }

    protected void onLoad() {}

}
