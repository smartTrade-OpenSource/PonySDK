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

import static com.ponysdk.sample.client.Ui.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PMenuItem;
import com.ponysdk.core.ui.basic.PMultiWordSuggestOracle;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PRadioButtonGroup;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PTabLayoutPanel;
import com.ponysdk.core.ui.basic.PTabPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.flexlayout.FlexLayoutPanel;
import com.ponysdk.core.ui.listbox.ListBox;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.listbox.ListBoxConfiguration;
import com.ponysdk.core.ui.rich.PNotificationManager;
import com.ponysdk.core.ui.rich.PNotificationManager.Notification;

/**
 * Theme (docs/decisions/0024-default-theme.md): every widget under the default theme, in a frame that runs a second
 * session of the showcase with the theme loaded in its page and the showcase's own stylesheet left out, and the
 * tokens as the stylesheet defines them.
 */
final class ThemePage {

    /** The token of the page the frame shows: {@link UISampleEntryPoint} hands it to {@link #buildPreview}. */
    static final String PREVIEW_TOKEN = "theme-preview";

    private static final String STYLESHEET = "style/pony-theme.css";
    private static final Pattern TOKEN = Pattern.compile("(--pony-[a-z0-9-]+):\\s*([^;]+);");

    private ThemePage() {
    }

    // ---- The page: the scheme, the frame, the tokens --------------------------------------------------------------

    static PFlowPanel build() {
        final PFlowPanel p = section();
        pageTitle(p, "Theme", "A default look for every widget, without writing CSS: light and dark from the system or "
            + "from one attribute, WCAG AA contrast in both, every colour, space and radius a --pony- custom property. "
            + "An application turns it on with ApplicationConfiguration.setTheme(\"pony\") and overrides what it wants.");

        p.add(sectionTitle("Every widget"));
        final PFlowPanel scheme = Element.newPFlowPanel();
        scheme.addStyleName("th-scheme");
        scheme.add(el("th-scheme-label", "Colour scheme"));
        final PFlowPanel segments = Element.newPFlowPanel();
        segments.addStyleName("pony-ex-seg");
        final PButton[] buttons = new PButton[3];
        final String[] names = { "System", "Light", "Dark" };
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            buttons[i] = Element.newPButton(names[i]);
            buttons[i].addStyleName(i == 0 ? "pony-ex-seg-on" : "pony-ex-seg-off");
            // region scheme
            // "light" or "dark" sets data-theme on the frame's page; "system" removes it, and the browser chooses
            buttons[i].addClickHandler(e -> PScript.call(PWindow.getMain(), "showcaseThemeScheme", names[index].toLowerCase()));
            // endregion
            buttons[i].addClickHandler(e -> {
                for (final PButton b : buttons) {
                    b.removeStyleName("pony-ex-seg-on");
                    b.addStyleName("pony-ex-seg-off");
                }
                buttons[index].removeStyleName("pony-ex-seg-off");
                buttons[index].addStyleName("pony-ex-seg-on");
            });
            segments.add(buttons[i]);
        }
        scheme.add(segments);
        p.add(scheme);

        final PElement frame = Element.newPElement("iframe");
        frame.addStyleName("th-frame");
        frame.setAttribute("title", "The widgets under the default theme");
        frame.setAttribute("src", "./#" + PREVIEW_TOKEN);
        p.add(frame);
        p.add(el("ov-note", "The frame is a second session of the showcase: its page loads style/pony-theme.css and leaves the "
            + "showcase's own stylesheet out. Dialogs, popups, menus and lists open in that page, so the theme reaches them."));
        p.add(codeToggle("what the frame's page does", Source.code(ThemePage.class, "load")));
        p.add(codeToggle("the colour scheme buttons", Source.code(ThemePage.class, "scheme")));

        p.add(sectionTitle("Tokens"));
        p.add(el("ov-note", "Read from the stylesheet. Override one on :root, or on an element with the class pony-theme: "
            + ":root { --pony-accent: #7c3aed; }. Dark values apply with prefers-color-scheme: dark, or [data-theme=dark]."));
        p.add(tokenTable());
        return p;
    }

    /** The tokens of the stylesheet's light and dark blocks, in file order, as a table with a swatch for the colours. */
    private static PFlowPanel tokenTable() {
        final Map<String, String> light = tokens("tokens:light");
        final Map<String, String> dark = tokens("tokens:dark");
        final Map<String, String> shared = tokens("tokens:shared");
        final PFlowPanel table = Element.newPFlowPanel();
        table.addStyleName("th-tokens");
        table.add(tokenRow("th-token-head", "Token", tokenValue("Light", false), tokenValue("Dark", false)));
        light.forEach((name, value) -> table.add(tokenRow("th-token", name, tokenValue(value, true),
            tokenValue(dark.getOrDefault(name, value), true))));
        shared.forEach((name, value) -> table.add(tokenRow("th-token", name, tokenValue(value, false), tokenValue(value, false))));
        return table;
    }

    private static PFlowPanel tokenRow(final String style, final String name, final PFlowPanel light, final PFlowPanel dark) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName(style);
        row.add(el("th-token-name", name), light, dark);
        return row;
    }

    /** A value, with a swatch of its colour when it is one. */
    private static PFlowPanel tokenValue(final String text, final boolean swatch) {
        final PFlowPanel cell = Element.newPFlowPanel();
        cell.addStyleName("th-token-value");
        if (swatch && (text.startsWith("#") || text.startsWith("rgb"))) {
            final PElement colour = Element.newDiv();
            colour.addStyleName("th-swatch");
            colour.setStyleProperty("background", text);
            cell.add(colour);
        }
        cell.add(el("th-token-text", text));
        return cell;
    }

    /** The declarations of the stylesheet's block that follows the comment {@code /* marker *}{@code /}. */
    private static Map<String, String> tokens(final String marker) {
        final String css;
        try (InputStream in = ThemePage.class.getClassLoader().getResourceAsStream(STYLESHEET)) {
            if (in == null) throw new IllegalStateException(STYLESHEET + " is not on the classpath");
            css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final int comment = css.indexOf("/* " + marker + " */");
        if (comment < 0) throw new IllegalStateException(STYLESHEET + " has no " + marker + " block");
        final int open = css.indexOf('{', comment);
        final Matcher m = TOKEN.matcher(css.substring(open, css.indexOf('}', open)));
        final Map<String, String> tokens = new LinkedHashMap<>();
        while (m.find()) tokens.put(m.group(1), m.group(2).trim());
        return tokens;
    }

    // ---- The frame's page: every widget -----------------------------------------------------------------------------

    /** The session of the frame: the theme in its page, then each family of widgets in a row. */
    static void buildPreview() {
        // region load
        // The theme is loaded in this page and the showcase's own stylesheet is left out (script/sample.js); an
        // application asks for it once, in its configuration: configuration.setTheme("pony")
        PScript.call(PWindow.getMain(), "showcaseTheme");
        // endregion
        final PFlowPanel page = Element.newPFlowPanel();
        page.addStyleName("th-page");

        page.add(group("Buttons", buttons()));
        page.add(group("Text fields", textFields()));
        page.add(group("Choices", choices()));
        page.add(group("Tabs, stack, disclosure", panels()));
        page.add(group("Menus", menus()));
        page.add(group("Tree", row(DataPage.createTree())));
        page.add(group("Dates", dates()));
        page.add(group("Rich text", row(InputsPage.buildRichText())));
        page.add(group("Drop-downs", dropDowns()));
        page.add(group("Layouts, tables", layouts()));
        page.add(group("DataGrid2", row(dataGrid())));
        page.add(group("Dialogs, popups, notifications", overlays()));
        page.add(group("FlexLayout", row(flexLayout())));
        PWindow.getMain().add(page);
    }

    private static PFlowPanel group(final String title, final PFlowPanel row) {
        final PFlowPanel group = Element.newPFlowPanel();
        group.addStyleName("th-group");
        group.add(el("th-title", title));
        group.add(row);
        return group;
    }

    private static PFlowPanel row(final PWidget... widgets) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("th-row");
        for (final PWidget w : widgets) row.add(w);
        return row;
    }

    private static PFlowPanel buttons() {
        final PButton primary = Element.newPButton("Primary");
        primary.addStyleName("pony-primary");
        final PButton danger = Element.newPButton("Delete");
        danger.addStyleName("pony-danger");
        final PButton disabled = Element.newPButton("Disabled");
        disabled.setEnabled(false);
        final PButton busy = Element.newPButton("Saving");
        busy.setEnabled(false);
        busy.setAttribute("aria-busy", "true"); // what the terminal sets while a request is in flight
        final PImage icon = Element.newPImage();
        icon.setAttribute("alt", ""); // decorative: the button's text says it
        final PButton push = Element.newPPushButton(icon);
        push.setText("Push button");
        return row(Element.newPButton("Default"), primary, danger, disabled, busy, push, Element.newPAnchor("An anchor", "#"));
    }

    private static PFlowPanel textFields() {
        final PTextBox box = Element.newPTextBox();
        box.setPlaceholder("A text box");
        box.setAttribute("aria-label", "Text box");
        final PTextBox invalid = Element.newPTextBox();
        invalid.setText("Not a number");
        invalid.setAttribute("aria-invalid", "true");
        invalid.setAttribute("aria-label", "Invalid text box");
        final PTextBox disabled = Element.newPTextBox();
        disabled.setText("Disabled");
        disabled.setEnabled(false);
        disabled.setAttribute("aria-label", "Disabled text box");
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.addAll(List.of("EUR/USD", "EUR/GBP", "EUR/JPY", "GBP/USD", "USD/JPY"));
        final PListBox select = Element.newPListBox();
        select.setAttribute("aria-label", "Drop-down list");
        select.addItem("One");
        select.addItem("Two");
        final PListBox multiple = Element.newPListBox();
        multiple.setMultipleSelect(true);
        multiple.setAttribute("aria-label", "Desks");
        multiple.addItem("Rates");
        multiple.addItem("Credit");
        multiple.addItem("FX");
        final PWidget area = Element.newPTextArea();
        area.setAttribute("aria-label", "Text area");
        final PWidget password = Element.newPPasswordTextBox("Password");
        password.setAttribute("aria-label", "Password");
        final PWidget suggest = Element.newPSuggestBox(oracle);
        suggest.setAttribute("aria-label", "Currency pair");
        return row(box, invalid, disabled, password, suggest, select, multiple, area);
    }

    private static PFlowPanel choices() {
        final PCheckBox on = Element.newPCheckBox("Checked");
        on.setValue(true);
        final PCheckBox off = Element.newPCheckBox("Unchecked");
        final PCheckBox disabled = Element.newPCheckBox("Disabled");
        disabled.setEnabled(false);
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("theme-radio");
        final PFlowPanel radios = Element.newPFlowPanel();
        radios.addStyleName("th-row");
        for (final String option : List.of("Small", "Medium", "Large")) {
            final PRadioButton radio = Element.newPRadioButton(option);
            group.addRadioButton(radio);
            radios.add(radio);
        }
        return row(on, off, disabled, radios, MiscPage.createPFileUpload());
    }

    private static PFlowPanel panels() {
        final PTabPanel tabs = Element.newPTabPanel();
        tabs.add(Element.newPLabel("First tab content"), "First");
        tabs.add(Element.newPLabel("Second tab content"), "Second");
        tabs.add(Element.newPLabel("Third tab content"), "Third");
        tabs.selectTab(0);
        final PTabLayoutPanel layoutTabs = Element.newPTabLayoutPanel();
        layoutTabs.add(Element.newPLabel("Layout tab content"), "Layout");
        layoutTabs.add(Element.newPLabel("Another tab content"), "Another");
        layoutTabs.selectTab(0);
        final PStackLayoutPanel stack = Element.newPStackLayoutPanel(PUnit.PX);
        stack.add(Element.newPLabel("Mail section"), "Mail", false, 32);
        stack.add(Element.newPLabel("Contacts section"), "Contacts", false, 32);
        stack.add(Element.newPLabel("Tasks section"), "Tasks", false, 32);
        final PDisclosurePanel disclosure = Element.newPDisclosurePanel("Disclosure panel");
        disclosure.setContent(Element.newPLabel("Its content, shown when it opens"));
        disclosure.setOpen(true);
        return row(box(tabs, 280, 120), box(layoutTabs, 280, 120), box(stack, 280, 160), disclosure);
    }

    private static PFlowPanel menus() {
        final PMenuBar bar = Element.newPMenuBar();
        final PMenuBar file = Element.newPMenuBar(true);
        file.addItem(Element.newPMenuItem("New", () -> { }));
        file.addItem(Element.newPMenuItem("Open", () -> { }));
        file.addSeparator();
        final PMenuItem disabled = Element.newPMenuItem("Disabled", () -> { });
        disabled.setEnabled(false);
        file.addItem(disabled);
        bar.addItem(Element.newPMenuItem("File", file));
        bar.addItem(Element.newPMenuItem("Edit", Element.newPMenuBar(true)));
        bar.addItem(Element.newPMenuItem("Help", () -> { }));
        return row(bar, InputsPage.createMenu());
    }

    private static PFlowPanel dates() {
        final PDateBox dateBox = Element.newPDateBox();
        dateBox.setAttribute("aria-label", "Date");
        return row(Element.newPDatePicker(), dateBox);
    }

    private static PFlowPanel dropDowns() {
        final List<ListBoxItem<String>> items = List.of(ListBoxItem.of("EUR"), ListBoxItem.of("USD"), ListBoxItem.of("GBP"),
            ListBoxItem.of("JPY"), ListBoxItem.of("CHF"));
        final ListBoxConfiguration singleConfiguration = new ListBoxConfiguration();
        singleConfiguration.setTitle("Currency");
        final ListBoxConfiguration multiConfiguration = new ListBoxConfiguration().enableMultiSelection();
        multiConfiguration.setTitle("Desks");
        final ListBox<String> single = new ListBox<>(singleConfiguration, items);
        final ListBox<String> multi = new ListBox<>(multiConfiguration,
            List.of(ListBoxItem.of("Rates"), ListBoxItem.of("Credit"), ListBoxItem.of("FX"), ListBoxItem.of("Equities")));
        return row(single.asWidget(), multi.asWidget());
    }

    private static PFlowPanel layouts() {
        final PSplitLayoutPanel split = Element.newPSplitLayoutPanel();
        split.addWest(Element.newPLabel("Drag the splitter"), 150);
        split.add(Element.newPLabel("Center"));
        final PScrollPanel scroll = Element.newPScrollPanel();
        scroll.setTabindex(PWidget.TabindexMode.TABULABLE); // no focusable content: a keyboard user scrolls it as a whole
        final PFlowPanel lines = Element.newPFlowPanel();
        for (int i = 1; i <= 12; i++) lines.add(Element.newPLabel("A line of a scroll panel, number " + i));
        scroll.setWidget(lines);
        final PFlexTable table = Element.newPFlexTable();
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) table.setWidget(r, c, Element.newPLabel("Cell " + r + ":" + c));
        return row(box(split, 320, 120), box(scroll, 320, 120), table);
    }

    private static PWidget dataGrid() {
        final PWidget grid = Costs.smallDataGrid();
        grid.setHeight("240px");
        grid.setWidth("100%");
        return grid;
    }

    private static PFlowPanel overlays() {
        final PButton dialog = Element.newPButton("Dialog");
        // region dialog
        dialog.addClickHandler(e -> {
            final PDialogBox box = Element.newPDialogBox(true);
            box.setCaption("A dialog box");
            final PFlowPanel body = Element.newPFlowPanel();
            body.add(Element.newPLabel("The caption, the content and the glass under it take the theme."));
            final PButton close = Element.newPButton("Close");
            close.addStyleName("pony-primary");
            close.addClickHandler(ev -> box.hide());
            body.add(close);
            box.setWidget(body);
            box.setGlassEnabled(true);
            PWindow.getMain().add(box);
            box.center();
        });
        // endregion
        final PButton popup = Element.newPButton("Popup");
        popup.addClickHandler(e -> {
            final com.ponysdk.core.ui.basic.PDecoratedPopupPanel panel = Element.newPDecoratedPopupPanel(true);
            panel.setWidget(Element.newPLabel("An auto-hide popup: click anywhere to close"));
            PWindow.getMain().add(panel);
            panel.center();
        });
        return row(dialog, popup, notify("Info", Notification.HUMANIZED), notify("Warning", Notification.WARNING_MESSAGE),
            notify("Error", Notification.ERROR_MESSAGE), notify("Tray", Notification.TRAY));
    }

    private static PButton notify(final String text, final Notification kind) {
        final PButton button = Element.newPButton(text);
        button.addClickHandler(e -> PNotificationManager.notify(text + " notification", kind));
        return button;
    }

    private static PWidget flexLayout() {
        final FlexLayoutPanel flex = new FlexLayoutPanel();
        flex.setTheme("fl-theme-pony"); // the skin of the default theme (style/pony-theme.css)
        for (final String tab : new String[] { "Blotter", "Chart", "Orders" }) flex.addTab(tab, Element.newPLabel(tab + " panel"));
        final PFlowPanel frame = Element.newPFlowPanel(); // the layout fills what its parent gives: a size to give
        frame.setHeight("220px");
        frame.setWidth("100%");
        frame.add(flex.asWidget());
        return frame;
    }

    /** A panel to the size of a layout panel, which needs one. */
    private static PWidget box(final PWidget w, final int width, final int height) {
        w.setWidth(width + "px");
        w.setHeight(height + "px");
        return w;
    }
}
