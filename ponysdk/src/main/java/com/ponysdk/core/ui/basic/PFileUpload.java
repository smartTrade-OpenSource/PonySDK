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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.HasPChangeHandlers;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.core.ui.basic.event.PChangeHandler;
import com.ponysdk.core.ui.basic.event.PSubmitCompleteHandler;
import com.ponysdk.core.ui.eventbus.StreamHandler;

/**
 * A widget that wraps the HTML &lt;input type='file'&gt; element.
 */
public class PFileUpload extends PWidget implements HasPChangeHandlers {

    private final List<PChangeHandler> changeHandlers = new ArrayList<>();

    private final List<PSubmitCompleteHandler> submitCompleteHandlers = new ArrayList<>();

    private StreamHandler streamHandler;

    private String name;

    private String fileName;
    private int fileSize;

    private boolean enabled = true;

    private String label;

    protected PFileUpload() {
    }

    @Override
    void init0() {
        super.init0();
        saveAddHandler(HandlerModel.HANDLER_CHANGE);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FILE_UPLOAD;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (!isVisible()) return;
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_CHANGE.toStringValue())) {
            final String fileName = jsonObject.getString(ClientToServerModel.HANDLER_CHANGE.toStringValue());
            if (fileName != null) setFileName(fileName);
            final int fileSize = jsonObject.getInt(ClientToServerModel.SIZE.toStringValue());
            setFileSize(fileSize);

            final PChangeEvent event = new PChangeEvent(this);
            changeHandlers.forEach(handler -> handler.onChange(event));
        } else if (jsonObject.containsKey(ClientToServerModel.HANDLER_SUBMIT_COMPLETE.toStringValue())) {
            submitCompleteHandlers.forEach(PSubmitCompleteHandler::onSubmitComplete);
        } else {
            super.onClientData(jsonObject);
        }
    }

    public void submit() {
        UIContext.get().stackEmbeddedStreamRequest(streamHandler, this);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        if (Objects.equals(this.name, name)) return;
        this.name = name;
        saveUpdate(ServerToClientModel.NAME, name);
    }

    public void setLabel(final String label) {
        if (Objects.equals(this.label, label)) return;
        this.label = label;
        saveUpdate(ServerToClientModel.TEXT, label);
    }

    public void reset() {
        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        if (Objects.equals(this.enabled, enabled)) return;
        this.enabled = enabled;
        saveUpdate(ServerToClientModel.ENABLED, enabled);
    }

    public int getFileSize() {
        return fileSize;
    }

    private void setFileSize(final int fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        changeHandlers.add(handler);
    }

    public void addStreamHandler(final StreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    public void addSubmitCompleteHandler(final PSubmitCompleteHandler handler) {
        submitCompleteHandlers.add(handler);
    }

    @Override
    protected String dumpDOM() {
        return "<upload></upload>";
    }
}
