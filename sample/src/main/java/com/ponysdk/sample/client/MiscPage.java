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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;
import static com.ponysdk.sample.client.Costs.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFrame;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.rich.PToolbar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Misc: HTML, scripts, frames, cookies and file upload. */
final class MiscPage {

    private static final Logger log = LoggerFactory.getLogger(MiscPage.class);
    private static final String VISITS = "showcase-visits";

    private MiscPage() {
    }

    // ── Tab: Misc ────────────────────────────────────────────────────────────

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Misc", "HTML, scripts, frames and cookies, all handled from the server.");

        p.add(sectionTitle("HTML / Anchors"));
        // region html
        final PHTML html = Element.newPHTML("Hello <b>HTML</b>");
        // endregion
        p.add(labeled("HTML", html, Source.code(MiscPage.class, "html")));
        // region anchor
        final PAnchor anchor = Element.newPAnchor("PonySDK site", "https://github.com/smartTrade-OpenSource/PonySDK");
        // endregion
        p.add(labeled("Anchor", anchor, Source.code(MiscPage.class, "anchor")));

        p.add(sectionTitle("Script"));
        // region pscript
        final PButton alertBtn = Element.newPButton("Run JS alert");
        alertBtn.addClickHandler(e -> PScript.execute(PWindow.getMain(), "alert('Hello from PScript!')"));
        // endregion
        p.add(labeled("PScript", alertBtn, Source.code(MiscPage.class, "pscript")));

        p.add(sectionTitle("Toolbar"));
        // region toolbar
        final PToolbar toolbar = new PToolbar();
        toolbar.addStyleName("misc-toolbar");
        final PLabel toolbarEcho = Element.newPLabel("server: no action yet");
        toolbarEcho.addStyleName("pony-ex-echo");
        for (final String action : new String[] { "New", "Open", "Save", "Export" }) {
            final PButton b = Element.newPButton(action);
            b.addClickHandler(e -> toolbarEcho.setText("server: " + action + " clicked"));
            toolbar.add(b);
        }
        // endregion
        p.add(labeled("PToolbar", stack(toolbar, toolbarEcho), Source.code(MiscPage.class, "toolbar")));

        p.add(sectionTitle("Frame"));
        // region frame
        final PFrame frame = Element.newPFrame("html/frame-demo.html"); // relative: same origin as the showcase
        frame.setHeight("260px");
        frame.setWidth("min(640px,100%)");
        frame.addStyleName("lay-frame");
        // endregion
        p.add(labeled("PFrame", frame, Source.code(MiscPage.class, "frame")));

        p.add(sectionTitle("Cookies"));
        // region cookies
        // The cookies of the application's origin, in its page or in an element of another page
        final PCookies cookies = UIContext.get().getCookies();
        final PLabel cookieLabel = Element.newPLabel();
        cookieLabel.addStyleName("misc-cookie");
        final Runnable count = () -> {
            int visits;
            try {
                visits = Integer.parseInt(String.valueOf(cookies.getCookie(VISITS))) + 1;
            } catch (final NumberFormatException e) {
                visits = 1;
            }
            cookies.setCookie(VISITS, String.valueOf(visits));
            cookieLabel.setText("Loaded " + visits + " time(s) in this browser: the cookie " + VISITS + " counts them");
        };
        cookies.addInitializedListener(count); // at once when the cookies are already known
        // endregion
        p.add(labeled("PCookies", cookieLabel, Source.code(MiscPage.class, "cookies")));

        return p;
    }

    static PFlowPanel createPFileUpload() {
        // region fileupload
        final PFlowPanel panel = Element.newPFlowPanel();
        final PFileUpload fileUpload = Element.newPFileUpload();
        fileUpload.setName("file");
        panel.add(fileUpload);
        fileUpload.addChangeHandler(event -> {
            final PFileUpload pFileUpload = (PFileUpload) event.getSource();
            log.debug("File name: {}", pFileUpload.getFileName());
            log.debug("File size: {} bytes", pFileUpload.getFileSize());
        });
        fileUpload.addStreamHandler((request, response, context) -> {
            try {
                final List<FileItem> items = new JakartaServletFileUpload(DiskFileItemFactory.builder().get()).parseRequest(request);
                for (final FileItem item : items) {
                    if (!item.isFormField()) readFileItem(item);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        final PButton button = Element.newPButton("Submit");
        button.addClickHandler(event -> fileUpload.submit());
        panel.add(button);
        // endregion
        return panel;
    }

    static void readFileItem(final FileItem<?> item) throws IOException, FileNotFoundException {
        // Store the uploaded file on the server (don't forget to remove)
        final String fileName = FilenameUtils.getName(item.getName());
        final InputStream fileContent = item.getInputStream();
        final File uploadedFile = File.createTempFile(fileName, "fileUpload");
        IOUtils.copy(fileContent, new FileOutputStream(uploadedFile));
        uploadedFile.deleteOnExit();

        // Read directly the input stream
        final BufferedReader reader = new BufferedReader(new InputStreamReader(item.getInputStream(), "UTF-8"));
        final StringBuilder value = new StringBuilder();
        final char[] buffer = new char[1024];
        for (int length = 0; (length = reader.read(buffer)) > 0;) {
            value.append(buffer, 0, length);
        }
        log.debug("File content: {}", value);
    }
}
