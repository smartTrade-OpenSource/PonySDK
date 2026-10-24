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

package com.ponysdk.core.server.stm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link Txn}: per-transaction lifecycle and (lazily created) listeners. */
public class TxnTest {

    @Test
    public void commitWithoutListenersFlushesAndEndsTheTransaction() {
        final TxnContext context = Mockito.mock(TxnContext.class);
        final Txn txn = Txn.get();
        assertSame(txn, Txn.get(), "same transaction while it is open");
        txn.begin(context);
        txn.commit();
        Mockito.verify(context).flush();
        assertThrows(RuntimeException.class, () -> Txn.get().commit(), "commit ends the transaction");
        Txn.get().begin(context);
        Txn.get().rollback(); // cleanup + rollback without listeners
    }

    @Test
    public void listenersAreFiredInOrderOfThePhase() {
        final TxnContext context = Mockito.mock(TxnContext.class);
        final List<String> calls = new ArrayList<>();
        final Txn txn = Txn.get();
        txn.addTxnListener(new TxnListener() {
            @Override public void beforeFlush(final TxnContext c) { calls.add("before"); }
            @Override public void afterFlush(final TxnContext c) { calls.add("after"); }
            @Override public void beforeRollback() { calls.add("rollback"); }
        });
        txn.begin(context);
        txn.commit();
        assertEquals(List.of("before", "after"), calls);
    }

    @Test
    public void listenersDoNotLeakIntoTheNextTransaction() {
        final TxnContext context = Mockito.mock(TxnContext.class);
        final List<String> calls = new ArrayList<>();
        final Txn first = Txn.get();
        first.addTxnListener(new TxnListener() {
            @Override public void beforeFlush(final TxnContext c) {}
            @Override public void afterFlush(final TxnContext c) {}
            @Override public void beforeRollback() { calls.add("rollback"); }
        });
        first.begin(context);
        first.rollback();
        assertEquals(List.of("rollback"), calls);

        final Txn second = Txn.get();
        second.begin(context);
        second.rollback();
        assertEquals(List.of("rollback"), calls, "the second transaction has no listener");
    }

    @Test
    public void aListenerMayRegisterAnotherWhileFiring() {
        final TxnContext context = Mockito.mock(TxnContext.class);
        final Txn txn = Txn.get();
        txn.addTxnListener(new TxnListener() {
            @Override public void beforeFlush(final TxnContext c) {
                txn.addTxnListener(new TxnListener() {
                    @Override public void beforeFlush(final TxnContext c2) {}
                    @Override public void afterFlush(final TxnContext c2) {}
                    @Override public void beforeRollback() {}
                });
            }
            @Override public void afterFlush(final TxnContext c) {}
            @Override public void beforeRollback() {}
        });
        txn.begin(context);
        txn.commit(); // must not throw ConcurrentModificationException
    }
}
