/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.server.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PObject;

public class AjaxServletTest {

    private final AjaxServlet ajaxServlet = new AjaxServlet();

    /**
     * Test method for
     * {@link com.ponysdk.core.server.servlet.AjaxServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void testDoGet() throws ServletException, IOException {
        final int uiContextID = 1;
        final int pObjectID = 2;

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(ClientToServerModel.UI_CONTEXT_ID.name())).thenReturn(String.valueOf(uiContextID));
        Mockito.when(request.getHeader(ClientToServerModel.OBJECT_ID.name())).thenReturn(String.valueOf(pObjectID));

        final Application application = new Application("0", null, null);
        final UIContext uiContext = Mockito.mock(UIContext.class);
        Mockito.when(uiContext.getID()).thenReturn(uiContextID);
        Mockito.when(uiContext.isAlive()).thenReturn(true);
        Mockito.when(uiContext.execute(ArgumentMatchers.any(Runnable.class))).thenCallRealMethod();
        application.registerUIContext(uiContext);
        SessionManager.get().registerApplication(application);

        final PObject pObject = Mockito.mock(PObject.class);
        Mockito.when(uiContext.getObject(ArgumentMatchers.eq(pObjectID))).thenReturn(pObject);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        UIContext.setCurrent(uiContext);
        ajaxServlet.doGet(request, response);

        SessionManager.get().unregisterApplication(application);
        UIContext.setCurrent(null);
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.servlet.AjaxServlet#doPost(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void testDoPost() throws ServletException, IOException {
        final int uiContextID = 1;
        final int pObjectID = 2;

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(ClientToServerModel.UI_CONTEXT_ID.name())).thenReturn(String.valueOf(uiContextID));
        Mockito.when(request.getHeader(ClientToServerModel.OBJECT_ID.name())).thenReturn(String.valueOf(pObjectID));

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ajaxServlet.doPost(request, response);

        Mockito.verify(response, Mockito.times(1)).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
            ArgumentMatchers.anyString());
    }

}
