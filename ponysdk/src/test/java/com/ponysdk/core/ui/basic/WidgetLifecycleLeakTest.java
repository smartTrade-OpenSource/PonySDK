/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;

import com.ponysdk.test.PSuite;

import org.junit.jupiter.api.Test;

/** Objects whose life ended must not stay reachable from longer-lived ones (window, parent, listeners). */
public class WidgetLifecycleLeakTest extends PSuite {

    static boolean collected(final WeakReference<?> ref) {
        for (int i = 0; i < 20 && ref.get() != null; i++) {
            System.gc();
            try {
                Thread.sleep(20);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return ref.get() == null;
    }

    @Test
    public void destroyingATableDestroysTheWidgetsOfItsCells() {
        final PFlexTable table = Element.newPFlexTable();
        PWindow.getMain().add(table);
        final PLabel label = Element.newPLabel("cell");
        table.setWidget(1, 2, label);
        final int[] destroyed = {0};
        label.addDestroyListener(o -> destroyed[0]++);

        table.onDestroy();

        assertEquals(1, destroyed[0]);
    }

    @Test
    public void aWidgetAddedToATableBeforeAttachIsCreated() {
        final PFlexTable table = Element.newPFlexTable();
        final PLabel label = Element.newPLabel("cell");
        table.setWidget(0, 0, label);

        PWindow.getMain().add(table);

        assertNotNull(label.getWindow(), "attached with its table");
    }

    @Test
    public void initializeListenersAreReleasedOnceFired() {
        final PLabel label = Element.newPLabel("x");
        PObject.InitializeListener listener = new PObject.InitializeListener() {
            @Override
            public void onInitialize(final PObject object) {
            }
        };
        label.addInitializeListener(listener);
        PWindow.getMain().add(label);
        final WeakReference<PObject.InitializeListener> ref = new WeakReference<>(listener);
        listener = null;

        assertTrue(collected(ref), "released by the initialized widget");
        assertNotNull(label.getWindow());
    }
}
