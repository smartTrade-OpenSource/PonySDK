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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PFunctionalLabelTest extends PSuite {

    private static final TextFunction SUM = TextFunction.named(args -> "sum=" + ((int) args[0] + (int) args[1]), "sum");
    private static final TextFunction UPPER = TextFunction.named(args -> String.valueOf(args[0]).toUpperCase(), "upper");

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    @Test
    public void theTextIsComputedServerSideFromTheArguments() {
        final PFunctionalLabel label = Element.newPFunctionalLabel(SUM, 1, 2);
        assertEquals(WidgetType.FUNCTIONAL_LABEL, label.getWidgetType());
        assertSame(SUM, label.getTextFunction());
        assertArrayEquals(new Object[] { 1, 2 }, label.getArgs());
        assertEquals("sum=3", label.getText());
        assertTrue(label.toString().contains("args=[1, 2]"));

        assertNull(Element.newPFunctionalLabel(SUM).getText());
    }

    @Test
    public void attachCreatesTheFunctionOnceAndSendsItsIdWithTheArguments() {
        final TextFunction product = TextFunction.named(args -> "p", "product");
        final PFunctionalLabel first = Element.newPFunctionalLabel(product, 1, 2);
        final PFunctionalLabel second = Element.newPFunctionalLabel(product);
        PWindow.getMain().add(first);
        PWindow.getMain().add(second);

        final PFunction function = PWindow.getMain().getPFunction(product);
        assertEquals(List.of("product"), writer.valuesOf(ServerToClientModel.FUNCTION_NAME));
        assertEquals(List.of(function.getID(), function.getID()), writer.valuesOf(ServerToClientModel.FUNCTION_ID));
        assertEquals(List.of("[1, 2]"), writer.frames().stream().filter(f -> f.startsWith("FUNCTION_ARGS="))
            .map(f -> f.substring("FUNCTION_ARGS=".length())).toList());
    }

    @Test
    public void argumentsAndFunctionChangesAreSentAfterAttach() {
        final PFunctionalLabel label = Element.newPFunctionalLabel(SUM, 1, 2);
        PWindow.getMain().add(label);
        writer.clear();

        label.setArgs(1, 2);
        assertEquals(0, writer.count(ServerToClientModel.FUNCTION_ARGS));
        label.setArgs(3, 4);
        assertEquals(1, writer.count(ServerToClientModel.FUNCTION_ARGS));
        assertEquals("sum=7", label.getText());

        label.setTextFunction(SUM);
        assertEquals(0, writer.count(ServerToClientModel.FUNCTION_ID));
        label.setTextFunction(UPPER);
        assertEquals(List.of(PWindow.getMain().getPFunction(UPPER).getID()), writer.valuesOf(ServerToClientModel.FUNCTION_ID));
        assertEquals("3", label.getText());
    }

    @Test
    public void aPlainTextReplacesTheArguments() {
        final PFunctionalLabel label = Element.newPFunctionalLabel(SUM, 1, 2);
        label.setText("fixed");
        assertNull(label.getArgs());
        assertEquals("fixed", label.getText());

        label.setArgs(5, 5);
        assertEquals("sum=10", label.getText());
    }

    @Test
    public void aFunctionChangeBeforeAttachIsUsedAtAttach() {
        final PFunctionalLabel label = Element.newPFunctionalLabel(SUM, "a");
        label.setTextFunction(UPPER);
        assertTrue(writer.isEmpty());
        assertEquals("A", label.getText());

        PWindow.getMain().add(label);
        assertEquals(List.of(PWindow.getMain().getPFunction(UPPER).getID()), writer.valuesOf(ServerToClientModel.FUNCTION_ID));
    }

}
