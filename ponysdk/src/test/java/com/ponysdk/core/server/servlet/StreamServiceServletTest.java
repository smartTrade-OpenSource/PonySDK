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
import jakarta.servlet.http.HttpSession;

import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.eventbus.StreamHandler;

public class StreamServiceServletTest {

    private final StreamServiceServlet streamServiceServlet = new StreamServiceServlet();

    /**
     * Test method for
     * {@link com.ponysdk.core.server.servlet.StreamServiceServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void testDoGet() throws ServletException, IOException {
        final int uiContextID = 1;
        final int streamRequestID = 2;

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter(ClientToServerModel.UI_CONTEXT_ID.toStringValue())).thenReturn(String.valueOf(uiContextID));
        Mockito.when(request.getParameter(ClientToServerModel.STREAM_REQUEST_ID.toStringValue()))
            .thenReturn(String.valueOf(streamRequestID));

        // The caller's HTTP session owns the application (keyed by session id)
        final HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getId()).thenReturn("0");
        Mockito.when(request.getSession(false)).thenReturn(session);

        final Application application = new Application("0", null, null);
        final UIContext uiContext = Mockito.mock(UIContext.class);
        Mockito.when(uiContext.getID()).thenReturn(uiContextID);
        final StreamHandler streamListener = Mockito.mock(StreamHandler.class);
        Mockito.when(uiContext.removeStreamListener(streamRequestID)).thenReturn(streamListener);
        application.registerUIContext(uiContext);
        SessionManager.get().registerApplication(application);

        final PObject pObject = Mockito.mock(PObject.class);
        Mockito.when(uiContext.getObject(ArgumentMatchers.eq(streamRequestID))).thenReturn(pObject);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        streamServiceServlet.doGet(request, response);
        Mockito.verify(streamListener, Mockito.times(1)).onStream(request, response, uiContext);

        SessionManager.get().unregisterApplication(application);
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.servlet.StreamServiceServlet#doPost(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void testDoPost() throws ServletException, IOException {
        final int uiContextID = 1;

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter(ClientToServerModel.UI_CONTEXT_ID.toStringValue())).thenReturn(String.valueOf(uiContextID));
        Mockito.when(request.getParameter(ClientToServerModel.STREAM_REQUEST_ID.toStringValue())).thenReturn("2");

        final HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getId()).thenReturn("0");
        Mockito.when(request.getSession(false)).thenReturn(session);

        final Application application = new Application("0", null, null);
        final UIContext uiContext = Mockito.mock(UIContext.class);
        Mockito.when(uiContext.getID()).thenReturn(uiContextID);
        application.registerUIContext(uiContext);
        SessionManager.get().registerApplication(application);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        streamServiceServlet.doPost(request, response);

        // Valid session but no stream handler registered → internal error
        Mockito.verify(response, Mockito.times(1)).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
            ArgumentMatchers.any());

        SessionManager.get().unregisterApplication(application);
    }

    /**
     * Non-regression (IDOR): a stream request from one HTTP session must not reach a UIContext
     * owned by another session, even if it guesses the (sequential) context id.
     */
    @Test
    public void testCrossSessionAccessIsDenied() throws ServletException, IOException {
        final int victimContextID = 1;

        final Application victim = new Application("victim", null, null);
        final UIContext victimContext = Mockito.mock(UIContext.class);
        Mockito.when(victimContext.getID()).thenReturn(victimContextID);
        victim.registerUIContext(victimContext);
        SessionManager.get().registerApplication(victim);

        // Attacker: different HTTP session, guesses the victim's context id
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter(ClientToServerModel.UI_CONTEXT_ID.toStringValue()))
            .thenReturn(String.valueOf(victimContextID));
        Mockito.when(request.getParameter(ClientToServerModel.STREAM_REQUEST_ID.toStringValue())).thenReturn("2");
        final HttpSession attackerSession = Mockito.mock(HttpSession.class);
        Mockito.when(attackerSession.getId()).thenReturn("attacker");
        Mockito.when(request.getSession(false)).thenReturn(attackerSession);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        streamServiceServlet.doGet(request, response);

        // The attacker never removes a listener from the victim's context, and gets a generic 404
        Mockito.verify(victimContext, Mockito.never()).removeStreamListener(ArgumentMatchers.anyInt());
        Mockito.verify(response, Mockito.times(1)).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_NOT_FOUND),
            ArgumentMatchers.anyString());

        SessionManager.get().unregisterApplication(victim);
    }

}
