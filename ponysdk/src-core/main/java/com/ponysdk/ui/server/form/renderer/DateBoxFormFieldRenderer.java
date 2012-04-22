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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.ponysdk.core.event.PEventHandler;
import com.ponysdk.core.event.PHandlerRegistration;
import com.ponysdk.ui.server.basic.HasPValueChangeHandlers;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.basic.event.HasPKeyPressHandlers;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form.FormField;

public class DateBoxFormFieldRenderer implements FormFieldRenderer, PValueChangeHandler<Date>, HasPValueChangeHandlers<Date>, HasPKeyPressHandlers {

    private Date value;

    private String caption;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private final List<FormFieldComponent<PDateBox>> fields = new ArrayList<FormFieldComponent<PDateBox>>();

    private final List<PKeyPressHandler> keyPressHandlers = new ArrayList<PKeyPressHandler>();

    private final List<PValueChangeHandler<Date>> valueChangeHandlers = new ArrayList<PValueChangeHandler<Date>>();

    private boolean enabled = true;

    private String debugID;

    public DateBoxFormFieldRenderer() {
        this(null);
    }

    public DateBoxFormFieldRenderer(final String caption) {
        this.caption = caption;
    }

    @Override
    public void setValue(final Object value) {
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
    public IsPWidget render(final FormField formField) {
        final PDateBox dateBox = new PDateBox();
        if (debugID != null) dateBox.ensureDebugId(debugID);
        final FormFieldComponent<PDateBox> dateFieldComponent = new FormFieldComponent<PDateBox>(dateBox);
        dateFieldComponent.setCaption(caption);
        dateFieldComponent.getInput().setDateFormat(dateFormat);
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
    public void addErrorMessage(final String errorMessage) {
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
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().setEnabled(enabled);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setCaption(final String caption) {
        this.caption = caption;
    }

    public void setDateFormat(final SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public PHandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        keyPressHandlers.add(handler);
        return new PHandlerRegistration() {

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
    public void addValueChangeHandler(final PValueChangeHandler<Date> handler) {
        valueChangeHandlers.add(handler);
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().addValueChangeHandler(handler);
        }
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<Date> handler) {
        valueChangeHandlers.remove(handler);
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().removeValueChangeHandler(handler);
        }
    }

    @Override
    public Collection<PValueChangeHandler<Date>> getValueChangeHandlers() {
        return valueChangeHandlers;
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Date> event) {
        this.value = event.getValue();

        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().setValue(value);
        }
    }

    @Override
    public void ensureDebugID(final String debugID) {
        this.debugID = debugID;
        if (fields.isEmpty()) return;

        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().ensureDebugId(debugID);
        }
    }

    @Override
    public <H extends PEventHandler> void addDomHandler(final H handler, final Type<H> type) {
        for (final FormFieldComponent<PDateBox> field : fields) {
            field.getInput().addDomHandler(handler, type);
        }
    }

    public void setTimeZone(final TimeZone timeZone) {
        for (final FormFieldComponent<PDateBox> p : fields) {
            p.getInput().setTimeZone(timeZone);
        }
    }
}
