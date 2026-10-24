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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ponysdk.core.model.PUnit;

import org.junit.jupiter.api.Test;
import com.ponysdk.test.PSuite;

public class ChildBeforeAttachTest extends PSuite {

    @Test
    public void childrenAddedBeforeAttachAreCreatedWithThePanel() {
        final PStackLayoutPanel stack = Element.newPStackLayoutPanel(PUnit.PX);
        final PLabel first = Element.newPLabel("first");
        final PLabel second = Element.newPLabel("second");
        stack.add(first, "First", false, 30);
        stack.add(second, "Second", false, 30);
        assertFalse(first.isInitialized());

        PWindow.getMain().add(stack);

        assertTrue(stack.isInitialized());
        assertTrue(first.isInitialized(), "child created when the panel is attached");
        assertTrue(second.isInitialized());
    }

    @Test
    public void childrenAddedAfterAttachAreCreatedAtOnce() {
        final PStackLayoutPanel stack = Element.newPStackLayoutPanel(PUnit.PX);
        PWindow.getMain().add(stack);
        final PLabel child = Element.newPLabel("late");
        stack.add(child, "Late", false, 30);
        assertTrue(child.isInitialized());
    }

    /** A composite's WIDGET_ID names its widget: the widget must exist on the terminal first. */
    @Test
    public void theWidgetOfACompositeIsCreatedBeforeTheComposite() {
        final PLabel inner = Element.newPLabel("inner");
        final PComposite<PLabel> composite = composite();
        composite.initWidget(inner);
        assertFalse(inner.isInitialized());
        PWindow.getMain().add(composite);
        assertTrue(inner.isInitialized(), "created with the composite");

        final PComposite<PLabel> late = composite();
        PWindow.getMain().add(late);
        final PLabel lateInner = Element.newPLabel("late inner");
        late.initWidget(lateInner);
        assertTrue(lateInner.isInitialized(), "wrapped once the composite is attached");
    }

    private static PComposite<PLabel> composite() {
        return new PComposite<>() {

            @Override
            public String dumpDOM() {
                return "";
            }
        };
    }

    @Test
    public void disclosureContentSetBeforeAttachIsCreatedWithThePanel() {
        final PDisclosurePanel disclosure = Element.newPDisclosurePanel("More");
        final PLabel content = Element.newPLabel("hidden content");
        disclosure.setContent(content);
        assertFalse(content.isInitialized());

        PWindow.getMain().add(disclosure);

        assertTrue(content.isInitialized(), "content created when the panel is attached");
    }

    @Test
    public void itemOfASubtreeAddedLaterCanBeSelected() {
        final PTree tree = Element.newPTree();
        final PTreeItem folder = Element.newPTreeItem("folder");
        final PTreeItem leaf = folder.add(Element.newPTreeItem("leaf"));
        assertFalse(leaf.isSelected(), "not in a tree yet");

        tree.add(folder);
        leaf.setSelected(true);

        assertTrue(leaf.isSelected());
    }
}
