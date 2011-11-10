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
import java.util.List;

import com.ponysdk.core.event.EventHandler;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.event.HasPValueChangeHandlers;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form.FormField;

public class CheckBoxFormFieldRenderer implements FormFieldRenderer, PValueChangeHandler<Boolean>, HasPValueChangeHandlers<Boolean> {
	
    private String caption;

    protected final List<FormFieldComponent<PCheckBox>> fields = new ArrayList<FormFieldComponent<PCheckBox>>();
    private List<PValueChangeHandler<Boolean>> valuesHandlers = new ArrayList<PValueChangeHandler<Boolean>>();

    private boolean enabled = true;
    private boolean value = false;
    
    protected String debugID;
	
    public CheckBoxFormFieldRenderer() {
    	this(null);
    }
    
    public CheckBoxFormFieldRenderer(String caption) {
    	this.caption = caption;
    }
    
	@Override
	public IsPWidget render(FormField formField) {
		final PCheckBox checkBox = new PCheckBox();
        if (debugID != null) {
            checkBox.ensureDebugId(debugID);
        }
        final FormFieldComponent<PCheckBox> checkBoxField = buildCheckBoxField(checkBox);
        fields.add(checkBoxField);
        return checkBoxField;
    }

    protected FormFieldComponent<PCheckBox> buildCheckBoxField(PCheckBox checkBox) {
        final FormFieldComponent<PCheckBox> formFieldComponent = new FormFieldComponent<PCheckBox>(checkBox);
        formFieldComponent.getInput().addValueChangeHandler(this);
        formFieldComponent.getInput().setValue(value);
        formFieldComponent.getInput().setEnabled(enabled);
        formFieldComponent.setCaption(caption);

        return formFieldComponent;
    }

	@Override
	public void reset() {
		value = false;
	}

	@Override
	public void addErrorMessage(String errorMessage) {
        for (final FormFieldComponent<PCheckBox> field : fields) {
            field.addErrorMessage(errorMessage);
        }
	}

	@Override
	public void clearErrorMessage() {
        for (final FormFieldComponent<PCheckBox> field : fields) {
            field.clearErrors();
        }
	}

	@Override
	public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for (final FormFieldComponent<PCheckBox> field : fields) {
            field.getInput().setEnabled(enabled);
        }
	}

	@Override
	public boolean isEnabled() {
		return enabled;
 	}

	@Override
	public void setValue(Object value) {
        this.value = (Boolean) value;
        for (final FormFieldComponent<PCheckBox> field : fields) {
            field.getInput().setValue(this.value);
        }
	}

	@Override
	public Boolean getValue() {
		return value;
	}

	@Override
	public void ensureDebugID(String id) {
		this.debugID = id;
	}

	@Override
	public <H extends EventHandler> void addDomHandler(H handler, Type<H> type) {
        for (final FormFieldComponent<PCheckBox> field : fields) {
            field.getInput().addDomHandler(handler, type);
        }
	}

	@Override
	public void addValueChangeHandler(PValueChangeHandler<Boolean> handler) {
		valuesHandlers.add(handler);
	}

	@Override
	public void removeValueChangeHandler(PValueChangeHandler<Boolean> handler) {
		valuesHandlers.remove(handler);
	}

	@Override
	public Collection<PValueChangeHandler<Boolean>> getValueChangeHandlers() {
		return valuesHandlers;
	}

	@Override
	public void onValueChange(Boolean value) {
		for (PValueChangeHandler<Boolean> handler : valuesHandlers) {
			handler.onValueChange(value);
		}
	}

}
