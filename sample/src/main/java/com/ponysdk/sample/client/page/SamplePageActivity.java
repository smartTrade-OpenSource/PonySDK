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

import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.place.Place;
import com.ponysdk.impl.webapplication.page.DefaultPageView;
import com.ponysdk.impl.webapplication.page.PageActivity;
import de.java2html.converter.JavaSource2HTMLConverter;
import de.java2html.javasource.JavaSource;
import de.java2html.javasource.JavaSourceParser;
import de.java2html.options.JavaSourceConversionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;

public class SamplePageActivity extends PageActivity {

    private static Logger log = LoggerFactory.getLogger(SamplePageActivity.class);

    protected PSimpleLayoutPanel examplePanel;

    public SamplePageActivity(final String pageName, final String pageCategory) {
        this(pageName, Collections.singleton(pageCategory));
    }

    public SamplePageActivity(final String pageName, final Collection<String> pageCategories) {
        super(pageName, pageCategories);
        setPageView(new DefaultPageView(pageName));
    }

    @Override
    protected void onInitialization() {
        // Nothing to do
    }

    @Override
    protected void onShowPage(final Place place) {
        // Nothing to do
    }

    @Override
    protected void onLeavingPage() {
        // Nothing to do
    }

    @Override
    protected void onFirstShowPage() {
        final PScrollPanel codePanel = Element.newPScrollPanel();

        examplePanel = new PSimpleLayoutPanel() {

            @Override
            public void setWidget(final PWidget w) {
                applyExampleStyle(w);
                super.setWidget(w);
            }
        };
        examplePanel.setSizeFull();

        final PTabLayoutPanel tabPanel = Element.newPTabLayoutPanel();

        tabPanel.add(examplePanel, "Example");

        final PHTML sourcePanel = Element.newPHTML(getSource());
        sourcePanel.addStyleName("codepanel");
        codePanel.setWidget(sourcePanel);

        tabPanel.add(codePanel, "Source Code");

        view.setWidget(tabPanel);
    }

    protected String getSource() {

        final String fileName = getClass().getName().replaceAll("\\.", "/") + ".java";
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = null;
        try {

            inputStream = classLoader.getResourceAsStream(fileName);
            if (inputStream == null) return "Unable to load file: " + fileName;

            final StringWriter writer = new StringWriter();
            final JavaSource javaSource = new JavaSourceParser().parse(inputStream);
            final JavaSource2HTMLConverter converter = new JavaSource2HTMLConverter();
            converter.convert(javaSource, JavaSourceConversionOptions.getDefault(), writer);

            return writer.toString();

        } catch (final IOException e) {
            log.error("", e);
        }

        return "";
    }

    protected void applyExampleStyle(final PWidget widget) {
        widget.addStyleName("examplepanel");
    }

}
