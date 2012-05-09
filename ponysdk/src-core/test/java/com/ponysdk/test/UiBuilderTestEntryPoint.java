
package com.ponysdk.test;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class UiBuilderTestEntryPoint implements EntryPoint {

    private static NextTestClickHandler nextTestClickHandler = new NextTestClickHandler();

    public interface RequestHandler {

        public void onRequest();
    }

    private static class NextTestClickHandler implements PClickHandler {

        public RequestHandler handler;

        @Override
        public void onClick(final PClickEvent event) {
            handler.onRequest();
        }

    }

    public static void setRequestHandler(final RequestHandler handler) {
        nextTestClickHandler.handler = handler;
    }

    @Override
    public void start(final PonySession session) {
        doStart(session);
    }

    @Override
    public void restart(final PonySession session) {
        doStart(session);
    }

    private void doStart(final PonySession session) {
        final PAnchor test = new PAnchor("Go to next test");
        test.ensureDebugId("startingpoint");
        test.addClickHandler(nextTestClickHandler);

        PRootPanel.get().add(test);
    }

}
