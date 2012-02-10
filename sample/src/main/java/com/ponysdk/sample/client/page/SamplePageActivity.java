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
import java.io.InputStream;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.DefaultPageView;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PTabPanel;

import de.java2html.converter.JavaSource2HTMLConverter;
import de.java2html.javasource.JavaSource;
import de.java2html.javasource.JavaSourceParser;
import de.java2html.options.JavaSourceConversionOptions;

public class SamplePageActivity extends PageActivity {

    private static Logger log = LoggerFactory.getLogger(SamplePageActivity.class);

    private PTabPanel tabPanel;

    protected PSimpleLayoutPanel examplePanel;

    private PScrollPanel codePanel;

    public SamplePageActivity(final String pageName, final String pageCategory) {
        super(pageName, pageCategory);
        setPageView(new DefaultPageView(pageName));
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(final Place place) {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {
        codePanel = new PScrollPanel();
        examplePanel = new PSimpleLayoutPanel();
        examplePanel.setSizeFull();

        tabPanel = new PTabPanel();

        tabPanel.add(examplePanel, "Example");

        codePanel.setWidget(new PHTML(getSource()));

        tabPanel.add(codePanel, "Source Code");

        pageView.getBody().setWidget(tabPanel);

    }

    protected String getSource() {

        String fileName = getClass().getName().replaceAll("\\.", "/") + ".java";
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = null;
        try {

            inputStream = classLoader.getResourceAsStream(fileName);
            if (inputStream == null) return "Unable to load file: " + fileName;

            StringWriter writer = new StringWriter();
            JavaSource javaSource = new JavaSourceParser().parse(inputStream);
            JavaSource2HTMLConverter converter = new JavaSource2HTMLConverter();
            converter.convert(javaSource, JavaSourceConversionOptions.getDefault(), writer);

            return writer.toString();

        } catch (IOException e) {
            log.error("", e);
        }

        return "";
    }
}
