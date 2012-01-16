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

package com.ponysdk.ui.server.form.renderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.basic.event.HasPKeyPressHandlers;
import com.ponysdk.ui.server.basic.event.HasPValueChangeHandlers;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form.FormField;

public class DateBoxFormFieldRenderer implements FormFieldRenderer, PValueChangeHandler<Date>, HasPValueChangeHandlers<Date>, HasPKeyPressHandlers {

    private Date value;

    private String caption;

    private String dateFormat = "dd/MM/yyyy";

    private final List<FormFieldComponent<PDateBox>> fields = new ArrayList<FormFieldComponent<PDateBox>>();

    private final List<PKeyPressHandler> keyPressHandlers = new ArrayList<PKeyPressHandler>();

    private final List<PValueChangeHandler<Date>> valueChangeHandlers = new ArrayList<PValueChangeHandler<Date>>();

    private boolean enabled = true;

    private String debugID;

    public DateBoxFormFieldRenderer() {
        this(null);
    }

    public DateBoxFormFieldRenderer(String caption) {
        this.caption = caption;
    }

    @Override
    public void setValue(Object value) {
        this.value = (Date) value;
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().setValue(this.value);
        }
    }

    @Override
    public Date getValue() {
        return value;
    }

    @Override
    public IsPWidget render(FormField formField) {
        final PDateBox dateBox = new PDateBox();
        if (debugID != null) dateBox.ensureDebugId(debugID);
        final FormFieldComponent<PDateBox> dateFieldComponent = new FormFieldComponent<PDateBox>(dateBox);
        dateFieldComponent.setCaption(caption);
        dateFieldComponent.getInput().setFormat(dateFormat);
        dateFieldComponent.getInput().addValueChangeHandler(this);
        fields.add(dateFieldComponent);
        addListener(dateFieldComponent.getInput());
        return dateFieldComponent;
    }

    private void addListener(final PDateBox dateField) {
        for (final PValueChangeHandler<Date> handler : valueChangeHandlers) {
            dateField.addValueChangeHandler(handler);
        }
        for (final PKeyPressHandler handler : keyPressHandlers) {
            dateField.addKeyPressHandler(handler);
        }
    }

    @Override
    public void addErrorMessage(String errorMessage) {
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.addErrorMessage(errorMessage);
        }
    }

    @Override
    public void clearErrorMessage() {
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.clearErrors();
        }
    }

    @Override
    public void reset() {
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().setValue(null);
        }
        value = null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().setEnabled(enabled);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public HandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        keyPressHandlers.add(handler);
        return new HandlerRegistration() {

            @Override
            public void removeHandler() {
                keyPressHandlers.remove(handler);
            }
        };
    }

    @Override
    public List<PKeyPressHandler> getKeyPressHandlers() {
        return keyPressHandlers;
    }

    @Override
    public void addValueChangeHandler(PValueChangeHandler<Date> handler) {
        valueChangeHandlers.add(handler);
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().addValueChangeHandler(handler);
        }
    }

    @Override
    public void removeValueChangeHandler(PValueChangeHandler<Date> handler) {
        valueChangeHandlers.remove(handler);
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().removeValueChangeHandler(handler);
        }
    }

    @Override
    public Collection<PValueChangeHandler<Date>> getValueChangeHandlers() {
        return null;
    }

    @Override
    public void onValueChange(Date value) {
        this.value = value;

        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().setValue(value);
        }
    }

    @Override
    public void ensureDebugID(String debugID) {
        this.debugID = debugID;
        if (fields.isEmpty()) return;

        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().ensureDebugId(debugID);
        }
    }

    @Override
    public <H extends EventHandler> void addDomHandler(H handler, Type<H> type) {
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().addDomHandler(handler, type);
        }
    }

    public void setTimeZone(TimeZone timeZone) {
        for (final FormFieldComponent<PDateBox> p : fields) {
            p.getInput().setTimeZone(timeZone);
        }
    }
}
