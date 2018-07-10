/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.driver;

import com.ponysdk.core.model.ServerToClientModel;

public class PonyFrame {

    final ServerToClientModel model;
    final Object value;

    PonyFrame(final ServerToClientModel model, final Object value) {
        super();
        this.model = model;
        this.value = value;
    }

    public ServerToClientModel getModel() {
        return model;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "model=" + model + ", value=" + value;
    }
}