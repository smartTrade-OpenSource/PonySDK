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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ponysdk.core.model.PUnit;
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
        assertTrue("child created when the panel is attached", first.isInitialized());
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

    @Test
    public void disclosureContentSetBeforeAttachIsCreatedWithThePanel() {
        final PDisclosurePanel disclosure = Element.newPDisclosurePanel("More");
        final PLabel content = Element.newPLabel("hidden content");
        disclosure.setContent(content);
        assertFalse(content.isInitialized());

        PWindow.getMain().add(disclosure);

        assertTrue("content created when the panel is attached", content.isInitialized());
    }

    @Test
    public void itemOfASubtreeAddedLaterCanBeSelected() {
        final PTree tree = Element.newPTree();
        final PTreeItem folder = Element.newPTreeItem("folder");
        final PTreeItem leaf = folder.add(Element.newPTreeItem("leaf"));
        assertFalse("not in a tree yet", leaf.isSelected());

        tree.add(folder);
        leaf.setSelected(true);

        assertTrue(leaf.isSelected());
    }
}
