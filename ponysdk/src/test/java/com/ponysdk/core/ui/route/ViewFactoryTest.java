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

package com.ponysdk.core.ui.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;

public class ViewFactoryTest {

    private static final RouteParameters NONE = new RouteParameters("", Map.of(), Map.of());

    static class Plain implements IsPWidget {

        private Plain() { // private: the factory reaches it
        }

        @Override
        public PWidget asWidget() {
            return null;
        }
    }

    static class WithParameters implements IsPWidget {

        final RouteParameters parameters;

        WithParameters(final RouteParameters parameters) {
            this.parameters = parameters;
        }

        @Override
        public PWidget asWidget() {
            return null;
        }
    }

    static class Neither implements IsPWidget {

        Neither(final String text) {
        }

        @Override
        public PWidget asWidget() {
            return null;
        }
    }

    abstract static class Abstract implements IsPWidget {
    }

    @Test
    public void aViewWithoutParametersIsBuiltWithoutThem() {
        assertEquals(Plain.class, new ViewFactory(Plain.class).create(NONE).getClass());
    }

    @Test
    public void aViewWithParametersGetsThem() {
        assertSame(NONE, ((WithParameters) new ViewFactory(WithParameters.class).create(NONE)).parameters);
    }

    @Test
    public void aClassThatCannotBeAViewIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ViewFactory(Neither.class));
        assertThrows(IllegalArgumentException.class, () -> new ViewFactory(Abstract.class));
        assertThrows(IllegalArgumentException.class, () -> new ViewFactory(IsPWidget.class));
        assertThrows(IllegalArgumentException.class, () -> new ViewFactory(String.class));
    }
}
