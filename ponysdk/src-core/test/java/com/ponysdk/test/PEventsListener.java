
package com.ponysdk.test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.BusinessEvent;
import com.ponysdk.core.event.Event;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionEvent;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionHandler;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PBlurHandler;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PFocusHandler;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverHandler;
import com.ponysdk.ui.server.basic.event.POpenEvent;
import com.ponysdk.ui.server.basic.event.POpenHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

@SuppressWarnings("rawtypes")
public class PEventsListener implements PValueChangeHandler, PCloseHandler, POpenHandler, PBlurHandler, PFocusHandler, PKeyPressHandler, PKeyUpHandler, PMouseOverHandler, PChangeHandler, PSelectionHandler, PBeforeSelectionHandler {

    private static Logger log = LoggerFactory.getLogger(PEventsListener.class);

    private final BlockingQueue<Event<?>> eventQueue = new ArrayBlockingQueue<>(1000);

    @Override
    public void onValueChange(final PValueChangeEvent event) {
        log.info("onValueChange #" + event.getValue());
        eventQueue.add(event);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event<?>> T poll() {
        Event<?> event2 = null;
        try {
            event2 = eventQueue.poll(2000, TimeUnit.MILLISECONDS);
            if (event2 == null) throw new RuntimeException("No event received");
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        return (T) event2;
    }

    public void clear() {
        eventQueue.clear();
    }

    @Override
    public void onClose(final PCloseEvent closeEvent) {
        log.info("onClose");
        eventQueue.add(closeEvent);
    }

    @Override
    public void onOpen(final POpenEvent openEvent) {
        log.info("onOpen");
        eventQueue.add(openEvent);
    }

    @Override
    public void onMouseOver(final PMouseOverEvent mouseOverEvent) {
        log.info("onMouseOver");
        eventQueue.add(mouseOverEvent);
    }

    @Override
    public void onKeyUp(final PKeyUpEvent keyUpEvent) {
        log.info("onKeyUp");
        eventQueue.add(keyUpEvent);
    }

    @Override
    public void onKeyPress(final PKeyPressEvent keyPressEvent) {
        log.info("onKeyPress");
        eventQueue.add(keyPressEvent);
    }

    @Override
    public void onFocus(final PFocusEvent event) {
        log.info("onFocus");
        eventQueue.add(event);
    }

    @Override
    public void onBlur(final PBlurEvent event) {
        log.info("onBlur");
        eventQueue.add(event);
    }

    @Override
    public void onChange(final PChangeEvent event) {
        log.info("onChange");
        eventQueue.add(event);
    }

    public void stackCommandResult(final BusinessEvent e) {
        log.info("stackCommandResult");
        eventQueue.add(e);
    }

    @Override
    public void onSelection(final PSelectionEvent event) {
        log.info("onSelection");
        eventQueue.add(event);
    }

    @Override
    public void onBeforeSelection(final PBeforeSelectionEvent event) {
        log.info("onBeforeSelection");
        eventQueue.add(event);
    }

}
