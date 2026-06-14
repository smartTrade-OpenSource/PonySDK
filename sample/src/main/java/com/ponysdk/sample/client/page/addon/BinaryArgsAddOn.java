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

package com.ponysdk.sample.client.page.addon;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PElement;

/**
 * Showcase addon demonstrating the binary PAddOn protocol: it receives typed primitives in pure
 * binary both as creation arguments (via {@link PAddOnComposite#PAddOnComposite(com.ponysdk.core.ui.basic.PWidget, Object...)})
 * and as a large method-call array (beyond the old 255-element cap), then renders what it decoded.
 */
public class BinaryArgsAddOn extends PAddOnComposite<PElement> {

    public BinaryArgsAddOn(final Object... creationArgs) {
        super(Element.newDiv(), creationArgs);
    }

    /** Sends a deterministic verification frame (a typed binary array) to the terminal. */
    public void verify(final Object[] values) {
        callTerminalMethod("verify", values);
    }

    /** Streams one frame of typed binary values (an animated waveform) to the terminal. */
    public void stream(final Object[] values) {
        callTerminalMethod("stream", values);
    }
}
