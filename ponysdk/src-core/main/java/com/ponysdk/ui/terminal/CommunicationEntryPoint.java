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

package com.ponysdk.ui.terminal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.timepedia.exporter.client.ExporterUtil;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.Window;

public class CommunicationEntryPoint implements EntryPoint {

    private final static Logger log = Logger.getLogger(CommunicationEntryPoint.class.getName());

    @Override
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void onUncaughtException(final Throwable e) {
                log.log(Level.SEVERE, "PonySDK has encountered an internal error : ", e);
                Window.alert("PonySDK has encountered an internal error : " + e.getMessage());
            }
        });

        ExporterUtil.exportAll();

        onLoadImpl();
    }

    private native void onLoadImpl() /*-{
                                     if ($wnd.onPonySDKModuleLoaded && typeof $wnd.onPonySDKModuleLoaded == 'function') $wnd.onPonySDKModuleLoaded();
                                     }-*/;
}
