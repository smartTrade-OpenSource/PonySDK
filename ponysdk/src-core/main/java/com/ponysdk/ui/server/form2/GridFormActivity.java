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

package com.ponysdk.ui.server.form2;

import com.ponysdk.ui.server.form2.formfield.FormField;

public class GridFormActivity extends FormActivity {

    private final GridFormView gridFormView;

    public GridFormActivity(final GridFormView formView) {
        super(formView);
        this.gridFormView = formView;
    }

    public void addFormField(final int row, final int col, final String caption, final FormField<?> formField) {
        getFormView().addFormField(row, col, caption, formField.asWidget());
        captionByFormField.put(formField, caption);
        formFieldByCaption.put(caption, formField);
    }

    @Override
    public GridFormView getFormView() {
        return gridFormView;
    }

}
