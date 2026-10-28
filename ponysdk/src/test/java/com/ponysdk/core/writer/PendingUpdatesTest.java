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

package com.ponysdk.core.writer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.websocket.WebsocketEncoder;

/** The pending-update buffer on its own: ordering, run markers, sorting paths and the reuse of its arrays. */
public class PendingUpdatesTest {

    /** Records blocks of "MODEL=value". */
    private static final class Wire implements WebsocketEncoder {

        final List<List<String>> blocks = new ArrayList<>();
        private List<String> open;
        private final int version;

        Wire(final int version) {
            this.version = version;
        }

        @Override
        public int protocolVersion() {
            return version;
        }

        @Override
        public void beginObject() {
        }

        @Override
        public void encode(final ServerToClientModel model, final Object value) {
            if (open == null) blocks.add(open = new ArrayList<>());
            open.add(model + "=" + value);
        }

        @Override
        public void endObject() {
            open = null;
        }
    }

    private static final class Target implements UpdateTarget {

        private final int id;
        private final Object group;
        boolean updatable = true;
        boolean begins = true;

        Target(final int id, final Object group) {
            this.id = id;
            this.group = group;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public Object updateGroup() {
            return group;
        }

        @Override
        public boolean isUpdatable() {
            return updatable;
        }

        @Override
        public boolean beginCoalescedUpdate(final ModelWriter writer) {
            if (!begins) return false;
            writer.writeIntUnchecked(ServerToClientModel.TYPE_UPDATE, id);
            return true;
        }
    }

    private static final Object WINDOW = new Object();

    private static void text(final PendingUpdates pending, final Target target, final String text) {
        pending.put(target, ServerToClientModel.TEXT, null, null, text);
    }

    private static List<List<String>> drain(final PendingUpdates pending, final int version) {
        final Wire wire = new Wire(version);
        pending.drainTo(new ModelWriter(wire));
        assertTrue(pending.isEmpty());
        return wire.blocks;
    }

    @Test
    public void lastValueWinsAndMovesToTheEnd() {
        final PendingUpdates pending = new PendingUpdates();
        final Target a = new Target(1, WINDOW);
        final Target b = new Target(2, WINDOW);
        text(pending, a, "1");
        text(pending, b, "x");
        text(pending, a, "2");
        pending.put(a, ServerToClientModel.PUT_STYLE_KEY, "color", ServerToClientModel.STYLE_VALUE, "red");
        pending.put(a, ServerToClientModel.ADD_STYLE_NAME, "k", null, null);
        assertEquals(4, pending.size());

        assertEquals(List.of(List.of("TYPE_UPDATE=2", "TEXT=x"),
            List.of("TYPE_UPDATE=1", "TEXT=2", "PUT_STYLE_KEY=color", "STYLE_VALUE=red", "ADD_STYLE_NAME=k")),
            drain(pending, ProtocolVersion.V1));
    }

    @Test
    public void destroyedTargetsAreSkipped() {
        final PendingUpdates pending = new PendingUpdates();
        final Target destroyed = new Target(1, WINDOW);
        final Target refusing = new Target(2, WINDOW);
        final Target alive = new Target(3, WINDOW);
        text(pending, destroyed, "a");
        text(pending, refusing, "b");
        text(pending, refusing, "b2");
        pending.put(refusing, ServerToClientModel.ADD_STYLE_NAME, "c", null, null);
        text(pending, alive, "c");
        destroyed.updatable = false;
        refusing.begins = false;

        assertEquals(List.of(List.of("TYPE_UPDATE=3", "TEXT=c")), drain(pending, ProtocolVersion.V1));
    }

    @Test
    public void runsChainObjectsInIdOrder() {
        final PendingUpdates pending = new PendingUpdates();
        for (final int id : new int[] { 9, 5, 300, 6, 7 }) {
            text(pending, new Target(id, WINDOW), "t" + id);
        }
        assertEquals(List.of(List.of("TYPE_UPDATE=5", "TEXT=t5", //
            "UPDATE_NEXT=null", "TEXT=t6", //
            "UPDATE_NEXT=null", "TEXT=t7", //
            "UPDATE_NEXT_DELTA=0", "TEXT=t9", //
            "UPDATE_NEXT_ID=300", "TEXT=t300")), drain(pending, ProtocolVersion.UPDATE_RUNS));
    }

    @Test
    public void eachGroupHasItsOwnBlockInOrderOfAppearance() {
        final PendingUpdates pending = new PendingUpdates();
        final Object other = new Object();
        text(pending, new Target(3, WINDOW), "a3");
        text(pending, new Target(2, other), "b2");
        text(pending, new Target(1, WINDOW), "a1");

        assertEquals(List.of(List.of("TYPE_UPDATE=1", "TEXT=a1", "UPDATE_NEXT_DELTA=0", "TEXT=a3"), List.of("TYPE_UPDATE=2", "TEXT=b2")),
            drain(pending, ProtocolVersion.UPDATE_RUNS));
    }

    @Test
    public void manyGroupsAndSparseIdsAreSortedByComparison() {
        final PendingUpdates pending = new PendingUpdates();
        final List<List<String>> expected = new ArrayList<>();
        for (int g = 0; g < 6; g++) {
            final Object group = new Object();
            text(pending, new Target(1_000_000 + g, group), "far");
            text(pending, new Target(g, group), "near");
            expected.add(List.of("TYPE_UPDATE=" + g, "TEXT=near", "UPDATE_NEXT_ID=" + (1_000_000 + g), "TEXT=far"));
        }
        assertEquals(expected, drain(pending, ProtocolVersion.UPDATE_RUNS));

        text(pending, new Target(1, WINDOW), "a");
        text(pending, new Target(100_000, WINDOW), "b");
        assertEquals(List.of(List.of("TYPE_UPDATE=1", "TEXT=a", "UPDATE_NEXT_ID=100000", "TEXT=b")),
            drain(pending, ProtocolVersion.UPDATE_RUNS));
    }

    @Test
    public void aReplacedSlotIsSkippedWhenSortingByComparison() {
        final PendingUpdates pending = new PendingUpdates();
        final Object other = new Object();
        final Target a = new Target(1, WINDOW);
        text(pending, a, "x");
        text(pending, new Target(2, other), "y");
        text(pending, a, "x2");

        assertEquals(List.of(List.of("TYPE_UPDATE=2", "TEXT=y"), List.of("TYPE_UPDATE=1", "TEXT=x2")),
            drain(pending, ProtocolVersion.UPDATE_RUNS));
    }

    @Test
    public void beyond255GroupsTheSlotOrderIsKept() {
        final PendingUpdates pending = new PendingUpdates();
        final List<List<String>> expected = new ArrayList<>();
        for (int g = 0; g < 256; g++) {
            final int id = 1000 - g;
            text(pending, new Target(id, new Object()), "t");
            expected.add(List.of("TYPE_UPDATE=" + id, "TEXT=t"));
        }
        assertEquals(expected, drain(pending, ProtocolVersion.UPDATE_RUNS));
    }

    @Test
    public void beyond255GroupsARunGoingBackwardsNamesTheNextId() {
        final PendingUpdates pending = new PendingUpdates();
        text(pending, new Target(10, WINDOW), "a");
        text(pending, new Target(5, WINDOW), "b");
        for (int g = 0; g < 255; g++) text(pending, new Target(100 + g, new Object()), "t");

        final List<List<String>> blocks = drain(pending, ProtocolVersion.UPDATE_RUNS);

        assertEquals(256, blocks.size());
        assertEquals(List.of("TYPE_UPDATE=10", "TEXT=a", "UPDATE_NEXT_ID=5", "TEXT=b"), blocks.get(0));
    }

    @Test
    public void anEmptyBufferDrainsNothingForARunTerminal() {
        assertEquals(List.of(), drain(new PendingUpdates(), ProtocolVersion.UPDATE_RUNS));
    }

    @Test
    public void aLongRunIsSplitIntoBlocks() {
        final PendingUpdates pending = new PendingUpdates();
        final Target first = new Target(1, WINDOW);
        text(pending, first, "old");
        for (int id = 1; id <= PendingUpdates.MAX_RUN + 1; id++) {
            text(pending, id == 1 ? first : new Target(id, WINDOW), "t" + id);
        }
        assertEquals(PendingUpdates.MAX_RUN + 1, pending.size());

        final List<List<String>> blocks = drain(pending, ProtocolVersion.UPDATE_RUNS);

        assertEquals(2, blocks.size());
        assertEquals(List.of("TYPE_UPDATE=1", "TEXT=t1", "UPDATE_NEXT=null", "TEXT=t2"), blocks.get(0).subList(0, 4));
        assertEquals(2 + 2 * (PendingUpdates.MAX_RUN - 1), blocks.get(0).size());
        assertEquals(List.of("TYPE_UPDATE=" + (PendingUpdates.MAX_RUN + 1), "TEXT=t" + (PendingUpdates.MAX_RUN + 1)), blocks.get(1));
    }

    @Test
    public void theIndexStaysRightAfterABurstAndASmallDrain() {
        final PendingUpdates pending = new PendingUpdates();
        final List<Target> targets = new ArrayList<>();
        for (int id = 0; id < 5_000; id++) {
            final Target t = new Target(id, WINDOW);
            targets.add(t);
            text(pending, t, "v");
        }
        assertTrue(pending.isOversized());
        assertEquals(5_000, drain(pending, ProtocolVersion.V1).size());

        // A small drain clears only its own index entries: replacement must still find them afterwards
        final Target t = targets.get(7);
        text(pending, t, "a");
        text(pending, t, "b");
        assertEquals(List.of(List.of("TYPE_UPDATE=7", "TEXT=b")), drain(pending, ProtocolVersion.V1));
        text(pending, t, "c");
        text(pending, t, "d");
        assertEquals(1, pending.size());
        assertEquals(List.of(List.of("TYPE_UPDATE=7", "TEXT=d")), drain(pending, ProtocolVersion.V1));
    }

    @Test
    public void discardDropsEverything() {
        final PendingUpdates pending = new PendingUpdates();
        final Target t = new Target(1, WINDOW);
        text(pending, t, "a");
        text(pending, t, "b");
        pending.discard();
        assertTrue(pending.isEmpty());
        assertEquals(List.of(), drain(pending, ProtocolVersion.V1));
        text(pending, t, "c");
        assertEquals(List.of(List.of("TYPE_UPDATE=1", "TEXT=c")), drain(pending, ProtocolVersion.V1));
    }
}
