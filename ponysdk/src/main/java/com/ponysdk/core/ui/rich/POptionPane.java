/*
 * Copyright (c) 2011 PonySDK
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

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;

/**
 * POptionPane makes it easy to pop up a standard dialog box that prompts users
 * for a value or informs them of something.
 */
public class POptionPane implements IsPWidget {

    final PDialogBox dialogBox;

    public POptionPane() {
        this(Element.newPDialogBox());
    }

    public POptionPane(final PDialogBox dialogBox) {
        this.dialogBox = dialogBox;
    }

    public static POptionPane showConfirmDialog(final PWindow window, final PActionHandler handler, final String message) {
        return showConfirmDialog(window, handler, message, "Message", POptionType.DEFAULT_OPTION);
    }

    public static POptionPane showConfirmDialog(final PWindow window, final PActionHandler handler, final String message,
                                                final String title, final POptionType optionType) {
        return showConfirmDialog(window, handler, message, title, optionType, PMessageType.QUESTION_MESSAGE);
    }

    public static POptionPane showConfirmDialog(final PWindow window, final PActionHandler handler, final String message,
                                                final String title, final POptionType optionType, final PMessageType messageType) {
        return showOptionDialog(window, handler, message, title, optionType, messageType, getOptions(optionType));
    }

    public static POptionPane showOptionDialog(final PWindow window, final PActionHandler handler, final String message,
                                               final String title, final POptionType optionType, final PMessageType messageType,
                                               final String... options) {
        final POptionPane optionPane = new POptionPane();

        final PDialogBox dialogBox = optionPane.getDialogBox();
        dialogBox.addStyleName("pony-DialogBox");
        dialogBox.setAnimationEnabled(false);
        dialogBox.setGlassEnabled(true);
        dialogBox.setTitle(title);
        dialogBox.setCaption(messageType.getName());

        // Build content
        final PVerticalPanel panel = Element.newPVerticalPanel();
        final PLabel content = Element.newPLabel(message);
        panel.add(content);
        final PHorizontalPanel controlsPanel = Element.newPHorizontalPanel();
        controlsPanel.addStyleName("dialogControls");
        controlsPanel.setHorizontalAlignment(PHorizontalAlignment.ALIGN_CENTER);

        for (final String option : options) {
            final PButton button = Element.newPButton(option);
            button.ensureDebugId("optionpane[" + option + "]");
            button.addClickHandler(clickEvent -> handler.onAction(dialogBox, option));
            controlsPanel.add(button);
            controlsPanel.setCellHorizontalAlignment(button, PHorizontalAlignment.ALIGN_CENTER);
        }

        panel.add(controlsPanel);
        panel.setCellHorizontalAlignment(controlsPanel, PHorizontalAlignment.ALIGN_CENTER);
        panel.setCellHorizontalAlignment(content, PHorizontalAlignment.ALIGN_CENTER);

        window.add(optionPane);

        dialogBox.setWidget(panel);
        dialogBox.center();

        return optionPane;
    }

    private static String[] getOptions(final POptionType optionType) {
        switch (optionType) {
            case DEFAULT_OPTION:
                return new String[] { POption.OK_OPTION.getName() };
            case OK_CANCEL_OPTION:
                return new String[] { POption.OK_OPTION.getName(), POption.CANCEL_OPTION.getName() };
            case YES_NO_CANCEL_OPTION:
                return new String[] { POption.YES_OPTION.getName(), POption.NO_OPTION.getName(), POption.CANCEL_OPTION.getName() };
            case YES_NO_OPTION:
                return new String[] { POption.YES_OPTION.getName(), POption.NO_OPTION.getName() };
            default:
                break;
        }
        return null;
    }

    public PDialogBox getDialogBox() {
        return dialogBox;
    }

    @Override
    public PWidget asWidget() {
        return getDialogBox();
    }

    public enum POption {

        CANCEL_OPTION("CANCEL"),
        CLOSED_OPTION("CLOSED"),
        NO_OPTION("NO"),
        OK_OPTION("OK"),
        YES_OPTION("YES");

        private final String name;

        POption(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean equals(final String option) {
            return name.equals(option);
        }
    }

    public enum POptionType {
        DEFAULT_OPTION,
        OK_CANCEL_OPTION,
        YES_NO_CANCEL_OPTION,
        YES_NO_OPTION
    }

    public enum PMessageType {

        PLAIN_MESSAGE(""),
        ERROR_MESSAGE("Error"),
        INFORMATION_MESSAGE("Info"),
        WARNING_MESSAGE("Warning"),
        QUESTION_MESSAGE("Question");

        private final String name;

        PMessageType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @FunctionalInterface
    public interface PActionHandler {

        void onAction(PDialogBox dialogBox, String option);
    }

}
