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

import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

public class PDialogBox extends PDecoratedPopupPanel {

    private String caption;

    public PDialogBox() {
        super(false);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DIALOG_BOX;
    }

    public void setCaption(final String caption) {
        this.caption = caption;
        final Update update = new Update(ID);
        update.put(PROPERTY.POPUP_CAPTION, caption);
        getPonySession().stackInstruction(update);
    }

    public String getCaption() {
        return caption;
    }

}
