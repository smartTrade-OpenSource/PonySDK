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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.event.*;
import com.ponysdk.core.ui.eventbus.StreamHandler;
import com.ponysdk.core.model.WidgetType;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    protected void init0() {
        super.init0();
        saveAddHandler(HandlerModel.HANDLER_CHANGE);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FILE_UPLOAD;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_CHANGE.toStringValue())) {
            final String fileName = jsonObject.getString(ClientToServerModel.HANDLER_CHANGE.toStringValue());
            if (fileName != null) {
                setFileName(fileName);
            }
            onChange(new PChangeEvent(this));
        } else if (jsonObject.containsKey(ClientToServerModel.HANDLER_SUBMIT_COMPLETE.toStringValue())) {
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
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.NAME, name);
        });
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
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.ENABLED, enabled);
        });
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
        submitCompleteHandlers.forEach(PSubmitCompleteHandler::onSubmitComplete);
    }

    @Override
    public Collection<PSubmitCompleteHandler> getSubmitCompleteHandlers() {
        return submitCompleteHandlers;
    }

}
