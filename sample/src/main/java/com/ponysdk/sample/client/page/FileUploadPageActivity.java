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

package com.ponysdk.sample.client.page;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PFileUpload;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSubmitCompleteHandler;

public class FileUploadPageActivity extends SamplePageActivity {

    public FileUploadPageActivity() {
        super("File Upload", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();
        panel.setSpacing(10);

        final PFileUpload fileUpload = new PFileUpload();
        fileUpload.setName("my_file");
        fileUpload.addSubmitCompleteHandler(new PSubmitCompleteHandler() {

            @Override
            public void onSubmitComplete() {
                PNotificationManager.showTrayNotification("File uploaded, submit file '" + fileUpload.getFileName() + "'");
            }
        });

        fileUpload.addStreamHandler(new StreamHandler() {

            @Override
            public void onStream(final HttpServletRequest request, final HttpServletResponse response) {
                try {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    response.getWriter().print("The file was created successfully.");
                    response.flushBuffer();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });

        final PButton submitButton = new PButton("Upload File");
        submitButton.showLoadingOnRequest(true);
        submitButton.setEnabledOnRequest(false);

        submitButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                fileUpload.submit();
            }
        });

        panel.add(fileUpload);
        panel.add(submitButton);

        examplePanel.setWidget(panel);
    }
}
