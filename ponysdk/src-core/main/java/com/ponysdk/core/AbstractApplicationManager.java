
package com.ponysdk.core;

import java.util.List;

import javax.servlet.ServletException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.core.servlet.Session;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.stm.TxnContextHttp;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.exception.ServerException;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    private final ApplicationManagerOption options;

    public AbstractApplicationManager() {
        this(new ApplicationManagerOption());
    }

    public AbstractApplicationManager(final ApplicationManagerOption options) {
        this.options = options;
        log.info("ApplicationManagerOption: " + options);
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
        final Session session = request.getSession();

        synchronized (session) {
            Long reloadedViewID = null;
            boolean isNewHttpSession = false;
            Application application = (Application) session.getAttribute(Application.class.getCanonicalName());
            if (application == null) {
                log.info("Creating a new application ... Session ID #" + session.getId() + " - " + request.getHeader("User-Agent") + " - " + request.getRemoteAddr());
                application = new Application(session, options);
                session.setUserAgent(request.getHeader("User-Agent"));
                session.setAttribute(Application.class.getCanonicalName(), application);
                isNewHttpSession = true;
            } else {
                if (data.has(APPLICATION.VIEW_ID)) reloadedViewID = data.getLong(APPLICATION.VIEW_ID);
                log.info("Reloading application " + reloadedViewID + " on session #" + session.getId());
            }

            if (reloadedViewID != null) {
                final UIContext previousUIContext = application.getUIContext(reloadedViewID);
                if (previousUIContext != null) previousUIContext.destroy();
            }

            final UIContext uiContext = new UIContext(application);
            UIContext.setCurrent(uiContext);

            try {
                final Txn txn = Txn.get();
                txn.begin(new TxnContextHttp(true, request, response));
                try {

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
                    txn.commit();
                } catch (final Throwable e) {
                    log.error("Cannot send instructions to the browser, Session ID #" + session.getId(), e);
                    txn.rollback();
                }
            } finally {
                UIContext.remove();
            }
        }
    }

    protected void fireInstructions(final JSONObject data, final Request request, final Response response) throws Exception {
        final long key = data.getLong(APPLICATION.VIEW_ID);
        final Session session = request.getSession();
        final Application applicationSession = (Application) session.getAttribute(Application.class.getCanonicalName());

        if (applicationSession == null) { throw new ServerException(ServerException.INVALID_SESSION, "Invalid session, please reload your application (viewID #" + key + ")."); }

        final UIContext uiContext = applicationSession.getUIContext(key);

        if (uiContext == null) { throw new ServerException(ServerException.INVALID_SESSION, "Invalid session (no UIContext found), please reload your application (viewID #" + key + ")."); }

        uiContext.acquire();
        UIContext.setCurrent(uiContext);
        try {
            final Txn txn = Txn.get();
            txn.begin(new TxnContextHttp(false, request, response));
            try {
                final Long receivedSeqNum = checkClientMessage(request.getSession(), data, uiContext);

                if (receivedSeqNum != null) {
                    process(uiContext, data);

                    final List<JSONObject> datas = uiContext.expungeIncomingMessageQueue(receivedSeqNum);
                    for (final JSONObject jsoObject : datas) {
                        process(uiContext, jsoObject);
                    }
                }

                txn.commit();
            } catch (final Throwable e) {
                log.error("Cannot process client instruction", e);
                txn.rollback();
            }
        } finally {
            UIContext.remove();
            uiContext.release();
        }
    }

    private Long checkClientMessage(final Session session, final JSONObject data, final UIContext uiContext) throws JSONException {
        printClientErrorMessage(data);

        final long receivedSeqNum = data.getLong(APPLICATION.SEQ_NUM);
        if (!uiContext.updateIncomingSeqNum(receivedSeqNum)) {
            final long key = data.getLong(APPLICATION.VIEW_ID);
            uiContext.stackIncomingMessage(receivedSeqNum, data);
            if (options.maxOutOfSyncDuration > 0 && uiContext.getLastSyncErrorTimestamp() > 0) {
                if (System.currentTimeMillis() - uiContext.getLastSyncErrorTimestamp() > options.maxOutOfSyncDuration) {
                    log.info("Unable to sync message for " + (System.currentTimeMillis() - uiContext.getLastSyncErrorTimestamp()) + " ms. Dropping connection (viewID #" + key + ").");
                    uiContext.destroy();
                    return null;
                }
            }
            log.info("Stacking incoming message #" + receivedSeqNum + ". Data #" + data + " (viewID #" + key + ")");
            return null;
        }
        return receivedSeqNum;

    }

    private void printClientErrorMessage(final JSONObject data) {
        try {
            final JSONArray errors = data.getJSONArray(APPLICATION.ERRORS);
            for (int i = 0; i < errors.length(); i++) {
                final JSONObject jsoObject = errors.getJSONObject(i);
                final String message = jsoObject.getString("message");
                final String details = jsoObject.getString("details");
                log.error("There was an unexpected error on the terminal. Message: " + message + ". Details: " + details);
            }
        } catch (final Throwable e) {
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
