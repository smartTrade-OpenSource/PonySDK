/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.binder;

import java.util.List;
import java.util.function.Supplier;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PTextBox;

import jakarta.json.Json;
import jakarta.validation.constraints.Size;

/**
 * What a form does with a bean that has a constraint, run by {@link BinderWithoutBeanValidationTest} in a class loader
 * where Bean Validation is missing, or has no provider: the constraint is not applied and nothing fails.
 */
public class BinderScenario implements Supplier<List<String>> {

    public static class Plain {

        @Size(max = 3, message = "Too long")
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }
    }

    @Override
    public List<String> get() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PTextBox box = Element.newPTextBox();
        panel.add(box);
        final Binder<Plain> binder = new Binder<>(Plain.class);
        binder.forField(box).bind(Plain::getCode, Plain::setCode);

        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), "abcdef").build());
        final Plain plain = new Plain();
        final boolean written = binder.writeBean(plain);
        return List.of(String.valueOf(written), String.valueOf(box.getAttribute("aria-invalid")), plain.getCode(),
            String.valueOf(binder.getStatus().valid()));
    }
}
