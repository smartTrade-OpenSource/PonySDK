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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.event.PStreamHandler;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.HasPSubmitCompleteHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PSubmitCompleteHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

/**
 * A widget that wraps the HTML &lt;input type='file'&gt; element.
 */
public class PFileUpload extends PWidget implements HasPChangeHandlers, PChangeHandler, HasPSubmitCompleteHandlers, PSubmitCompleteHandler {

    private final List<PChangeHandler> changeHandlers = new ArrayList<PChangeHandler>();

    private final List<PSubmitCompleteHandler> submitCompleteHandlers = new ArrayList<PSubmitCompleteHandler>();

    private PStreamHandler streamHandler;

    private String name;

    private String fileName;

    private boolean enabled = true;

    public PFileUpload() {
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.CHANGE_HANDLER);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FILE_UPLOAD;
    }

    @Override
    public void onEventInstruction(final JSONObject instruction) throws JSONException {
        final String handler = instruction.getString(HANDLER.KEY);

        if (HANDLER.CHANGE_HANDLER.equals(handler)) {
            final String fileName = instruction.getString(PROPERTY.FILE_NAME);
            if (fileName != null) {
                setFileName(fileName);
            }
            onChange(new PChangeEvent(this));
        } else if (HANDLER.SUBMIT_COMPLETE_HANDLER.equals(handler)) {
            onSubmitComplete();
        } else {
            super.onEventInstruction(instruction);
        }
    }

    @Override
    public void addSubmitCompleteHandler(final PSubmitCompleteHandler handler) {
        submitCompleteHandlers.add(handler);
    }

    public void setName(final String name) {
        this.name = name;
        final Update update = new Update(getID());
        update.put(PROPERTY.NAME, this.name);
        getPonySession().stackInstruction(update);
    }

    public void submit() {
        final StreamResource streamResource = new StreamResource();
        streamResource.embed(streamHandler, this);
    }

    public String getName() {
        return name;
    }

    public void addStreamHandler(final PStreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        final Update update = new Update(getID());
        update.put(PROPERTY.ENABLED, this.enabled);
        getPonySession().stackInstruction(update);
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        changeHandlers.add(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return changeHandlers;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void onChange(final PChangeEvent event) {
        for (final PChangeHandler handler : changeHandlers) {
            handler.onChange(event);
        }
    }

    @Override
    public void onSubmitComplete() {
        for (final PSubmitCompleteHandler handler : submitCompleteHandlers) {
            handler.onSubmitComplete();
        }
    }

    @Override
    public Collection<PSubmitCompleteHandler> getSubmitCompleteHandlers() {
        return submitCompleteHandlers;
    }

}
