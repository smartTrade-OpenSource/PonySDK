
package com.ponysdk.core;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.servlet.ServletException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.addon.Addon;
import com.ponysdk.core.addon.ScriptInjector;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.core.servlet.Session;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.exception.ServerException;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    private final ServiceLoader<Addon> addonServiceLoader = ServiceLoader.load(Addon.class);
    private final ServiceLoader<ScriptInjector> scriptInjectorServiceLoader = ServiceLoader.load(ScriptInjector.class);

    // private final Set<String> scripts = new HashSet<String>();

    public AbstractApplicationManager() {
        final Iterator<Addon> addons = addonServiceLoader.iterator();
        while (addons.hasNext()) {
            final Addon addon = addons.next();
            log.info("Addon detected : " + addon.getName());
        }
        final Iterator<ScriptInjector> scriptInjectors = scriptInjectorServiceLoader.iterator();
        while (scriptInjectors.hasNext()) {
            final ScriptInjector scriptInjector = scriptInjectors.next();
            log.info("Script dependencies for Addon #" + scriptInjector.getAddon().getName());
            for (final String script : scriptInjector.getScripts()) {
                log.info(script);
            }
            // scripts.addAll(scriptInjector.getScripts());
        }
        //
        // for (final String script : scripts) {
        // log.info("Injected script : " + script);
        // }
    }

    public void process(final Request request, final Response response) throws Exception {
        final JSONObject data = new JSONObject(new JSONTokener(request.getReader()));
        if (data.has(APPLICATION.KEY)) {
            startApplication(data, request, response);
        } else {
            fireInstructions(data, request, response);
        }
    }

    public void startApplication(final JSONObject data, final Request request, final Response response) throws Exception {
        final JSONObject jsonObject = new JSONObject();

        final Session session = request.getSession();

        Long reloadedViewID = null;
        boolean isNewHttpSession = false;
        Application applicationSession = (Application) session.getAttribute(Application.class.getCanonicalName());
        if (applicationSession == null) {
            log.info("Creating a new application ... Session ID #" + session.getId());
            applicationSession = new Application(session);
            session.setUserAgent(request.getHeader("User-Agent"));
            session.setAttribute(Application.class.getCanonicalName(), applicationSession);
            isNewHttpSession = true;
        } else {
            if (data.has(APPLICATION.VIEW_ID)) reloadedViewID = data.getLong(APPLICATION.VIEW_ID);
            log.info("Reloading application " + reloadedViewID + " on session #" + session.getId());
        }

        synchronized (applicationSession) {
            if (reloadedViewID != null) applicationSession.unregisterUIContext(reloadedViewID);

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

                // if (!scripts.isEmpty()) jsonObject.put(APPLICATION.SCRIPTS, scripts);

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

        if (applicationSession == null) { throw new ServerException(ServerException.INVALID_SESSION, "Invalid session, please reload your application"); }

        final UIContext uiContext = applicationSession.getUIContext(key);

        if (uiContext == null) { throw new ServerException(ServerException.INVALID_SESSION, "Invalid session, please reload your application"); }

        uiContext.acquire();
        try {
            printClientErrorMessage(data);

            final long receivedSeqNum = data.getLong(APPLICATION.SEQ_NUM);
            if (!uiContext.updateIncomingSeqNum(receivedSeqNum)) {
                uiContext.stackIncomingMessage(receivedSeqNum, data);
                log.info("Stacking incoming message #" + receivedSeqNum);
                return;
            }

            UIContext.setCurrent(uiContext);
            uiContext.begin();

            process(uiContext, data);

            final List<JSONObject> datas = uiContext.expungeIncomingMessageQueue(receivedSeqNum);
            for (final JSONObject jsoObject : datas) {
                process(uiContext, jsoObject);
            }

            uiContext.end();

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

    private void printClientErrorMessage(final JSONObject data) {
        try {
            final JSONArray errors = data.getJSONArray(APPLICATION.ERRORS);
            for (int i = 0; i < errors.length(); i++) {
                final JSONObject jsoObject = errors.getJSONObject(i);
                // TODO temp
                final String message = jsoObject.getString("message");
                final String details = jsoObject.getString("details");
                log.error("There was an unexpected error on the terminal. Message: " + message + ". Details: " + details);
            }
        } catch (final Exception e) {
            log.error("Failed to display errors", e);
        }
    }

    private void process(final UIContext uiContext, final JSONObject jsoObject) throws JSONException {
        if (jsoObject.has(APPLICATION.INSTRUCTIONS)) {
            final JSONArray instructions = jsoObject.getJSONArray(APPLICATION.INSTRUCTIONS);
            for (int i = 0; i < instructions.length(); i++) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = instructions.getJSONObject(i);
                    uiContext.fireClientData(jsonObject);
                } catch (final Throwable e) {
                    log.error("Failed to process instruction: " + jsonObject, e);
                }
            }
        }
    }

    protected abstract EntryPoint initializePonySession(final UIContext ponySession) throws ServletException;

}
