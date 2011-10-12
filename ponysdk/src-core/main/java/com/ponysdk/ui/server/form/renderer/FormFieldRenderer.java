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

import com.ponysdk.core.event.EventHandler;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.PDomEvent;
import com.ponysdk.ui.server.form.FormField;

public interface FormFieldRenderer {

    public IsPWidget render(FormField formField);

    public void reset();

    public void addErrorMessage(String errorMessage);

    public void clearErrorMessage();

    public void setEnabled(boolean enabled);

    public boolean isEnabled();

    public void setValue(Object value);

    public Object getValue();

    public void ensureDebugID(String id);

    public <H extends EventHandler> void addDomHandler(final H handler, final PDomEvent.Type<H> type);

}
