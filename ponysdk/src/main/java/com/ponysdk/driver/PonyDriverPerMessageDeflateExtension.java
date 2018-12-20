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

import org.glassfish.tyrus.core.frame.Frame;
import org.glassfish.tyrus.ext.extension.deflate.PerMessageDeflateExtension;

public class PonyDriverPerMessageDeflateExtension extends PerMessageDeflateExtension {

    private final PonyBandwithListener listener;

    public PonyDriverPerMessageDeflateExtension(final PonyBandwithListener listener) {
        super();
        this.listener = listener;
    }

    @Override
    public Frame processIncoming(final ExtensionContext context, final Frame frame) {
        listener.onReceiveCompressed(getFrameHeaderLength(frame) + (int) frame.getPayloadLength());
        return super.processIncoming(context, frame);
    }

    @Override
    public Frame processOutgoing(final ExtensionContext context, final Frame frame) {
        final Frame f = super.processOutgoing(context, frame);
        listener.onSendCompressed(getFrameHeaderLength(f) + (int) f.getPayloadLength());
        return f;
    }

    private static int getFrameHeaderLength(final Frame frame) {
        final int payloadLength = (int) frame.getPayloadLength();
        int length = 1;

        // if length is over 65535 then its a 7 + 64 bit length
        if (payloadLength > 0xFF_FF) length += 9;
        // if payload is greater that 126 we have a 7 + 16 bit length
        else if (payloadLength >= 0x7E) length += 3;
        // we have a 7 bit length
        else length += 1;

        if (frame.isMask()) length++;

        return length;
    }
}
