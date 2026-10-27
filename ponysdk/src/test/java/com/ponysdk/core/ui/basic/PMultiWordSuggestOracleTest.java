/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PMultiWordSuggestOracleTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    @Test
    public void testInit() {
        assertEquals(WidgetType.MULTIWORD_SUGGEST_ORACLE, new PMultiWordSuggestOracle().getWidgetType());
    }

    @Test
    public void testAddAndClear() {
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.add("first");
        oracle.clear();
        // Stacked until the oracle is attached
        assertEquals(0, writer.count(ServerToClientModel.SUGGESTION));

        oracle.attach(PWindow.getMain(), null);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(oracle.getID()));
        assertEquals(List.of("first"), writer.valuesOf(ServerToClientModel.SUGGESTION));
        assertEquals(1, writer.count(ServerToClientModel.CLEAR));

        writer.clear();
        oracle.add("second");
        assertEquals(List.of("second"), writer.valuesOf(ServerToClientModel.SUGGESTION));
    }

    @Test
    public void testAddAllSendsEachSuggestion() {
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.attach(PWindow.getMain(), null);
        writer.clear();
        oracle.addAll(List.of("a", "b"));
        assertEquals(List.of("a", "b"), writer.valuesOf(ServerToClientModel.SUGGESTION));
    }

    @Test
    public void testDefaultSuggestionsAreNotSent() {
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.attach(PWindow.getMain(), null);
        writer.clear();
        oracle.setDefaultSuggestions(List.of("a", "b"));
        assertTrue(writer.isEmpty());
    }
}
