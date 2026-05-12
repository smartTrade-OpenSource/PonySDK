/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.test.inspector;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import javax.json.Json;
import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.ui.basic.PAddOn;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollAddon;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollProvider;

/**
 * Specialized inspector that simulates the client-side render/scroll events an
 * {@link InfiniteScrollAddon} would receive from the browser, server-side and without any
 * real terminal round trip.
 *
 * <h2>Why this exists</h2>
 * <p>The real {@code InfiniteScrollAddon} waits for its JavaScript companion to push a
 * {@code {beginIndex, maxVisibleItem}} message before it fetches and renders rows. Tests
 * that assert on the rendered content therefore have to stand in for that push. This
 * inspector replays the same path - building the {@code NATIVE} envelope and dispatching
 * it through {@link PAddOn#onClientData(JsonObject)} - so the addon's terminal handler
 * runs exactly as it would in production.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * InspectorWidget panel = InspectorWidget.of(myPanel);
 * InspectorInfiniteScroll scroll = panel.find(InspectorInfiniteScroll.FACTORY);
 * scroll.simulateRender(0, 20); // render the first 20 items
 * scroll.simulateFullRender();  // render everything the provider knows about
 * }</pre>
 *
 * <h2>Lookup contract</h2>
 * <p>{@link #FACTORY} matches any widget that has an {@code InfiniteScrollAddon} bound to
 * it. Detection is done by simple name so the testkit does not force callers to put the
 * {@code InfiniteScrollAddon} class on the classpath when they do not need this
 * inspector.</p>
 */
public class InspectorInfiniteScroll extends InspectorWidget {

    /** JSON key the {@link InfiniteScrollAddon} terminal handler expects for the begin index. */
    private static final String KEY_BEGIN_INDEX = "beginIndex";

    /** JSON key the {@link InfiniteScrollAddon} terminal handler expects for the page size. */
    private static final String KEY_MAX_VISIBLE_ITEM = "maxVisibleItem";

    /** Simple name of the addon the base predicate scans for. */
    private static final String INFINITE_SCROLL_ADDON_SIMPLE_NAME = "InfiniteScrollAddon";

    /**
     * Factory matching widgets that carry an {@link InfiniteScrollAddon} in their addon set.
     * <p>Matching is done by simple class name on {@code PWidget.getAddons()} which keeps the
     * predicate resilient to callers using their own subclass of the addon.</p>
     */
    public static final InspectorFactory<InspectorInfiniteScroll> FACTORY = new InspectorFactory<InspectorInfiniteScroll>() {

        @Override
        public Predicate<PWidget> basePredicate() {
            return new Predicate<PWidget>() {
                @Override
                public boolean test(final PWidget w) {
                    if (w == null) return false;
                    final Set<PAddOn> addons = w.getAddons();
                    if (addons == null || addons.isEmpty()) return false;
                    for (final PAddOn addon : addons) {
                        if (addon != null
                                && INFINITE_SCROLL_ADDON_SIMPLE_NAME.equals(addon.getClass().getSimpleName())) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return "hasAddon(InfiniteScrollAddon)";
                }
            };
        }

        @Override
        public InspectorInfiniteScroll create(final PWidget widget) {
            return new InspectorInfiniteScroll(widget);
        }
    };

    /**
     * Wraps the given widget.
     *
     * @param widget the widget hosting an {@link InfiniteScrollAddon}; must not be {@code null}
     * @throws NullPointerException if {@code widget} is {@code null}
     */
    public InspectorInfiniteScroll(final PWidget widget) {
        super(widget);
    }

    /**
     * Simulates the JavaScript client pushing a render request for the given window.
     * <p>
     * Builds {@code {"4": {"beginIndex": beginIndex, "maxVisibleItem": maxSize}}} - the exact
     * envelope the browser sends through {@code pony.sendDataToServer} - and dispatches it
     * through {@link PAddOn#onClientData(JsonObject)} on the bound
     * {@link InfiniteScrollAddon}. The addon then calls
     * {@link InfiniteScrollProvider#getData(int, int, java.util.function.Consumer)} and
     * renders the returned rows.
     * </p>
     *
     * @param beginIndex the first row index to render
     * @param maxSize    the number of rows to request
     * @throws IllegalArgumentException if either argument is negative
     * @throws IllegalStateException    if the widget no longer has an {@code InfiniteScrollAddon}
     */
    public void simulateRender(final int beginIndex, final int maxSize) {
        if (beginIndex < 0) throw new IllegalArgumentException("beginIndex must be >= 0, got " + beginIndex);
        if (maxSize < 0) throw new IllegalArgumentException("maxSize must be >= 0, got " + maxSize);

        final PAddOn addon = requireInfiniteScrollAddon();
        final JsonObject payload = Json.createObjectBuilder()
                .add(KEY_BEGIN_INDEX, beginIndex)
                .add(KEY_MAX_VISIBLE_ITEM, maxSize)
                .build();
        final JsonObject envelope = Json.createObjectBuilder()
                .add(ClientToServerModel.NATIVE.toStringValue(), payload)
                .build();
        addon.onClientData(envelope);
    }

    /**
     * Simulates a full render: asks the addon's data provider for the total row count and
     * then calls {@link #simulateRender(int, int)} with {@code beginIndex = 0} and
     * {@code maxSize} equal to that count.
     * <p>
     * The total row count is retrieved by calling
     * {@link InfiniteScrollProvider#getFullSize(IntConsumer)} exactly like the addon itself
     * does - this is the cleanest way to stand in for the JS client which normally drives
     * the "render everything" path on dropdown open. The data provider is read by reflection
     * because it is a private field of {@code InfiniteScrollAddon}; the simple-name check on
     * the addon and the reflective access stay colocated in this method so the rest of the
     * testkit keeps its reflection-free posture.
     * </p>
     * <p>
     * If the provider fires its callback synchronously (the common case), the captured size
     * is used immediately; if it never fires, {@code simulateFullRender} falls back to
     * {@link InfiniteScrollAddon#getFullSize()} which holds the last value the addon saw.
     * </p>
     *
     * @throws IllegalStateException if the widget no longer has an {@code InfiniteScrollAddon}
     *                               or if the data provider field cannot be read
     */
    public void simulateFullRender() {
        final PAddOn addon = requireInfiniteScrollAddon();
        final int fullSize = resolveFullSize(addon);
        simulateRender(0, fullSize);
    }

    // =====================================================================================
    // Internals
    // =====================================================================================

    /**
     * Returns the first addon bound to the wrapped widget whose simple class name matches
     * {@value #INFINITE_SCROLL_ADDON_SIMPLE_NAME}.
     */
    private PAddOn requireInfiniteScrollAddon() {
        final Set<PAddOn> addons = widget.getAddons();
        if (addons != null) {
            for (final PAddOn addon : addons) {
                if (addon != null
                        && INFINITE_SCROLL_ADDON_SIMPLE_NAME.equals(addon.getClass().getSimpleName())) {
                    return addon;
                }
            }
        }
        throw new IllegalStateException("Widget has no InfiniteScrollAddon: " + widget);
    }

    /**
     * Best-effort resolution of the provider's full size.
     * <p>
     * Walks up the class hierarchy to find the {@code dataProvider} field declared on
     * {@link InfiniteScrollAddon}, invokes
     * {@link InfiniteScrollProvider#getFullSize(IntConsumer)} and captures the value the
     * provider reports. Falls back to {@link InfiniteScrollAddon#getFullSize()} when the
     * provider does not invoke its callback synchronously.
     * </p>
     */
    private static int resolveFullSize(final PAddOn addon) {
        final AtomicInteger holder = new AtomicInteger(-1);

        final Field providerField = findFieldIgnoreCase(addon.getClass(), "dataProvider");
        if (providerField != null) {
            try {
                providerField.setAccessible(true);
                final Object provider = providerField.get(addon);
                if (provider instanceof InfiniteScrollProvider) {
                    ((InfiniteScrollProvider<?, ?>) provider).getFullSize((IntConsumer) holder::set);
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Cannot read InfiniteScrollAddon.dataProvider", e);
            }
        }

        if (holder.get() >= 0) return holder.get();
        if (addon instanceof InfiniteScrollAddon) {
            return ((InfiniteScrollAddon<?, ?>) addon).getFullSize();
        }
        throw new IllegalStateException(
                "Unable to determine full size: InfiniteScrollProvider did not answer synchronously and addon is not an InfiniteScrollAddon instance");
    }

    private static Field findFieldIgnoreCase(Class<?> type, final String name) {
        while (type != null && type != Object.class) {
            for (final Field field : type.getDeclaredFields()) {
                if (name.equals(field.getName())) return field;
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
