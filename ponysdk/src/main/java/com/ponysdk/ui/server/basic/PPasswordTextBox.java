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

import com.ponysdk.ui.terminal.WidgetType;

/**
 * A text box that visually masks its input to prevent eavesdropping. <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-PasswordTextBox { primary style }</li>
 * <li>.gwt-PasswordTextBox-readonly { dependent style set when the password text box is read-only }</li>
 * </ul>
 */
public class PPasswordTextBox extends PTextBox {

    public PPasswordTextBox() {
        super();
    }

    public PPasswordTextBox(final String text) {
        super(text);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.PASSWORD_TEXTBOX;
    }

}
