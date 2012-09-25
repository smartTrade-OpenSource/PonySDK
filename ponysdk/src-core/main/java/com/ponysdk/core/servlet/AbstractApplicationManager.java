
package com.ponysdk.core.servlet;

import java.util.List;

import javax.servlet.ServletException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.exception.PonySessionException;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    public void process(final Request request, final Response response) throws Exception {
        final JSONObject data = new JSONObject(new JSONTokener(request.getReader()));
        if (data.has(APPLICATION.KEY)) {
            startApplication(data, request, response);
        } else {
            fireInstructions(data, request, response);
        }
    }

    // public static String readFully(final Reader reader) throws IOException {
    // final char[] arr = new char[8 * 1024];
    // final StringBuffer buf = new StringBuffer();
    // int numChars;
    //
    // while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
    // buf.append(arr, 0, numChars);
    // }
    //
    // return buf.toString();
    // }

    public void startApplication(final JSONObject data, final Request request, final Response response) throws Exception {
        final JSONObject jsonObject = new JSONObject();

        final Session session = request.getSession();

        boolean isNewHttpSession = false;
        Application applicationSession = (Application) session.getAttribute(Application.class.getCanonicalName());
        if (applicationSession == null) {
            log.info("Creating a new application ... Session ID #" + session.getId());
            applicationSession = new Application(session);
            session.setUserAgent(request.getHeader("User-Agent"));
            session.setAttribute(Application.class.getCanonicalName(), applicationSession);
            isNewHttpSession = true;
        } else {
            log.info("Reloading application ... Session ID #" + session.getId());
        }

        synchronized (applicationSession) {
            final UIContext uiContext = new UIContext(applicationSession);

            jsonObject.put(APPLICATION.VIEW_ID, applicationSession.registerUIContext(uiContext));
            UIContext.setCurrent(uiContext);

            final long receivedSeqNum = data.getLong(APPLICATION.SEQ_NUM);
            uiContext.updateIncomingSeqNum(receivedSeqNum);

            final EntryPoint entryPoint = initializePonySession(uiContext);

            final String historyToken = data.getString(HISTORY.TOKEN);
            if (historyToken != null && !historyToken.isEmpty()) uiContext.getHistory().newItem(historyToken, false);

            final PCookies pCookies = uiContext.getCookies();
            final JSONArray cookies = data.getJSONArray(PROPERTY.COOKIES);
            for (int i = 0; i < cookies.length(); i++) {
                final JSONObject jsoObject = cookies.getJSONObject(i);
                final String name = jsoObject.getString(PROPERTY.KEY);
                final String value = jsoObject.getString(PROPERTY.VALUE);
                pCookies.cacheCookie(name, value);
            }

            if (isNewHttpSession) {
                entryPoint.start(uiContext);
            } else {
                entryPoint.restart(uiContext);
            }

            try {
                jsonObject.put(APPLICATION.SEQ_NUM, uiContext.getAndIncrementNextSentSeqNum());
                uiContext.flushInstructions(jsonObject);
                response.write(jsonObject.toString());
                response.flush();
            } catch (final Throwable e) {
                log.error("Cannot send instructions to the browser, Session ID #" + session.getId(), e);
            }
        }
    }

    protected void fireInstructions(final JSONObject data, final Request request, final Response response) throws Exception {
        final JSONObject jsonObject = new JSONObject();

        final long key = data.getLong(APPLICATION.VIEW_ID);
        final Session session = request.getSession();
        final Application applicationSession = (Application) session.getAttribute(Application.class.getCanonicalName());

        if (applicationSession == null) { throw new PonySessionException("Invalid session, please reload your application"); }

        final UIContext uiContext = applicationSession.getUIContext(key);

        if (uiContext == null) { throw new PonySessionException("Invalid session, please reload your application"); }

        uiContext.acquire();
        try {
            final long receivedSeqNum = data.getLong(APPLICATION.SEQ_NUM);
            if (!uiContext.updateIncomingSeqNum(receivedSeqNum)) {
                uiContext.stackIncomingMessage(receivedSeqNum, data);
                log.info("Stacking incoming message #" + receivedSeqNum);
                return;
            }

            UIContext.setCurrent(uiContext);
            final List<JSONObject> datas = uiContext.expungeIncomingMessageQueue(receivedSeqNum);
            for (final JSONObject jsoObject : datas) {
                process(uiContext, jsoObject);
            }

            process(uiContext, data);

            try {
                if (uiContext.flushInstructions(jsonObject)) {
                    jsonObject.put(APPLICATION.SEQ_NUM, uiContext.getAndIncrementNextSentSeqNum());
                    response.write(jsonObject.toString());
                    response.flush();
                }
            } catch (final Throwable e) {
                log.error("Cannot send instructions to the browser, Session ID #" + session.getId(), e);
            }
        } finally {
            UIContext.remove();
            uiContext.release();
        }
    }

    private void process(final UIContext uiContext, final JSONObject jsoObject) throws JSONException, PonySessionException {
        if (jsoObject.has(APPLICATION.INSTRUCTIONS)) {
            final JSONArray instructions = jsoObject.getJSONArray(APPLICATION.INSTRUCTIONS);
            for (int i = 0; i < instructions.length(); i++) {
                uiContext.fireInstruction(instructions.getJSONObject(i));
            }
        }
    }

    protected abstract EntryPoint initializePonySession(final UIContext ponySession) throws ServletException;

}
