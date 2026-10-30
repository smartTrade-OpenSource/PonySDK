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

import java.time.LocalDate;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCommands;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PEventOptions;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.binder.Binder;
import com.ponysdk.core.ui.binder.Converter;
import com.ponysdk.core.ui.binder.Converters;
import com.ponysdk.sample.client.OrderTicket.Side;
import com.ponysdk.sample.client.OrderTicket.Type;
import com.ponysdk.sample.client.wa.PWaAvatar;
import com.ponysdk.sample.client.wa.PWaBadge;
import com.ponysdk.sample.client.wa.PWaButton;
import com.ponysdk.sample.client.wa.PWaCallout;
import com.ponysdk.sample.client.wa.PWaCard;
import com.ponysdk.sample.client.wa.PWaCheckbox;
import com.ponysdk.sample.client.wa.PWaColorPicker;
import com.ponysdk.sample.client.wa.PWaDetails;
import com.ponysdk.sample.client.wa.PWaDialog;
import com.ponysdk.sample.client.wa.PWaDrawer;
import com.ponysdk.sample.client.wa.PWaInput;
import com.ponysdk.sample.client.wa.PWaOption;
import com.ponysdk.sample.client.wa.PWaProgressBar;
import com.ponysdk.sample.client.wa.PWaProgressRing;
import com.ponysdk.sample.client.wa.PWaRadio;
import com.ponysdk.sample.client.wa.PWaRadioGroup;
import com.ponysdk.sample.client.wa.PWaRating;
import com.ponysdk.sample.client.wa.PWaSelect;
import com.ponysdk.sample.client.wa.PWaSlider;
import com.ponysdk.sample.client.wa.PWaSplitPanel;
import com.ponysdk.sample.client.wa.PWaSwitch;
import com.ponysdk.sample.client.wa.PWaTab;
import com.ponysdk.sample.client.wa.PWaTabGroup;
import com.ponysdk.sample.client.wa.PWaTabPanel;
import com.ponysdk.sample.client.wa.PWaTag;
import com.ponysdk.sample.client.wa.PWaTextarea;
import com.ponysdk.sample.client.wa.PWaToast;
import com.ponysdk.sample.client.wa.PWaTooltip;
import com.ponysdk.sample.client.wa.PWaTree;
import com.ponysdk.sample.client.wa.PWaTreeItem;
import com.ponysdk.sample.client.wa.WaFields;

import jakarta.json.Json;

/**
 * Web Awesome (docs/decisions/0028-web-awesome.md): a design system's components driven from Java, through classes the
 * build generates from the library's own Custom Elements Manifest. The library is loaded by this page alone, from this
 * application's origin; its look follows the default theme's tokens ({@code css/pony-webawesome.css}); its form controls
 * are fields of the Binder.
 */
final class WebAwesomePage {

    private WebAwesomePage() {
    }

    static PFlowPanel build(final UIContext uiContext) {
        // region load
        // Loads the library once, by name (script/webawesome-loader.js), when the first page that uses it is built
        PScript.call(PWindow.getMain(), "loadWebAwesome");
        // endregion

        final PFlowPanel p = section();
        pageTitle(p, "Web Awesome", "A design system of 70 components, driven from Java: typed classes the build generates from the "
            + "library's custom-elements.json, the library served by this application and loaded by this page alone, its look "
            + "taken from the default theme, its form controls bound with the Binder.");

        // The scope of the theme and of the library's look: everything below is in it. pony-theme gives the tokens,
        // wa-theme-default makes the library compute its own from them here (css/pony-webawesome.css)
        final PFlowPanel root = Element.newPFlowPanel();
        root.addStyleName("pony-theme");
        root.addStyleName("wa-theme-default");
        root.addStyleName("wad-root");

        root.add(schemeSwitch(root));
        final PFlowPanel grid = Element.newPFlowPanel();
        grid.addStyleName("wad-grid");
        grid.add(buttons(), overlays(), tabs(), tree(), split(), values(), disclosure());
        root.add(grid, orderTicket());
        p.add(root);
        p.add(codeToggle("loading the library", Source.code(WebAwesomePage.class, "load")));
        p.add(codeToggle("buttons, badges, tags, avatars", Source.code(WebAwesomePage.class, "buttons")));
        p.add(codeToggle("a dialog and a drawer", Source.code(WebAwesomePage.class, "overlays")));
        p.add(codeToggle("tabs", Source.code(WebAwesomePage.class, "tabs")));
        p.add(codeToggle("a tree", Source.code(WebAwesomePage.class, "tree")));
        p.add(codeToggle("a split panel", Source.code(WebAwesomePage.class, "split")));
        p.add(codeToggle("a slider, a rating, a progress bar and a ring", Source.code(WebAwesomePage.class, "values")));
        p.add(codeToggle("details, a tooltip, a callout and a toast", Source.code(WebAwesomePage.class, "disclosure")));
        p.add(codeToggle("the order ticket's fields", Source.code(WebAwesomePage.class, "fields")));
        p.add(codeToggle("the binding", Source.code(WebAwesomePage.class, "binder")));
        p.add(codeToggle("the adapters of the form controls", Source.code(com.ponysdk.sample.client.wa.WaFields.class, "adapters")));
        return p;
    }

    /** Light, dark or the system's: the container's data-theme, set by the terminal with no round trip (ADR-0027). */
    private static PFlowPanel schemeSwitch(final PFlowPanel root) {
        final PFlowPanel bar = Element.newPFlowPanel();
        bar.addStyleName("wad-scheme");
        final PElement caption = Element.newDiv();
        caption.setInnerText("Colour scheme of this page's content:");
        bar.add(caption);
        add(bar, "System", root, null);
        add(bar, "Light", root, "light");
        add(bar, "Dark", root, "dark");
        return bar;
    }

    private static void add(final PFlowPanel bar, final String label, final PFlowPanel root, final String scheme) {
        final PButton button = Element.newPButton(label);
        button.addEventListener("click", PEventOptions.NONE.commands(scheme == null
            ? PCommands.NONE.removeAttribute(root, "data-theme") : PCommands.NONE.setAttribute(root, "data-theme", scheme)));
        bar.add(button);
    }

    // ── Cards ────────────────────────────────────────────────────────────────

    private static PFlowPanel card(final String title, final PWidget... content) {
        final PFlowPanel card = Element.newPFlowPanel();
        card.addStyleName("wad-card");
        card.add(sectionTitle(title));
        for (final PWidget widget : content) card.add(widget);
        return card;
    }

    private static PFlowPanel row(final PWidget... content) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("wad-row");
        for (final PWidget widget : content) row.add(widget);
        return row;
    }

    /** The text of an element's default slot. */
    private static PElement text(final String text) {
        final PElement span = Element.newPElement("span");
        span.setInnerText(text);
        return span;
    }

    private static PElement status(final String initial) {
        final PElement status = Element.newDiv();
        status.addStyleName("wad-status");
        status.setAttribute("role", "status");
        status.setInnerText(initial);
        return status;
    }

    // ── Buttons and small things ─────────────────────────────────────────────

    private static PFlowPanel buttons() {
        final PElement clicked = status("Click a button");
        // region buttons
        final PFlowPanel buttons = Element.newPFlowPanel();
        buttons.addStyleName("wad-row");
        for (final PWaButton.Variant variant : PWaButton.Variant.values()) {
            final PWaButton button = new PWaButton();
            button.setVariant(variant);
            button.add(text(variant.literal));
            // a DOM event of the element, by name: the server hears the click like any widget's
            button.addEventListener("click", PEventOptions.NONE, e -> clicked.setInnerText("Server: clicked " + variant.literal));
            buttons.add(button);
        }
        final PWaButton loading = new PWaButton();
        loading.setLoading(true);
        loading.setAppearance(PWaButton.Appearance.OUTLINED);
        loading.add(text("Loading"));

        final PWaBadge badge = new PWaBadge();
        badge.setVariant(PWaBadge.Variant.SUCCESS);
        badge.add(text("Filled"));
        final PWaTag tag = new PWaTag();
        tag.setWithRemove(true);
        tag.add(text("EURUSD"));
        tag.onWaRemove(detail -> clicked.setInnerText("Server: tag removed"));
        final PWaAvatar avatar = new PWaAvatar();
        avatar.setInitials("PS");
        avatar.setLabel("PonySDK");
        // endregion
        return card("Buttons, badges, tags, avatars", buttons, row(loading, badge, tag, avatar), clicked);
    }

    // ── A dialog and a drawer ────────────────────────────────────────────────

    private static PFlowPanel overlays() {
        final PElement closed = status("Open one: its close is reported here");
        // region overlays
        // The browser changes `open` itself (Escape, the close button, a click outside): the server's copy would be
        // stale, and setOpen(true) a second time would be skipped as "no change". A stateless property is always sent.
        final PWaDialog dialog = new PWaDialog();
        dialog.property("open").stateless();
        dialog.setLabel("Confirm the order");
        dialog.add(text("Sell 1 000 000 EURUSD at market?"));
        final PWaButton confirm = new PWaButton();
        confirm.setVariant(PWaButton.Variant.BRAND);
        confirm.setAttribute("data-dialog", "close"); // the library's own way to close its dialog
        confirm.add(text("Confirm"));
        dialog.slotFooter().add(confirm);
        dialog.onWaAfterHide(detail -> closed.setInnerText("Server: the dialog is closed"));

        final PWaDrawer drawer = new PWaDrawer();
        drawer.property("open").stateless();
        drawer.setLabel("Blotter filters");
        drawer.setPlacement(PWaDrawer.Placement.END);
        drawer.add(text("Filters go here."));
        drawer.onWaAfterHide(detail -> closed.setInnerText("Server: the drawer is closed"));

        final PWaButton openDialog = new PWaButton();
        openDialog.add(text("Open the dialog"));
        openDialog.addEventListener("click", PEventOptions.NONE, e -> dialog.setOpen(true));
        final PWaButton openDrawer = new PWaButton();
        openDrawer.setAppearance(PWaButton.Appearance.OUTLINED);
        openDrawer.add(text("Open the drawer"));
        openDrawer.addEventListener("click", PEventOptions.NONE, e -> drawer.setOpen(true));
        // endregion
        return card("Dialog and drawer", row(openDialog, openDrawer), closed, dialog, drawer);
    }

    // ── Tabs ─────────────────────────────────────────────────────────────────

    private static PFlowPanel tabs() {
        final PElement shown = status("Active tab: orders");
        // region tabs
        final PWaTabGroup group = new PWaTabGroup();
        for (final String name : new String[] { "orders", "fills", "risk" }) {
            final PWaTab tab = new PWaTab();
            tab.setPanel(name);
            tab.add(text(name.substring(0, 1).toUpperCase() + name.substring(1)));
            final PWaTabPanel panel = new PWaTabPanel();
            panel.setName(name);
            panel.add(text("The " + name + " of the desk."));
            group.slotNav().add(tab);
            group.add(panel);
        }
        group.setActive("orders");
        // the detail of the library's custom event: { name }
        group.onWaTabShow(detail -> shown.setInnerText("Active tab: " + detail.asJsonObject().getString("name")));
        // endregion
        return card("Tabs", group, shown);
    }

    // ── Tree ─────────────────────────────────────────────────────────────────

    private static PFlowPanel tree() {
        final PElement selected = status("Nothing selected");
        // region tree
        final PWaTree tree = new PWaTree();
        final PWaTreeItem desk = item("Desk", null);
        desk.setExpanded(true);
        desk.add(item("FX spot", "fx-spot"), item("FX forward", "fx-forward"), item("Rates", "rates"));
        tree.add(desk);
        // The event's detail holds the selected elements, which no JSON text can stand for: the terminal reads what the
        // server needs, by path, from the event itself
        tree.addEventListener("wa-selection-change", PEventOptions.fields("detail.selection.0.id"),
            e -> selected.setInnerText("Selected: " + e.getString("detail.selection.0.id")));
        // endregion
        return card("Tree", tree, selected);
    }

    private static PWaTreeItem item(final String label, final String id) {
        final PWaTreeItem item = new PWaTreeItem();
        if (id != null) item.setAttribute("id", id);
        item.add(text(label));
        return item;
    }

    // ── Split panel ──────────────────────────────────────────────────────────

    private static PFlowPanel split() {
        final PElement position = status("Drag the divider");
        // region split
        final PWaSplitPanel split = new PWaSplitPanel();
        split.addStyleName("wad-split");
        split.setPosition(35);
        split.slotStart().add(text("Watchlist"));
        split.slotEnd().add(text("Chart"));
        split.addEventListener("wa-reposition", PEventOptions.fields("target.position").debounce(java.time.Duration.ofMillis(150)),
            e -> position.setInnerText("Divider at " + Math.round(e.getNumber("target.position")) + " %"));
        // endregion
        return card("Split panel", split, position);
    }

    // ── Values: a slider, a rating, progress ─────────────────────────────────

    private static PFlowPanel values() {
        final PElement shown = status("Move the slider");
        // region values
        final PWaProgressBar bar = new PWaProgressBar();
        bar.setLabel("Fill ratio");
        bar.setValue(40);
        final PWaProgressRing ring = new PWaProgressRing();
        ring.setLabel("Fill ratio");
        ring.setValue(40);
        final PWaSlider slider = new PWaSlider();
        slider.setLabel("Fill ratio");
        slider.setMin(0);
        slider.setMax(100);
        slider.setWithTooltip(true);
        // A field of the Binder as well: WaFields reads target.value on change, and writes it
        final com.ponysdk.core.ui.basic.HasPValue<Double> ratio = WaFields.of(slider);
        ratio.setValue(40.0);
        ratio.addValueChangeHandler(e -> {
            bar.setValue(e.getData());
            ring.setValue(e.getData());
            shown.setInnerText("Server: fill ratio " + Math.round(e.getData()) + " %");
        });
        final PWaRating rating = new PWaRating();
        rating.setLabel("Execution quality");
        rating.setMax(5);
        final com.ponysdk.core.ui.basic.HasPValue<Double> stars = WaFields.of(rating);
        stars.addValueChangeHandler(e -> shown.setInnerText("Server: " + Math.round(e.getData()) + " star(s)"));
        // endregion
        return card("Slider, rating, progress", slider, row(bar), row(ring, rating), shown);
    }

    // ── Details, tooltip, callout, toast ─────────────────────────────────────

    private static PFlowPanel disclosure() {
        // region disclosure
        final PWaDetails details = new PWaDetails();
        details.setSummary("Settlement instructions");
        details.add(text("T+2, value date adjusted for the calendars of both currencies."));

        final PWaButton hint = new PWaButton();
        hint.setAppearance(PWaButton.Appearance.OUTLINED);
        hint.forceDomId();
        hint.add(text("Hover for a tooltip"));
        final PWaTooltip tooltip = new PWaTooltip();
        tooltip.setFor(String.valueOf(hint.getID()));
        tooltip.add(text("Rendered by the library, positioned by its popup"));

        final PWaCallout callout = new PWaCallout();
        callout.setVariant(PWaCallout.Variant.WARNING);
        callout.add(text("The market closes in 5 minutes."));

        final PWaToast toast = new PWaToast();
        final PWaButton notify = new PWaButton();
        notify.setVariant(PWaButton.Variant.SUCCESS);
        notify.add(text("Show a toast"));
        notify.addEventListener("click", PEventOptions.NONE, e -> toast.create("Order sent",
            Json.createObjectBuilder().add("variant", "success").add("duration", 3000).build()));
        // endregion
        return card("Details, tooltip, callout, toast", details, row(hint, tooltip, notify), callout, toast);
    }

    // ── The order ticket, bound with the Binder ──────────────────────────────

    private static PFlowPanel orderTicket() {
        final PFlowPanel form = Element.newPFlowPanel();
        form.addStyleName("wad-ticket");
        form.setAttribute("role", "group");
        form.setAttribute("aria-label", "Order ticket");
        form.add(sectionTitle("Order ticket, bound to a bean"));

        // region fields
        final PWaInput instrument = new PWaInput();
        instrument.setLabel("Instrument");
        instrument.setHint("Six capital letters");
        final PWaSelect side = new PWaSelect();
        side.setLabel("Side");
        for (final Side s : Side.values()) side.add(option(s.name(), s.name().charAt(0) + s.name().substring(1).toLowerCase()));
        final PWaRadioGroup type = new PWaRadioGroup();
        type.setLabel("Type");
        type.setOrientation(PWaRadioGroup.Orientation.HORIZONTAL);
        for (final Type t : Type.values()) {
            final PWaRadio radio = new PWaRadio();
            radio.setValue(t.name());
            radio.add(text(t.name().charAt(0) + t.name().substring(1).toLowerCase()));
            type.add(radio);
        }
        final PWaInput quantity = new PWaInput();
        quantity.setLabel("Quantity");
        final PWaInput limitPrice = new PWaInput();
        limitPrice.setLabel("Limit price");
        final PWaInput valueDate = new PWaInput();
        valueDate.setLabel("Value date");
        valueDate.setType(PWaInput.Type.DATE);
        final PWaTextarea comment = new PWaTextarea();
        comment.setLabel("Comment");
        comment.setRows(2);
        final PWaCheckbox confirmed = new PWaCheckbox();
        confirmed.add(text("I confirm the details"));
        // endregion
        final PElement problems = Element.newDiv(); // always there: what is put in an alert is announced
        problems.addStyleName("forms-problems");
        problems.setAttribute("role", "alert");
        final PElement saved = Element.newDiv();
        saved.addStyleName("forms-saved");
        saved.setAttribute("aria-live", "polite");
        saved.setInnerText("Nothing saved yet.");
        final PWaButton save = new PWaButton();
        save.setVariant(PWaButton.Variant.BRAND);
        save.add(text("Save"));
        final PWaButton reset = new PWaButton();
        reset.setAppearance(PWaButton.Appearance.OUTLINED);
        reset.add(text("Reset"));

        // region binder
        // The same bean and the same binder as the Forms page: the fields are Web Awesome's, through WaFields
        final Binder<OrderTicket> binder = new Binder<>(OrderTicket.class);
        binder.forField(WaFields.of(instrument)).bind(OrderTicket::getInstrument, OrderTicket::setInstrument);
        binder.forField(WaFields.of(side)).withConverter(constant(Side::valueOf, Side::name))
            .bind(OrderTicket::getSide, OrderTicket::setSide);
        binder.forField(WaFields.of(type)).withConverter(constant(Type::valueOf, Type::name))
            .bind(OrderTicket::getType, OrderTicket::setType);
        binder.forField(WaFields.of(quantity)).withConverter(Converters.toInteger("Not a whole number"))
            .bind(OrderTicket::getQuantity, OrderTicket::setQuantity);
        binder.forField(WaFields.of(limitPrice)).withConverter(Converters.toBigDecimal("Not a price"))
            .bind(OrderTicket::getLimitPrice, OrderTicket::setLimitPrice);
        binder.forField(WaFields.of(valueDate)).withConverter(Converters.toLocalDate("Not a date"))
            .bind(OrderTicket::getValueDate, OrderTicket::setValueDate);
        binder.forField(WaFields.of(comment)).bind(OrderTicket::getComment, OrderTicket::setComment);
        binder.forField(WaFields.of(confirmed)).bind(OrderTicket::isConfirmed, OrderTicket::setConfirmed);

        final OrderTicket order = new OrderTicket();
        order.setInstrument("EURUSD");
        order.setQuantity(1_000);
        order.setValueDate(LocalDate.now());
        binder.readBean(order);
        binder.carry("wa-ticket"); // the typed values survive a move of the page, as on the Forms page
        // endregion

        binder.addStatusChangeListener(s -> problems.setInnerText(String.join(" ", s.beanErrors())));
        save.addEventListener("click", PEventOptions.NONE, e -> {
            if (binder.writeBean(order)) {
                problems.setInnerText("");
                saved.setInnerText("Saved: " + order);
            } else if (binder.getStatus().beanErrors().isEmpty()) {
                problems.setInnerText("Some fields are not valid: fix the ones marked.");
            }
        });
        reset.addEventListener("click", PEventOptions.NONE, e -> {
            binder.readBean(order);
            problems.setInnerText("");
        });

        final PFlowPanel actions = row(save, reset);
        form.add(instrument, side, type, quantity, limitPrice, valueDate, comment, confirmed, problems, actions, saved);
        return form;
    }

    private static PWaOption option(final String value, final String label) {
        final PWaOption option = new PWaOption();
        option.setValue(value);
        option.add(text(label));
        return option;
    }

    /** An enum constant by its name; an empty choice is no constant. */
    private static <E extends Enum<E>> Converter<String, E> constant(final java.util.function.Function<String, E> of,
                                                                     final java.util.function.Function<E, String> name) {
        return Converter.of(s -> s == null || s.isEmpty() ? null : of.apply(s), m -> m == null ? "" : name.apply(m), "Not a choice");
    }
}
