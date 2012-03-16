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

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.HasPSubmitCompleteHandlers;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PSubmitCompleteHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PFileUpload extends PWidget implements HasPChangeHandlers, PChangeHandler, HasPSubmitCompleteHandlers, PSubmitCompleteHandler {

    private final List<PChangeHandler> changeHandlers = new ArrayList<PChangeHandler>();

    private final List<PSubmitCompleteHandler> submitCompleteHandlers = new ArrayList<PSubmitCompleteHandler>();

    private StreamHandler streamHandler;

    private String name;

    private String fileName;

    private boolean enabled = true;

    public PFileUpload() {
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.CHANGE_HANDLER);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    public void onEventInstruction(EventInstruction instruction) {
        if (HandlerType.CHANGE_HANDLER.equals(instruction.getType())) {
            final PropertyKey key = instruction.getMainProperty().getKey();
            if (PropertyKey.FILE_NAME.equals(key)) {
                setFileName(instruction.getMainProperty().getValue());
            }
            onChange(this, 0);
        } else if (HandlerType.SUBMIT_COMPLETE_HANDLER.equals(instruction.getType())) {
            onSubmitComplete();
        } else {
            super.onEventInstruction(instruction);
        }
    }

    @Override
    public void addSubmitCompleteHandler(PSubmitCompleteHandler handler) {
        submitCompleteHandlers.add(handler);
    }

    public void setName(String name) {
        this.name = name;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.NAME, this.name);
        getPonySession().stackInstruction(update);
    }

    public void submit() {
        final StreamResource streamResource = new StreamResource();
        streamResource.embed(streamHandler, this);
    }

    public String getName() {
        return name;
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.FILE_UPLOAD;
    }

    public void addStreamHandler(StreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.ENABLED, this.enabled);
        getPonySession().stackInstruction(update);
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public void addChangeHandler(PChangeHandler handler) {
        changeHandlers.add(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return changeHandlers;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void onChange(Object source, int selectedIndex) {
        for (final PChangeHandler handler : changeHandlers) {
            handler.onChange(source, selectedIndex);
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
