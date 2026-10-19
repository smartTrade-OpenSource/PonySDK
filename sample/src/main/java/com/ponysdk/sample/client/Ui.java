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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Costs.*;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Small building blocks shared by the showcase pages: cards, titles, code snippets, example cards. */
final class Ui {

    private static final Logger log = LoggerFactory.getLogger(Ui.class);

    private Ui() {
    }

    static PFlowPanel binaryCtrlGroup(final String label, final PFlowPanel content) {
        final PFlowPanel group = Element.newPFlowPanel();
        final PElement caption = Element.newDiv();
        caption.setAttribute("style", "font-size:12px;font-weight:500;color:var(--text-3);margin-bottom:6px");
        caption.setInnerText(label);
        group.add(caption);
        group.add(content);
        return group;
    }

    static void styleBinaryCtrl(final PButton button, final boolean active) {
        // A segmented choice: the selected value is filled with the accent
        button.setAttribute("style", "margin-right:6px;min-width:44px;font-variant-numeric:tabular-nums;"
            + (active ? "background:var(--accent);border-color:var(--accent);color:#fff;" : ""));
    }

    static String bytes(final double v) {
        if (v < 0) return "n/a";
        return v < 10 ? String.format(java.util.Locale.ROOT, "%.1f B", v) : String.format(java.util.Locale.ROOT, "%.0f B", v);
    }

    /** A small labelled box used to fill the layout demos. */
    static PLabel chip(final String text) {
        final PLabel l = Element.newPLabel(text);
        l.addStyleName("lay-chip");
        return l;
    }

    /** Layout panels need a definite size: give them a framed 180 px box. */
    static PWidget sized(final PWidget w) {
        w.setHeight("180px");
        w.setWidth("min(560px,100%)");
        w.addStyleName("lay-frame");
        return w;
    }

    /**
     * Nobody is looking (background tab, minimised window) or the connection is suspended (its writes would
     * pile up in the reconnection buffer): periodic showcase work skips the tick, no work and no bytes.
     */
    static boolean idle() {
        return UIContext.get().isSuspended() || !PWindow.getMain().isShown();
    }

    /**
     * Shown by the terminal while the connection is lost (it writes the message into #reconnecting), hidden once the
     * session resumes (showcase.css styles it).
     */
    static PFlowPanel reconnectionNotice() {
        final PFlowPanel reconnection = Element.newPFlowPanel();
        reconnection.setAttribute("id", "reconnection");
        final PElement reconnecting = Element.newDiv();
        reconnecting.setAttribute("id", "reconnecting");
        reconnecting.setAttribute("role", "status");
        reconnection.add(reconnecting);
        return reconnection;
    }

    static PElement el(final String cssClass, final String text) {
        final PElement e = Element.newDiv();
        for (final String c : cssClass.split(" ")) e.addStyleName(c);
        if (!text.isEmpty()) e.setInnerText(text);
        return e;
    }

    static PFlowPanel perfCard(final PElement... elements) {
        final PFlowPanel c = Element.newPFlowPanel();
        c.addStyleName("perf-card");
        for (final PElement e : elements) c.add(e);
        return c;
    }

    static PFlowPanel highlightBox(final String icon, final String title, final String desc) {
        final PFlowPanel box = Element.newPFlowPanel();
        box.addStyleName("highlight-box");
        final PFlowPanel txt = Element.newPFlowPanel();
        final PElement ttl = Element.newDiv(); ttl.addStyleName("highlight-title"); ttl.setInnerText(title);
        final PElement dsc = Element.newDiv(); dsc.addStyleName("highlight-desc"); dsc.setInnerText(desc);
        txt.add(ttl, dsc);
        box.add(txt);
        return box;
    }

    static PFlowPanel codeSnippet(final String title, final String lang, final String html) {
        final PFlowPanel wrap = Element.newPFlowPanel();
        wrap.addStyleName("code-snippet");
        final PFlowPanel hdr = Element.newPFlowPanel();
        hdr.addStyleName("code-header");
        final PElement d1 = Element.newDiv(); d1.addStyleName("code-dot"); d1.setAttribute("style", "background:#ff5f57");
        final PElement d2 = Element.newDiv(); d2.addStyleName("code-dot"); d2.setAttribute("style", "background:#febc2e");
        final PElement d3 = Element.newDiv(); d3.addStyleName("code-dot"); d3.setAttribute("style", "background:#28c840");
        final PElement t = Element.newDiv(); t.addStyleName("code-title"); t.setInnerText(title);
        final PElement l = Element.newDiv(); l.addStyleName("code-lang"); l.setInnerText(lang);
        hdr.add(d1, d2, d3, t, l);
        final PElement body = Element.newDiv();
        body.addStyleName("code-body");
        body.setInnerHTML(html);
        wrap.add(hdr, body);
        return wrap;
    }

    static String kw(final String s) { return "<span class='code-kw'>" + s + "</span>"; }

    static String ty(final String s) { return "<span class='code-type'>" + s + "</span>"; }

    static String st(final String s) { return "<span class='code-str'>\"" + s + "\"</span>"; }

    static String cm(final String s) { return "<span class='code-comment'>// " + s + "</span>"; }

    static String mt(final String s) { return "<span class='code-method'>" + s + "</span>"; }

    static String ln(final String s) { return "<span class='code-line'>" + s + "</span>"; }

    // ── Interactive example cards (a live widget + the exact Java behind it) ───

    /** A card pairing a live, interactive widget with the Java source that built it (Preview / Code). */
    static PFlowPanel exampleCard(final String title, final String desc,
                                          final PWidget preview, final String codeHtml) {
        return exampleCard(title, desc, preview, codeHtml, null);
    }

    static PFlowPanel exampleCard(final String title, final String desc,
                                          final PWidget preview, final String codeHtml, final CostSpec cost) {
        final PFlowPanel card = Element.newPFlowPanel();
        card.addStyleName("pony-ex");

        final PFlowPanel head = Element.newPFlowPanel();
        head.addStyleName("pony-ex-head");
        final PFlowPanel titleWrap = Element.newPFlowPanel();
        titleWrap.addStyleName("pony-ex-titlewrap");
        final PElement t = Element.newDiv(); t.addStyleName("pony-ex-title"); t.setInnerText(title);
        final PElement d = Element.newDiv(); d.addStyleName("pony-ex-desc"); d.setInnerText(desc);
        titleWrap.add(t, d);

        final PFlowPanel seg = Element.newPFlowPanel();
        seg.addStyleName("pony-ex-seg");
        final PButton bPreview = Element.newPButton("Preview");
        final PButton bCode = Element.newPButton("Code");
        bPreview.addStyleName("pony-ex-seg-on");
        seg.add(bPreview, bCode);
        final PButton bCost = cost != null ? Element.newPButton("Cost") : null;
        if (bCost != null) seg.add(bCost);
        head.add(titleWrap, seg);

        final PFlowPanel previewBody = Element.newPFlowPanel();
        previewBody.addStyleName("pony-ex-body");
        previewBody.addStyleName("pony-ex-preview");
        previewBody.add(preview);

        final PFlowPanel codeBody = Element.newPFlowPanel();
        codeBody.addStyleName("pony-ex-code");
        final PElement body = Element.newDiv();
        body.addStyleName("code-body");
        body.setInnerHTML(codeHtml);
        codeBody.add(body);
        codeBody.setVisible(false);
        final PFlowPanel costBody = cost != null ? costBody(cost) : null;
        if (costBody != null) costBody.setVisible(false);

        // The toggle itself runs on the server (setVisible → binary delta) — the value prop, demoed by the demo.
        final PWidget[] bodies = { previewBody, codeBody, costBody };
        final PButton[] buttons = { bPreview, bCode, bCost };
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] == null) continue;
            final int sel = i;
            buttons[i].addClickHandler(e -> {
                for (int j = 0; j < buttons.length; j++) {
                    if (buttons[j] == null) continue;
                    bodies[j].setVisible(j == sel);
                    if (j == sel) buttons[j].addStyleName("pony-ex-seg-on");
                    else buttons[j].removeStyleName("pony-ex-seg-on");
                }
            });
        }

        card.add(head, previewBody, codeBody);
        if (costBody != null) card.add(costBody);
        return card;
    }

    /** A live "server received…" readout — proves each interaction is handled in Java, server-side. */
    static PElement echoIdle(final String text) {
        final PElement e = Element.newDiv();
        e.addStyleName("pony-ex-echo");
        e.addStyleName("pony-ex-echo-idle");
        e.setInnerText(text);
        return e;
    }

    static void echoSet(final PElement echo, final String text) {
        echo.removeStyleName("pony-ex-echo-idle");
        echo.setInnerText(text);
    }

    /** Stacks a live control above its server-echo readout. */
    static PFlowPanel previewWithEcho(final PWidget control, final PElement echo) {
        final PFlowPanel wrap = Element.newPFlowPanel();
        wrap.setStyleProperty("display", "flex");
        wrap.setStyleProperty("flex-direction", "column");
        wrap.setStyleProperty("gap", "14px");
        wrap.setStyleProperty("align-items", "flex-start");
        wrap.add(control);
        wrap.add(echo);
        return wrap;
    }

    static String nu(final String s) { return "<span class='code-num'>" + s + "</span>"; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static PFlowPanel section() {
        return Element.newPFlowPanel();
    }

    static void pageTitle(final PFlowPanel p, final String title, final String desc) {
        final PElement h = Element.newDiv(); h.addStyleName("pony-page-title"); h.setInnerText(title);
        final PElement d = Element.newDiv(); d.addStyleName("pony-page-desc"); d.setInnerText(desc);
        p.add(h, d);
    }

    static PElement sectionTitle(final String text) {
        final PElement h = Element.newDiv();
        h.addStyleName("pony-section-title");
        h.setInnerText(text);
        return h;
    }

    static PFlowPanel labeled(final String label, final PWidget widget) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("pony-row");
        final PElement lbl = Element.newDiv();
        lbl.addStyleName("pony-label");
        lbl.setInnerText(label);
        row.add(lbl);
        row.add(widget);
        final String costName = ROW_COSTS.get(label);
        if (costName != null) row.add(rowCost(costSpec(costName)));
        return row;
    }

    static PFlowPanel labeled(final String label, final IsPWidget widget) {
        return labeled(label, widget.asWidget());
    }

    /** Widgets stacked vertically (an example's preview). */
    static PFlowPanel stack(final PWidget... widgets) {
        final PFlowPanel p = Element.newPFlowPanel();
        p.addStyleName("pony-stack");
        for (final PWidget w : widgets) p.add(w);
        return p;
    }

    static void updateLabel(final PLabel label, final String text) {
        log.debug("Update label {}", text);
        label.setText("Server counter: " + text);
    }
}
