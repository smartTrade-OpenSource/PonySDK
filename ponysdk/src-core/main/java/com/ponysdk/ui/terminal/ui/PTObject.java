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

package com.ponysdk.ui.terminal.ui;

import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public interface PTObject {

    public void create(final ReaderBuffer buffer, int objectId, final UIService uiService);

    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel);

    public void add(final ReaderBuffer buffer, final PTObject ptObject);

    public void remove(final ReaderBuffer buffer, final PTObject ptObject, final UIService uiService);

    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService);

    public void removeHandler(final ReaderBuffer buffer, final UIService uiService);

    public void gc(final UIService uiService);

    public int getObjectID();

    public PTWidget<?> isPTWidget();

}
