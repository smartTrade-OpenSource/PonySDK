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

package com.ponysdk.sample.client.playground;

import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * Represents a form control with its metadata.
 * <p>
 * Immutable value object combining a label, input control, error label,
 * the method signature it represents, a type hint and a category.
 * </p>
 *
 * @param label      the label displaying the method name, must not be null
 * @param control    the input control widget, must not be null
 * @param errorLabel the label for displaying errors, must not be null
 * @param method     the method signature this control represents, must not be null
 * @param typeHint   the human-readable type hint, must not be null
 * @param category   the property category for grouping, must not be null
 */
public record PropertyControl(
    PLabel label,
    PWidget control,
    PLabel errorLabel,
    MethodSignature method,
    String typeHint,
    PropertyCategory category
) {
    public PropertyControl {
        if (label == null) throw new IllegalArgumentException("label must not be null");
        if (control == null) throw new IllegalArgumentException("control must not be null");
        if (errorLabel == null) throw new IllegalArgumentException("errorLabel must not be null");
        if (method == null) throw new IllegalArgumentException("method must not be null");
        if (typeHint == null) throw new IllegalArgumentException("typeHint must not be null");
        if (category == null) throw new IllegalArgumentException("category must not be null");
    }
}
