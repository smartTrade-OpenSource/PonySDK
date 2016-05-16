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

import javax.json.JsonObject;

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.HasPSubmitCompleteHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PSubmitCompleteHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * A widget that wraps the HTML &lt;input type='file'&gt; element.
 */
public class PFileUpload extends PWidget
        implements HasPChangeHandlers, PChangeHandler, HasPSubmitCompleteHandlers, PSubmitCompleteHandler {

    private final List<PChangeHandler> changeHandlers = new ArrayList<>();

    private final List<PSubmitCompleteHandler> submitCompleteHandlers = new ArrayList<>();

    private StreamHandler streamHandler;

    private String name;

    private String fileName;

    private boolean enabled = true;

    public PFileUpload() {
        super();
    }

    @Override
    protected void init() {
        super.init();
        saveAddHandler(HandlerModel.HANDLER_CHANGE_HANDLER);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FILE_UPLOAD;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_CHANGE_HANDLER.toStringValue())) {
            final String fileName = jsonObject.getString(ClientToServerModel.FILE_NAME.toStringValue());
            if (fileName != null) {
                setFileName(fileName);
            }
            onChange(new PChangeEvent(this));
        } else if (jsonObject.containsKey(ClientToServerModel.HANDLER_SUBMIT_COMPLETE_HANDLER.toStringValue())) {
            onSubmitComplete();
        } else {
            super.onClientData(jsonObject);
        }
    }

    @Override
    public void addSubmitCompleteHandler(final PSubmitCompleteHandler handler) {
        submitCompleteHandlers.add(handler);
    }

    public void setName(final String name) {
        this.name = name;
        saveUpdate(ServerToClientModel.NAME, name);
    }

    public void submit() {
        final StreamResource streamResource = new StreamResource();
        streamResource.embed(streamHandler, this);
    }

    public String getName() {
        return name;
    }

    public void addStreamHandler(final StreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        saveUpdate(ServerToClientModel.ENABLED, enabled);
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
