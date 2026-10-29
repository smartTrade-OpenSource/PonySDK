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

package com.ponysdk.core.ui.rich;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class RichWidgetsTest extends PSuite {

    private CapturingModelWriter writer;

    @AfterEach
    public void tearDown() {
        if (writer != null) writer.uninstall();
    }

    @Test
    public void testBreadCrumbs() {
        final PBreadCrumbs breadCrumbs = new PBreadCrumbs();
        assertEquals("ul", breadCrumbs.getTagName());
        assertTrue(breadCrumbs.hasStyleName("pony-BreadCrumbs"));

        breadCrumbs.addItem("home");
        // A single item cannot be selected away
        breadCrumbs.selectItem("home");
        breadCrumbs.addItem("home");
        assertEquals(1, breadCrumbs.getWidgetCount());

        breadCrumbs.addItem("folder");
        breadCrumbs.addItem("file");
        assertEquals(3, breadCrumbs.getWidgetCount());

        final PBreadCrumbs.ItemLevel home = (PBreadCrumbs.ItemLevel) breadCrumbs.getWidget(0);
        final PBreadCrumbs.ItemLevel folder = (PBreadCrumbs.ItemLevel) breadCrumbs.getWidget(1);
        final PBreadCrumbs.ItemLevel file = (PBreadCrumbs.ItemLevel) breadCrumbs.getWidget(2);
        assertEquals("home", home.getItem());
        assertEquals(1, home.getLevel());
        assertEquals(3, file.getLevel());
        assertFalse(home.hasPrevious());
        assertSame(home, folder.getPrevious());
        assertTrue(folder.hasPrevious());
        assertSame(file, folder.getNext());
        assertTrue(folder.hasNext());
        assertFalse(file.hasNext());
        // Previous levels become links, the current one stays text
        assertNull(home.getInnerText());
        assertEquals("home", ((PAnchor) home.getWidget(0)).getText());
        assertEquals("file", file.getInnerText());

        final List<String> selected = new ArrayList<>();
        breadCrumbs.addSelectionHandler(event -> selected.add(event.getSelectedItem().getItem()));
        PEmulator.click(folder.getWidget(0));
        assertEquals(List.of("folder"), selected);
        assertEquals(2, breadCrumbs.getWidgetCount());
        assertEquals("folder", folder.getInnerText());

        breadCrumbs.selectItem("unknown");
        // folder is current: nothing to do
        breadCrumbs.selectItem("folder");
        assertEquals(2, breadCrumbs.getWidgetCount());

        // file was dropped, it can be added again
        breadCrumbs.addItem("file");
        assertEquals(3, breadCrumbs.getWidgetCount());
        breadCrumbs.selectItem("home");
        assertEquals(1, breadCrumbs.getWidgetCount());
    }

    @Test
    public void testClosableDialogBox() {
        final PClosableDialogBox dialog = new PClosableDialogBox("Title");
        assertTrue(dialog.hasStyleName("pony-closable-dialog-box"));
        final PFlexTable layout = (PFlexTable) dialog.getWidget();
        final PSimplePanel caption = (PSimplePanel) layout.getWidget(0, 0);
        final PSimplePanel close = (PSimplePanel) layout.getWidget(0, 1);
        final PSimplePanel content = (PSimplePanel) layout.getWidget(1, 0);
        assertEquals("Title", ((PLabel) caption.getWidget()).getText());

        final PLabel newCaption = Element.newPLabel("Other");
        final PLabel newClose = Element.newPLabel("x");
        final PLabel body = Element.newPLabel("body");
        dialog.setCaption(newCaption);
        dialog.setClose(newClose);
        dialog.setContent(body);
        assertSame(newCaption, caption.getWidget());
        assertSame(newClose, close.getWidget());
        assertSame(body, content.getWidget());
        assertThrows(IllegalArgumentException.class, () -> dialog.setWidget(Element.newPLabel()));

        PWindow.getMain().add(dialog);
        writer = CapturingModelWriter.install();
        dialog.displayAtCenter();
        assertTrue(dialog.isVisible());
        dialog.onClientData(jakarta.json.Json.createObjectBuilder()
            .add(com.ponysdk.core.model.ClientToServerModel.POPUP_POSITION.toStringValue(),
                jakarta.json.Json.createArrayBuilder().add(100).add(50).add(1000).add(800))
            .build());
        assertEquals(List.of(450), writer.valuesOf(ServerToClientModel.POSITION_LEFT));
        assertEquals(List.of(375), writer.valuesOf(ServerToClientModel.POSITION_TOP));

        PEmulator.click(close);
        assertFalse(dialog.isVisible());
        dialog.removeFromParent();
    }

    @Test
    public void testConfirmDialogOkAndCancel() {
        final List<String> calls = new ArrayList<>();
        final boolean[] acceptOk = { false };
        final com.ponysdk.core.ui.basic.PConfirmDialogHandler handler = new com.ponysdk.core.ui.basic.PConfirmDialogHandler() {

            @Override
            public boolean onOK(final com.ponysdk.core.ui.basic.PDialogBox dialogBox) {
                calls.add("ok");
                return acceptOk[0];
            }

            @Override
            public void onCancel() {
                calls.add("cancel");
            }
        };
        final PConfirmDialog dialog = PConfirmDialog.show(PWindow.getMain(), "Confirm", "Sure?", "Yes", "No", handler);
        assertEquals("Confirm", dialog.getCaption());
        assertTrue(dialog.isVisible());
        assertTrue(dialog.hasStyleName("pony-DialogBox"));
        assertFalse(dialog.isAnimationEnabled());

        final com.ponysdk.core.ui.basic.PVerticalPanel content = (com.ponysdk.core.ui.basic.PVerticalPanel) dialog.getWidget();
        assertEquals("Sure?", ((PLabel) content.getWidget(0)).getText());
        final PHorizontalPanel controls = (PHorizontalPanel) content.getWidget(1);
        final PButton cancel = (PButton) controls.getWidget(0);
        final PButton ok = (PButton) controls.getWidget(1);
        assertEquals("No", cancel.getText());
        assertEquals("Yes", ok.getText());

        Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(true);
        dialog.ensureDebugId("confirm");
        Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(false);
        assertEquals("confirm[ok]", ok.getDebugID());
        assertEquals("confirm[cancel]", cancel.getDebugID());

        // The handler refuses: the dialog stays
        PEmulator.click(ok);
        assertTrue(dialog.isVisible());
        acceptOk[0] = true;
        PEmulator.click(ok);
        assertFalse(dialog.isVisible());

        dialog.show();
        PEmulator.click(cancel);
        assertFalse(dialog.isVisible());
        assertEquals(List.of("ok", "ok", "cancel"), calls);
        dialog.removeFromParent();
    }

    @Test
    public void testConfirmDialogWithoutHandler() {
        final PConfirmDialog dialog = PConfirmDialog.show(PWindow.getMain(), "Info", Element.newPLabel("Done"));
        final com.ponysdk.core.ui.basic.PVerticalPanel content = (com.ponysdk.core.ui.basic.PVerticalPanel) dialog.getWidget();
        final PHorizontalPanel controls = (PHorizontalPanel) content.getWidget(1);
        assertEquals(1, controls.getWidgetCount());
        final PButton ok = (PButton) controls.getWidget(0);
        assertEquals("Ok", ok.getText());
        PEmulator.click(ok);
        assertFalse(dialog.isVisible());
        dialog.removeFromParent();

        final PConfirmDialog withCancelOnly = PConfirmDialog.buildPopup("Caption", Element.newPLabel("x"), null, "Cancel", null);
        final PHorizontalPanel cancelOnly = (PHorizontalPanel) ((com.ponysdk.core.ui.basic.PVerticalPanel) withCancelOnly.getWidget())
            .getWidget(1);
        assertEquals(1, cancelOnly.getWidgetCount());
        withCancelOnly.show();
        PEmulator.click(cancelOnly.getWidget(0));
        assertFalse(withCancelOnly.isVisible());

        final boolean[] ok2 = { false };
        final PConfirmDialog withHandler = PConfirmDialog.show(PWindow.getMain(), "Caption", Element.newPLabel("y"),
            new com.ponysdk.core.ui.basic.PConfirmDialogHandler() {

                @Override
                public boolean onOK(final com.ponysdk.core.ui.basic.PDialogBox dialogBox) {
                    ok2[0] = true;
                    return true;
                }

                @Override
                public void onCancel() {
                }
            });
        PEmulator.click(((PHorizontalPanel) ((com.ponysdk.core.ui.basic.PVerticalPanel) withHandler.getWidget()).getWidget(1)).getWidget(0));
        assertTrue(ok2[0]);
        assertFalse(withHandler.isVisible());
        withHandler.removeFromParent();
    }

    @Test
    public void testOptionPane() {
        Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(true);
        final List<String> options = new ArrayList<>();
        final POptionPane pane = POptionPane.showConfirmDialog(PWindow.getMain(), (dialog, option) -> options.add(option), "Proceed?",
            "Title", POptionPane.POptionType.YES_NO_CANCEL_OPTION, POptionPane.PMessageType.WARNING_MESSAGE);
        assertSame(pane.getDialogBox(), pane.asWidget());
        assertEquals("Warning", pane.getDialogBox().getCaption());
        assertEquals("Title", pane.getDialogBox().getTitle());
        assertTrue(pane.getDialogBox().isVisible());

        final com.ponysdk.core.ui.basic.PVerticalPanel panel = (com.ponysdk.core.ui.basic.PVerticalPanel) pane.getDialogBox().getWidget();
        assertEquals("Proceed?", ((PLabel) panel.getWidget(0)).getText());
        final PHorizontalPanel controls = (PHorizontalPanel) panel.getWidget(1);
        assertEquals(3, controls.getWidgetCount());
        Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(false);
        assertEquals("optionpane[NO]", controls.getWidget(1).getDebugID());
        for (int i = 0; i < controls.getWidgetCount(); i++) {
            PEmulator.click(controls.getWidget(i));
        }
        assertEquals(List.of("YES", "NO", "CANCEL"), options);
        pane.getDialogBox().removeFromParent();

        assertEquals(1, buttonCount(POptionPane.showConfirmDialog(PWindow.getMain(), (d, o) -> {
        }, "m")));
        assertEquals(2, buttonCount(POptionPane.showConfirmDialog(PWindow.getMain(), (d, o) -> {
        }, "m", "t", POptionPane.POptionType.OK_CANCEL_OPTION)));
        assertEquals(2, buttonCount(POptionPane.showConfirmDialog(PWindow.getMain(), (d, o) -> {
        }, "m", "t", POptionPane.POptionType.YES_NO_OPTION)));
        assertEquals(1, buttonCount(POptionPane.showOptionDialog(PWindow.getMain(), (d, o) -> {
        }, "m", "t", POptionPane.POptionType.DEFAULT_OPTION, POptionPane.PMessageType.PLAIN_MESSAGE, "Custom")));
    }

    private static int buttonCount(final POptionPane pane) {
        final com.ponysdk.core.ui.basic.PVerticalPanel panel = (com.ponysdk.core.ui.basic.PVerticalPanel) pane.getDialogBox().getWidget();
        final int count = ((PHorizontalPanel) panel.getWidget(1)).getWidgetCount();
        pane.getDialogBox().removeFromParent();
        return count;
    }

    @Test
    public void testOptionEnums() {
        assertTrue(POptionPane.POption.OK_OPTION.equals("OK"));
        assertFalse(POptionPane.POption.OK_OPTION.equals("CANCEL"));
        assertEquals("CLOSED", POptionPane.POption.CLOSED_OPTION.getName());
        assertEquals("Error", POptionPane.PMessageType.ERROR_MESSAGE.getName());
        assertEquals("Info", POptionPane.PMessageType.INFORMATION_MESSAGE.getName());
        assertEquals("", POptionPane.PMessageType.PLAIN_MESSAGE.getName());
        assertEquals("Question", POptionPane.PMessageType.QUESTION_MESSAGE.getName());

        final com.ponysdk.core.ui.basic.PDialogBox dialogBox = Element.newPDialogBox();
        assertSame(dialogBox, new POptionPane(dialogBox).getDialogBox());
        assertTrue(new POptionPane().getDialogBox() != null);
    }

    @Test
    public void testToolbar() {
        final PToolbar toolbar = new PToolbar();
        assertTrue(toolbar.hasStyleName("ptoolbar"));
        final PButton button = Element.newPButton("b");
        toolbar.add(button);
        toolbar.addSepararator();
        assertEquals(2, toolbar.getWidgetCount());
        assertSame(button, toolbar.getWidget(0));
        assertTrue(toolbar.getWidget(1).hasStyleName("pony-Toolbar-Separator"));
    }

    @Test
    public void testTwinListBox() {
        final PTwinListBox<String> twin = new PTwinListBox<>("Left", "Right");
        assertTrue(twin.hasStyleName("pony-TwinListBox"));
        assertEquals("Left", ((PLabel) twin.getWidget(0, 0)).getText());
        assertEquals("Right", ((PLabel) twin.getWidget(0, 2)).getText());

        final PListBox left = twin.getLeftListBox();
        final PListBox right = twin.getRightListBox();
        assertTrue(left.isMultipleSelect());
        left.addItem("a", 1);
        left.addItem("b", 2);
        left.addItem("c", 3);
        right.addItem("z", 26);
        left.setSelectedIndex(0, true);
        right.setSelectedIndex(0, true);

        final int[] changes = { 0 };
        final com.ponysdk.core.ui.basic.event.PChangeHandler handler = event -> {
            assertSame(twin, event.getSource());
            changes[0]++;
        };
        twin.addChangeHandler(handler);

        PEmulator.click(twin.getWidget(1, 1));
        assertEquals(1, changes[0]);
        assertEquals(List.of("b", "c", "z"), items(left));
        assertEquals(List.of("a"), items(right));
        assertEquals(1, right.getValue(0));

        assertTrue(twin.removeChangeHandler(handler));
        assertFalse(twin.removeChangeHandler(handler));

        twin.setEnabled(false);
        assertFalse(twin.isEnabled());
        assertFalse(left.isEnabled());
        assertFalse(right.isEnabled());
        assertFalse(((PButton) twin.getWidget(1, 1)).isEnabled());
        twin.setEnabled(true);
        assertTrue(twin.isEnabled());

        twin.clear();
        assertEquals(0, left.getItemCount());
        assertEquals(0, right.getItemCount());

        final PTwinListBox<String> noCaption = new PTwinListBox<>();
        assertNull(noCaption.getWidget(0, 0));
        assertNull(noCaption.getWidget(0, 2));
    }

    @Test
    public void testTwinListBoxMovesOnlySelectedItems() {
        final PTwinListBox<String> twin = new PTwinListBox<>();
        final PListBox left = twin.getLeftListBox();
        left.addItem("a", 1);
        left.addItem("b", 2);
        left.addItem("c", 3);
        left.setSelectedIndex(0, true);
        left.setSelectedIndex(2, true);
        twin.getRightListBox().addItem("y", 25);
        PEmulator.click(twin.getWidget(1, 1));
        assertEquals(List.of("b"), items(left));
        assertEquals(List.of("y", "a", "c"), items(twin.getRightListBox()));
    }

    private static List<String> items(final PListBox listBox) {
        final List<String> items = new ArrayList<>();
        for (int i = 0; i < listBox.getItemCount(); i++) {
            items.add(listBox.getItem(i));
        }
        return items;
    }
}
