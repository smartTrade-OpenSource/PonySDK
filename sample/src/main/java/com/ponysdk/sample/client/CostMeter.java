/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.sample.client;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFrame;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;

/**
 * Measures what a component really costs, in the running showcase session:
 * <ul>
 * <li><b>server memory</b> retained by one instance (object graph walk, shared objects excluded),</li>
 * <li><b>creation</b> bytes per instance, as produced by the binary protocol and as sent on the wire,</li>
 * <li><b>update</b> bytes per instance for a typical change (setText, setValue...).</li>
 * </ul>
 * {@link #SAMPLE} instances are created in a hidden container, flushed, updated, flushed again, then removed.
 * Protocol bytes are exact (flush counter); wire bytes are the deltas of the connection counter over the
 * following {@link #SETTLE} and include permessage-deflate, which shrinks repeated instances a lot.
 */
final class CostMeter {

    static final int SAMPLE = 100;
    private static final Duration SETTLE = Duration.ofMillis(250);

    /** Result of one measurement (bytes per instance). */
    record Cost(String component, long memory, double createProtocol, double createWire, double updateProtocol,
                double updateWire, String updateLabel) {}

    private CostMeter() {
    }

    /**
     * Runs one measurement of {@link #SAMPLE} instances; {@code done} is called on the UI thread with the result.
     * Must be called inside a UIContext transaction (a click handler, a scheduled UIRunnable).
     */
    static void measure(final String component, final Supplier<? extends PWidget> factory, final Consumer<PWidget> update,
                        final String updateLabel, final Consumer<Cost> done) {
        measure(component, factory, update, updateLabel, SAMPLE, false, done);
    }

    /**
     * @param sample number of instances
     * @param async  for heavy components that render after creation (data grid rows, layout engines): the
     *               container is laid out off-screen instead of {@code display:none}, and the protocol bytes are
     *               counted over the whole settle window instead of the flush alone
     */
    static void measure(final String component, final Supplier<? extends PWidget> factory, final Consumer<PWidget> update,
                        final String updateLabel, final int sample, final boolean async, final Consumer<Cost> done) {
        final UIContext ctx = UIContext.get();
        final Duration settle = async ? Duration.ofMillis(900) : SETTLE;
        final PFlowPanel box = Element.newPFlowPanel();
        if (async) {
            box.setStyleProperty("position", "absolute");
            box.setStyleProperty("left", "-20000px");
            box.setStyleProperty("top", "0");
            box.setStyleProperty("width", "1000px");
            box.setStyleProperty("visibility", "hidden");
        } else {
            box.setStyleProperty("display", "none");
        }
        PWindow.getMain().add(box);
        Txn.get().flush(); // isolate the container itself

        final List<PWidget> widgets = new ArrayList<>(sample);
        for (int i = 0; i < sample; i++) {
            final PWidget w = factory.get();
            widgets.add(w);
            box.add(w);
        }
        final long memory = retainedSize(widgets.get(sample / 2));

        final long p0 = ctx.getProtocolBytesSent();
        final long w0 = ctx.getWireBytesSent();
        Txn.get().flush();
        final long createFlush = ctx.getProtocolBytesSent() - p0;

        PScheduler.schedule(() -> {
            final long w1 = ctx.getWireBytesSent();
            final long p1 = ctx.getProtocolBytesSent();
            final long createProtocol = async ? p1 - p0 : createFlush;
            if (update != null) widgets.forEach(update);
            Txn.get().flush();
            final long updateFlush = ctx.getProtocolBytesSent() - p1;
            PScheduler.schedule(() -> {
                final long w2 = ctx.getWireBytesSent();
                final long updateProtocol = async ? ctx.getProtocolBytesSent() - p1 : updateFlush;
                box.removeFromParent();
                final boolean wire = w0 >= 0;
                done.accept(new Cost(component, memory, (double) createProtocol / sample,
                    wire ? (double) (w1 - w0) / sample : -1, update != null ? (double) updateProtocol / sample : -1,
                    wire && update != null ? (double) (w2 - w1) / sample : -1, updateLabel));
            }, settle);
        }, settle);
    }

    // ── Retained-size estimate ───────────────────────────────────────────

    /** Approximate retained size of one widget (64-bit JVM, compressed oops: 12 B header, 4 B refs). */
    /** "80 B", "1.2 KB", "320 KB". */
    static String memory(final long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 10 * 1024) return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return Math.round(bytes / 1024.0) + " KB";
    }

    static long retainedSize(final PWidget widget) {
        return new Walker(widget).size(widget);
    }

    private static final class Walker {

        private final PWidget root;
        private final IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();

        Walker(final PWidget root) {
            this.root = root;
        }

        long size(final Object o) {
            if (o == null || seen.put(o, Boolean.TRUE) != null) return 0;
            if (isShared(o)) return 0;
            final Class<?> c = o.getClass();
            if (o instanceof String s) return s == s.intern() ? 0 : 24 + align(16 + s.length());
            if (c.isArray()) return arraySize(o);
            if (c.getName().startsWith("java.")) return jdkSize(o);
            long shallow = 12, deep = 0;
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                for (final Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType().isPrimitive()) {
                        shallow += primitiveSize(f.getType());
                        continue;
                    }
                    shallow += 4;
                    try {
                        f.setAccessible(true);
                        deep += size(f.get(o));
                    } catch (final RuntimeException | IllegalAccessException ignored) {
                        // inaccessible: counted as a reference only
                    }
                }
            }
            return align(shallow) + deep;
        }

        /** Objects that belong to the session, the page or a shared pool: not part of this instance's cost. */
        private boolean isShared(final Object o) {
            if (o instanceof PObject p && p != root && !isOwnedChild(p)) return true;
            return o instanceof UIContext || o instanceof PWindow || o instanceof PFrame || o instanceof Class
                || o instanceof Thread || o instanceof ClassLoader || o instanceof Enum
                || o.getClass().getName().startsWith("com.ponysdk.core.util.SetPool")
                || o.getClass().getName().startsWith("com.ponysdk.core.server.")
                || o.getClass().getName().startsWith("org.");
        }

        /**
         * Sub-widgets created by the component itself are part of its cost: its children, and helper widgets it
         * holds without attaching them anywhere (e.g. the picker of a date box).
         */
        private boolean isOwnedChild(final PObject p) {
            if (p instanceof com.ponysdk.core.ui.basic.PAddOn) return true; // add-ons are per widget
            if (!(p instanceof PWidget w)) return false;
            if (w.getParent() == null) return true;
            for (com.ponysdk.core.ui.basic.IsPWidget parent = w.getParent(); parent != null;
                 parent = parent.asWidget().getParent()) {
                if (parent.asWidget() == root) return true;
            }
            return false;
        }

        private long arraySize(final Object a) {
            final int n = Array.getLength(a);
            final Class<?> t = a.getClass().getComponentType();
            if (t.isPrimitive()) return align(16L + (long) n * primitiveSize(t));
            long s = align(16L + 4L * n);
            for (int i = 0; i < n; i++) s += size(Array.get(a, i));
            return s;
        }

        /** JDK classes are closed to reflection: estimate collections from their content, others from their kind. */
        private long jdkSize(final Object o) {
            if (o instanceof Collection<?> col) {
                long s = 40 + align(16L + 4L * Math.max(col.size(), 10));
                for (final Object e : col) s += size(e) + (o instanceof java.util.Set ? 32 : 0);
                return s;
            }
            if (o instanceof Map<?, ?> m) {
                long s = 48 + align(16L + 4L * Math.max(16, m.size() * 2));
                for (final Map.Entry<?, ?> e : m.entrySet()) s += 32 + size(e.getKey()) + size(e.getValue());
                return s;
            }
            if (o instanceof Number || o instanceof Boolean || o instanceof Character) return 16;
            if (o.getClass().isSynthetic() || o.getClass().getName().contains("$$Lambda")) return 16;
            return 24;
        }

        private static int primitiveSize(final Class<?> t) {
            return t == long.class || t == double.class ? 8 : t == int.class || t == float.class ? 4
                : t == short.class || t == char.class ? 2 : 1;
        }

        private static long align(final long n) {
            return (n + 7) & ~7L;
        }
    }
}
