
package com.ponysdk.core;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.stm.TxnContext;
import com.ponysdk.ui.terminal.exception.ServerException;
import com.ponysdk.ui.terminal.model.Model;

public abstract class AbstractApplicationManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractApplicationManager.class);

    private final ApplicationManagerOption options;

    public AbstractApplicationManager() {
        this(new ApplicationManagerOption());
    }

    public AbstractApplicationManager(final ApplicationManagerOption options) {
        this.options = options;
        log.info(options.toString());
    }

    public void process(final TxnContext context) throws Exception {
        if (context.getApplication() == null) {
            final Integer reloadedViewID = null;
            boolean isNewHttpSession = false;
            Application application = context.getApplication();
            if (application == null) {
                application = new Application(context, options);
                context.setApplication(application);

                isNewHttpSession = true;
                log.info("Creating a new application, {}", application.toString());
            } else {
                // if (data.containsKey(Model.APPLICATION_VIEW_ID.getKey())) {
                // final JsonNumber jsonNumber = data.getJsonNumber(Model.APPLICATION_VIEW_ID.getKey());
                // reloadedViewID = jsonNumber.longValue();
                // }
                // log.info("Reloading application {} {}", reloadedViewID, context);
            }

            startApplication(context, application, isNewHttpSession, reloadedViewID);
        } else {
            final JsonReader reader = context.getReader();
            final JsonObject data = reader.readObject();
            fireInstructions(data, context);
        }
    }

    public void startApplication(final TxnContext context, Application application, boolean isNewHttpSession, Integer reloadedViewID)
            throws Exception {
        synchronized (context) {
            final UIContext uiContext = new UIContext(application, context);
            UIContext.setCurrent(uiContext);

            if (reloadedViewID != null) {
                final UIContext previousUIContext = application.getUIContext(reloadedViewID);
                if (previousUIContext != null) previousUIContext.destroy();
            }

            try {
                final Txn txn = Txn.get();
                txn.begin(context);
                try {

                    final int receivedSeqNum = context.getSeqNum();
                    uiContext.updateIncomingSeqNum(receivedSeqNum);// ??

                    final EntryPoint entryPoint = initializeUIContext(uiContext);

                    final String historyToken = context.getHistoryToken();

                    if (historyToken != null && !historyToken.isEmpty()) {
                        uiContext.getHistory().newItem(historyToken, false);
                    }

                    if (isNewHttpSession) {
                        entryPoint.start(uiContext);
                    } else {
                        entryPoint.restart(uiContext);
                    }

                    txn.commit();
                } catch (final Throwable e) {
                    log.error("Cannot send instructions to the browser " + context, e);
                    txn.rollback();
                }
            } finally {
                UIContext.remove();
            }
        }
    }

    protected void fireInstructions(final JsonObject data, final TxnContext context) throws Exception {
        // final int key = data.getJsonNumber(Model.APPLICATION_VIEW_ID.getKey()).intValue();
        final int key = 1;

        final Application applicationSession = context.getApplication();

        if (applicationSession == null) {
            throw new ServerException(ServerException.INVALID_SESSION,
                    "Invalid session, please reload your application (viewID #" + key + ").");
        }

        final UIContext uiContext = context.getUIContext();

        if (uiContext == null) {
            throw new ServerException(ServerException.INVALID_SESSION,
                    "Invalid session (no UIContext found), please reload your application (viewID #" + key + ").");
        }

        uiContext.execute(() -> process(uiContext, data));
    }

    private void printClientErrorMessage(final JsonObject data) {
        try {
            final JsonArray errors = data.getJsonArray(Model.APPLICATION_ERRORS.getKey());
            for (int i = 0; i < errors.size(); i++) {
                final JsonObject jsoObject = errors.getJsonObject(i);
                final String message = jsoObject.getString("message");
                final String details = jsoObject.getString("details");
                log.error("There was an unexpected error on the terminal. Message: " + message + ". Details: " + details);
            }
        } catch (final Throwable e) {
            log.error("Failed to display errors", e);
        }
    }

    private void process(final UIContext uiContext, final JsonObject jsonObject) {
        if (jsonObject.containsKey(Model.APPLICATION_INSTRUCTIONS.getKey())) {
            final JsonArray array = jsonObject.getJsonArray(Model.APPLICATION_INSTRUCTIONS.getKey());

            for (int i = 0; i < array.size(); i++) {
                final JsonObject item = array.getJsonObject(i);
                uiContext.fireClientData(item);
            }
        }
    }

    protected abstract EntryPoint initializeUIContext(final UIContext ponySession) throws ServletException;

    public ApplicationManagerOption getOptions() {
        return options;
    }

}
