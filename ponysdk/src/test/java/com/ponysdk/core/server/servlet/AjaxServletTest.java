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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
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

        // The caller's HTTP session owns the application (keyed by session id)
        final HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getId()).thenReturn("0");
        Mockito.when(request.getSession(false)).thenReturn(session);

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

        // The request owned by this session reaches its own object
        Mockito.verify(pObject, Mockito.times(1)).handleAjaxRequest(request, response);

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

        // No HTTP session on the request → no context can be resolved → denied
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ajaxServlet.doPost(request, response);

        Mockito.verify(response, Mockito.times(1)).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_NOT_FOUND),
            ArgumentMatchers.anyString());
    }

    /**
     * Non-regression (IDOR): a request from one HTTP session must not reach a UIContext owned by
     * another session, even if it guesses the (sequential) context id.
     */
    @Test
    public void testCrossSessionAccessIsDenied() throws ServletException, IOException {
        final int victimContextID = 1;
        final int pObjectID = 2;

        // Victim's context lives in the victim's application (session "victim")
        final Application victim = new Application("victim", null, null);
        final UIContext victimContext = Mockito.mock(UIContext.class);
        Mockito.when(victimContext.getID()).thenReturn(victimContextID);
        Mockito.when(victimContext.isAlive()).thenReturn(true);
        victim.registerUIContext(victimContext);
        SessionManager.get().registerApplication(victim);

        // Attacker: different HTTP session, guesses the victim's context id
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(ClientToServerModel.UI_CONTEXT_ID.name())).thenReturn(String.valueOf(victimContextID));
        Mockito.when(request.getHeader(ClientToServerModel.OBJECT_ID.name())).thenReturn(String.valueOf(pObjectID));
        final HttpSession attackerSession = Mockito.mock(HttpSession.class);
        Mockito.when(attackerSession.getId()).thenReturn("attacker");
        Mockito.when(request.getSession(false)).thenReturn(attackerSession);

        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ajaxServlet.doGet(request, response);

        // The attacker never touches the victim's context, and gets a generic 404
        Mockito.verify(victimContext, Mockito.never()).execute(ArgumentMatchers.any(Runnable.class));
        Mockito.verify(victimContext, Mockito.never()).getObject(ArgumentMatchers.anyInt());
        Mockito.verify(response, Mockito.times(1)).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_NOT_FOUND),
            ArgumentMatchers.anyString());

        SessionManager.get().unregisterApplication(victim);
    }

    /** A live context of the session "ajax", as a servlet thread finds it: not the current one. */
    private UIContext liveContext(final Application application) {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(HttpSession.class)));
        final TxnContext txnContext = new TxnContext(socket);
        txnContext.setApplication(application);
        final UIContext uiContext = new UIContext(socket, txnContext, config);
        application.registerUIContext(uiContext);
        return uiContext;
    }

    private static HttpServletRequest requestOf(final String sessionId, final String contextId, final String objectId) {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(ClientToServerModel.UI_CONTEXT_ID.name())).thenReturn(contextId);
        Mockito.when(request.getHeader(ClientToServerModel.OBJECT_ID.name())).thenReturn(objectId);
        final HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getId()).thenReturn(sessionId);
        Mockito.when(request.getSession(false)).thenReturn(session);
        return request;
    }

    private interface Exchange {

        void run(UIContext uiContext, HttpServletResponse response) throws Exception;
    }

    private void withLiveContext(final Exchange exchange) throws Exception {
        final Application application = new Application("ajax", Mockito.mock(HttpSession.class), new ApplicationConfiguration());
        SessionManager.get().registerApplication(application);
        final UIContext uiContext = liveContext(application);
        try {
            exchange.run(uiContext, Mockito.mock(HttpServletResponse.class));
        } finally {
            uiContext.destroy();
            SessionManager.get().unregisterApplication(application);
            UIContext.remove();
        }
    }

    private static PObject registeredObject(final UIContext uiContext, final int id) {
        final PObject pObject = Mockito.mock(PObject.class);
        Mockito.when(pObject.getID()).thenReturn(id);
        uiContext.execute(() -> uiContext.registerObject(pObject));
        return pObject;
    }

    @Test
    public void anObjectOfTheContextAnswersItsRequest() throws Exception {
        withLiveContext((uiContext, response) -> {
            final PObject pObject = registeredObject(uiContext, 5);
            final HttpServletRequest request = requestOf("ajax", String.valueOf(uiContext.getID()), "5");

            ajaxServlet.doPost(request, response);

            Mockito.verify(pObject).handleAjaxRequest(request, response);
            Mockito.verify(response, Mockito.never()).sendError(ArgumentMatchers.anyInt(), ArgumentMatchers.any());
        });
    }

    @Test
    public void anUnknownObjectIsNotFound() throws Exception {
        withLiveContext((uiContext, response) -> {
            ajaxServlet.doGet(requestOf("ajax", String.valueOf(uiContext.getID()), "999"), response);

            Mockito.verify(response).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_NOT_FOUND), ArgumentMatchers.anyString());
        });
    }

    @Test
    public void aFailingObjectAnswers500() throws Exception {
        withLiveContext((uiContext, response) -> {
            final PObject pObject = registeredObject(uiContext, 6);
            Mockito.doThrow(new IOException("disk full")).when(pObject).handleAjaxRequest(ArgumentMatchers.any(), ArgumentMatchers.any());

            ajaxServlet.doGet(requestOf("ajax", String.valueOf(uiContext.getID()), "6"), response);

            Mockito.verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "disk full");
        });
    }

    @Test
    public void aFailingErrorAnswerIsOnlyLogged() throws Exception {
        withLiveContext((uiContext, response) -> {
            final PObject pObject = registeredObject(uiContext, 7);
            Mockito.doThrow(new ServletException("bad")).when(pObject).handleAjaxRequest(ArgumentMatchers.any(), ArgumentMatchers.any());
            Mockito.doThrow(new IOException("reset")).when(response).sendError(ArgumentMatchers.anyInt(), ArgumentMatchers.any());

            ajaxServlet.doGet(requestOf("ajax", String.valueOf(uiContext.getID()), "7"), response);

            Mockito.verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "bad");
        });
    }

    @Test
    public void aMalformedContextIdAnswers500() throws IOException {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ajaxServlet.doGet(requestOf("ajax", "not-a-number", "1"), response);
        Mockito.verify(response).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), ArgumentMatchers.any());
    }

    @Test
    public void aFailingErrorResponseIsLogged() throws IOException {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.doThrow(new IOException("reset")).when(response).sendError(ArgumentMatchers.anyInt(), ArgumentMatchers.any());

        ajaxServlet.doGet(requestOf("ajax", "not-a-number", "1"), response);
        ajaxServlet.doPost(requestOf("ajax", "not-a-number", "1"), response);

        Mockito.verify(response, Mockito.times(2)).sendError(ArgumentMatchers.eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
            ArgumentMatchers.any());
    }

}
