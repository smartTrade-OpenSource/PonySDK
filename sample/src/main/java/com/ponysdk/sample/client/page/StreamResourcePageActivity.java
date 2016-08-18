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

package com.ponysdk.sample.client.page;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.eventbus.StreamHandler;

public class StreamResourcePageActivity extends SamplePageActivity {

    public StreamResourcePageActivity() {
        super("Stream resource", "Resource management");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlowPanel panel = new PFlowPanel();

        final PButton downloadImageButton = new PButton("Download Pony image");

        downloadImageButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                UIContext.get().stackStreamRequest(new StreamHandler() {

                    @Override
                    public void onStream(final HttpServletRequest request, final HttpServletResponse response) {
                        response.reset();
                        response.setContentType("image/png");
                        response.setHeader("Content-Disposition", "attachment; filename=pony_image.png");

                        try {
                            final OutputStream output = response.getOutputStream();
                            final InputStream input = getClass().getClassLoader().getResourceAsStream("images/pony.png");

                            final byte[] buff = new byte[1024];
                            while (input.read(buff) != -1) {
                                output.write(buff);
                            }

                            output.flush();
                            output.close();
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        panel.add(downloadImageButton);

        examplePanel.setWidget(panel);
    }
}
