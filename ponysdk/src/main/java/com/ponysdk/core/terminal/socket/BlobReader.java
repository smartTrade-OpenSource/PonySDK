/*============================================================================
 *
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

package com.ponysdk.core.terminal.socket;

import java.util.LinkedList;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.ArrayBuffer;
import elemental.html.Blob;
import elemental.html.FileReader;
import elemental.html.Window;

public class BlobReader implements MessageReader {

    private static final String LOAD_FILE_READER_KEY = "load";

    private final MessageSender messageSender;

    private final LinkedList<Blob> queue;
    private final FileReader fileReader;

    public BlobReader(final MessageSender messageSender) {
        this.messageSender = messageSender;

        this.queue = new LinkedList<>();

        final Window window = Browser.getWindow();
        this.fileReader = window.newFileReader();
        fileReader.setOnload(new EventListener() {

            @Override
            public void handleEvent(final Event event) {
                if (LOAD_FILE_READER_KEY.equals(event.getType())) {
                    BlobReader.this.messageSender.read((ArrayBuffer) fileReader.getResult());
                }

                if (!queue.isEmpty()) {
                    final Blob blob = queue.removeFirst();
                    fileReader.readAsArrayBuffer(blob);
                }
            }
        });
    }

    @Override
    public void read(final MessageEvent event) {
        final Blob blob = (Blob) event.getData();
        if (fileReader.getReadyState() != 1) {
            fileReader.readAsArrayBuffer(blob);
        } else {
            queue.add(blob);
        }
    }

}
