/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.SessionAdmin;
import com.ponysdk.core.server.application.SessionInfo;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.impl.java.ShowcaseMain;

/**
 * Reconnection: a second session of the showcase runs in a frame, in the same HTTP session, and the page reads it and
 * acts on it through {@link SessionAdmin} (ADR-0001). The frame opens {@code #reconnection-client-<page context id>},
 * which {@link UISampleEntryPoint} hands to {@link #buildClient}.
 */
final class ReconnectionPage {

    static final String CLIENT_TOKEN = "reconnection-client-";
    private static final int ROWS_PER_PUSH = 1000;
    private static final int CELLS = 40;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Shared by the page and the session in its frame (an Application attribute): the frame's id, the rows ordered. */
    static final class Client {
        volatile int id = -1;
        final AtomicInteger rowsToAdd = new AtomicInteger();
    }

    private ReconnectionPage() {
    }

    private static String key(final int pageId) {
        return "showcase.reconnection." + pageId;
    }

    // ---- The page: the frame, and the server's view of it --------------------------------------------------------

    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Reconnection", "The frame runs a second session of this showcase. The server panel shows that "
            + "session as the server sees it, read and driven through SessionAdmin: cut its socket, keep it away, watch "
            + "what the server holds for it, let it back; or move it, as a server that stops moves its pages to the others.");
        final Application application = uiContext.getApplication();
        if (application == null) {
            p.add(el("rc-note", "This page needs an HTTP session."));
            return p;
        }
        final Client client = new Client();
        application.setAttribute(key(uiContext.getID()), client);

        final PFlowPanel layout = Element.newPFlowPanel();
        layout.addStyleName("rc-layout");
        final PElement frame = Element.newPElement("iframe");
        frame.addStyleName("rc-frame");
        frame.setAttribute("title", "A second session of the showcase");
        frame.setAttribute("src", "./#" + CLIENT_TOKEN + uiContext.getID());
        final PFlowPanel frameCard = Element.newPFlowPanel();
        frameCard.addStyleName("rc-card");
        frameCard.add(el("rc-card-title", "Client · a second session, in a frame"), frame);

        // region admin
        final SessionAdmin admin = SessionAdmin.of(uiContext); // checked by the SessionAccessPolicy
        // endregion
        final ServerView view = new ServerView(admin, client);
        layout.add(frameCard, view.card);
        p.add(layout);

        p.add(sectionTitle("The API behind the buttons"));
        p.add(codeSnippet("The buttons: ReconnectionPage.java", "java",
            Source.code(ReconnectionPage.class, "admin") + Source.code(ReconnectionPage.class, "buttons")));
        p.add(codeSnippet("The server panel, each second", "java", Source.code(ReconnectionPage.class, "read")));
        p.add(el("ov-note", "By default a caller acts only on the sessions of its own HTTP session: this page reaches its "
            + "frame, never another visitor. ApplicationConfiguration.setSessionAccessPolicy opens more, for an operator "
            + "console. Every action is logged on com.ponysdk.audit.SessionAdmin."));

        p.add(sectionTitle("When a server stops"));
        p.add(el("ov-note", "Behind a load balancer, a server asked to stop (a deployment, a scale-in, a spot "
            + "interruption) moves its pages to the other servers before it stops: each session is destroyed, its page "
            + "reloads, and the balancer, which no longer sends anything to that server, sends it to another one. Move "
            + "server does that to the frame's session. What the page's address keeps comes back: click a price in the "
            + "frame, then Move server. Two servers behind a balancer on this machine: ./gradlew :sample:runTwoServers, "
            + "then stop one (docs/guide/deployment.md)."));
        p.add(codeSnippet("The drain on SIGTERM: ShowcaseMain.java", "java", Source.code(ShowcaseMain.class, "drain")));
        p.add(el("ov-note", "The balancer's health check is the application's path followed by /health: 200 UP while the "
            + "server takes pages, 503 DRAINING from the start of a drain. No session, not cached."));
        final PAnchor health = Element.newPAnchor("Open this server's health check", "health");
        health.setAttribute("target", "_blank");
        p.add(health);
        p.add(codeSnippet("The watched price, in the page's address: ReconnectionPage.java", "java",
            Source.code(ReconnectionPage.class, "watch")));
        p.add(el("ov-note", "What the address cannot hold, the application declares for a move: the frame's note goes to "
            + "the next server sealed (AES-GCM under a key every server shares), carried by the page itself. No shared "
            + "store; the key turns it on (ApplicationConfiguration.setStateTransferKey)."));
        p.add(codeSnippet("The note, carried by a move: ReconnectionPage.java", "java", Source.code(ReconnectionPage.class, "carry")));

        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            view.refresh();
        }, Duration.ofMillis(200)));
        return p;
    }

    private static final class ServerView {

        final PFlowPanel card = Element.newPFlowPanel();
        private final SessionAdmin admin;
        private final Client client;

        private final PElement state = el("rc-state", "Waiting for the frame");
        private final PElement session = el("rc-session", "");
        private final PElement heldFill = el("rc-fill", "");
        private final PElement heldValue = el("rc-gauge-value", "—");
        private final PElement heldNote = el("rc-gauge-note", "");
        private final PElement windowFill = el("rc-fill", "");
        private final PElement windowValue = el("rc-gauge-value", "—");
        private final PElement windowNote = el("rc-gauge-note", "");
        private final PFlowPanel log = Element.newPFlowPanel();
        private final PButton disconnect = Element.newPButton("Disconnect");
        private final PButton reconnect = Element.newPButton("Reconnect");
        private final PButton push = Element.newPButton("Push 1 000 rows");
        private final PButton move = Element.newPButton("Move server");
        private final PButton end = Element.newPButton("End session");

        private int shownId = -1;
        private SessionInfo last;
        private boolean ending;
        private boolean moving;

        ServerView(final SessionAdmin admin, final Client client) {
            this.admin = admin;
            this.client = client;
            card.addStyleName("rc-card");
            card.addStyleName("rc-server");
            final PFlowPanel head = Element.newPFlowPanel();
            head.addStyleName("rc-head");
            head.add(el("rc-card-title", "Server · what it knows of that session"), state);
            card.add(head, session);

            final PFlowPanel buttons = Element.newPFlowPanel();
            buttons.addStyleName("rc-buttons");
            disconnect.addStyleName("primary");
            buttons.add(disconnect, reconnect, push, move, end);
            card.add(buttons);
            card.add(gauge("Instructions held for the client", heldFill, heldValue, heldNote));
            card.add(gauge("Reconnection window", windowFill, windowValue, windowNote));
            log.addStyleName("rc-log");
            card.add(log);

            // region buttons
            disconnect.addClickHandler(e -> act(id -> {
                admin.holdReconnection(id, true);
                admin.dropConnection(id);
                event("Disconnect: reconnection held, socket dropped");
            }));
            reconnect.addClickHandler(e -> act(id -> {
                admin.holdReconnection(id, false);
                event("Reconnect: hold released, the client comes back at its next attempt (every 2 s)");
            }));
            push.addClickHandler(e -> {
                client.rowsToAdd.addAndGet(ROWS_PER_PUSH);
                event("The frame's application adds " + count(ROWS_PER_PUSH) + " rows: new widgets, which compaction cannot merge");
            });
            move.addClickHandler(e -> act(id -> {
                moving = true;
                admin.move(id); // what Drain does to each page of a server that stops
                event("Move server: destroyed as a draining server destroys its pages; the frame reloads, and gets its "
                    + "watched price back from its address, its note from the state the move carried");
            }));
            end.addClickHandler(e -> act(id -> {
                ending = true;
                admin.destroy(id);
                event("End session: destroyed, the frame is told and reloads");
            }));
            // endregion
            render(null);
        }

        private static PFlowPanel gauge(final String label, final PElement fill, final PElement value, final PElement note) {
            final PFlowPanel g = Element.newPFlowPanel();
            g.addStyleName("rc-gauge");
            final PFlowPanel top = Element.newPFlowPanel();
            top.addStyleName("rc-gauge-top");
            top.add(el("rc-gauge-label", label), value);
            final PFlowPanel track = Element.newPFlowPanel();
            track.addStyleName("rc-track");
            track.add(fill);
            g.add(top, track, note);
            return g;
        }

        private void act(final java.util.function.IntConsumer action) {
            final int id = client.id;
            if (id < 0) return;
            try {
                action.accept(id);
            } catch (final NoSuchElementException | SecurityException e) {
                event(e.getMessage());
            }
            refresh();
        }

        void refresh() {
            final int id = client.id;
            // region read: what the server holds for the session, read each second
            final Optional<SessionInfo> now = id < 0 ? Optional.empty() : admin.session(id);
            // endregion
            if (now.isEmpty()) {
                if (last != null) ended();
                render(null);
                return;
            }
            final SessionInfo info = now.get();
            // the frame may already be in its next session when the panel reads again: the previous one ended first
            if (last != null && last.id() != info.id()) ended();
            if (info.id() != shownId) {
                event(shownId < 0 ? "Session #" + info.id() + " is up in the frame" : "New session #" + info.id() + " in the frame");
                shownId = info.id();
                last = null;
            }
            if (last != null && last.state() == SessionInfo.State.CONNECTED && info.state() == SessionInfo.State.SUSPENDED) {
                event("Socket closed: the client is away, the server holds what it writes for it");
            } else if (last != null && last.state() == SessionInfo.State.SUSPENDED && info.state() == SessionInfo.State.CONNECTED) {
                final SessionInfo.Suspension s = last.suspension();
                event(String.format(Locale.ROOT, "Client back after %.1f s: about %s instructions replayed, of %s written while away",
                    (System.currentTimeMillis() - s.since()) / 1000.0, count(s.held()), count(s.written())));
            }
            last = info;
            render(info);
        }

        private void ended() {
            event("Session #" + last.id() + " destroyed: " + endReason() + ". The frame reloads into a new session.");
            last = null;
            ending = false;
            moving = false;
        }

        private String endReason() {
            if (moving) return "moved, as a server being drained moves each of its pages";
            if (ending) return "ended from this page";
            final SessionInfo.Suspension s = last.suspension();
            if (s == null) return "it ended";
            return System.currentTimeMillis() - s.since() >= s.timeoutMs() ? "its reconnection window expired"
                    : "its held instructions went over the cap";
        }

        private void render(final SessionInfo info) {
            final boolean alive = info != null;
            final boolean away = alive && info.state() == SessionInfo.State.SUSPENDED;
            final boolean held = alive && info.reconnectionHeld();
            state.setInnerText(!alive ? (shownId < 0 ? "Waiting for the frame" : "Destroyed")
                    : away ? held ? "Client away · held" : "Client away · reconnecting" : "Connected");
            tone(state, !alive ? "rc-bad" : away ? "rc-warn" : "rc-ok");
            session.setInnerText(alive ? String.format(Locale.ROOT, "Session #%d · roundtrip %.0f ms", info.id(), info.latencyMs())
                    : shownId < 0 ? "" : "Session #" + shownId + " is gone");

            disconnect.setEnabled(alive && !held);
            reconnect.setEnabled(held);
            push.setEnabled(alive);
            move.setEnabled(alive);
            end.setEnabled(alive);

            final SessionInfo.Suspension s = away ? info.suspension() : null;
            if (s == null) {
                bar(heldFill, 0);
                heldValue.setInnerText("0");
                heldNote.setInnerText("Nothing to hold: the client receives every instruction as it is written.");
                bar(windowFill, 0);
                windowValue.setInnerText("—");
                windowNote.setInnerText("Opens when the client goes away; the session is destroyed when it closes.");
                return;
            }
            bar(heldFill, 100.0 * s.held() / s.maxEntries());
            heldValue.setInnerText(count(s.held()) + " / " + count(s.maxEntries()));
            heldNote.setInnerText(count(s.written()) + " written since the drop; compaction merged "
                + count(s.written() - s.held()) + " into the last value of each property. Over the cap, the session is destroyed.");
            final long elapsed = System.currentTimeMillis() - s.since();
            final long left = Math.max(0, s.timeoutMs() - elapsed);
            bar(windowFill, 100.0 * elapsed / s.timeoutMs());
            windowValue.setInnerText(String.format(Locale.ROOT, "%.1f s left of %d s", left / 1000.0, s.timeoutMs() / 1000));
            windowNote.setInnerText(held ? "Held: its reconnection attempts are refused until Reconnect."
                    : "Its next attempt resumes the session and replays what is held.");
        }

        private static void bar(final PElement fill, final double pct) {
            final double p = Math.max(0, Math.min(100, pct));
            fill.setStyleProperty("width", String.format(Locale.ROOT, "%.1f%%", p));
            tone(fill, p < 50 ? "rc-ok" : p < 85 ? "rc-warn" : "rc-bad");
        }

        private static void tone(final PElement e, final String tone) {
            for (final String t : new String[] { "rc-ok", "rc-warn", "rc-bad" }) {
                if (t.equals(tone)) e.addStyleName(t);
                else e.removeStyleName(t);
            }
        }

        private void event(final String text) {
            final PElement line = el("rc-log-line", "");
            line.add(el("rc-log-time", LocalTime.now().format(TIME)), el("rc-log-text", text));
            log.insert(line, 0);
            while (log.getWidgetCount() > 8) log.remove(log.getWidgetCount() - 1);
        }

        private static String count(final long n) {
            return String.format(Locale.ROOT, "%,d", n).replace(',', ' ');
        }
    }

    // ---- The session in the frame ---------------------------------------------------------------------------------

    /** The frame's session: a price grid ticking every 100 ms, and rows added on the page's order. */
    static void buildClient(final UIContext uiContext, final String token) {
        // #reconnection-client-<page id>[/<watched cell>]: what the address keeps survives a move to another server
        final String[] parts = token.substring(CLIENT_TOKEN.length()).split("/");
        int pageId = -1;
        int restored = -1;
        try {
            pageId = Integer.parseInt(parts[0]);
            if (parts.length > 1) restored = Integer.parseInt(parts[1]);
        } catch (final NumberFormatException e) {
            // not opened by the page: says so below
        }
        final Application application = uiContext.getApplication();
        final Client client = application == null ? null : application.getAttribute(key(pageId));
        final PFlowPanel root = Element.newPFlowPanel();
        root.addStyleName("rc-client");
        PWindow.getMain().add(root);
        if (client == null) {
            root.add(el("rc-note", "This session is meant for the frame of the Reconnection page."));
            return;
        }
        client.id = uiContext.getID();

        root.add(el("rc-client-title", "Session #" + uiContext.getID()),
            el("rc-note", "Prices tick every 100 ms, whether the client is there or not. Click a price to watch it."));
        final PElement watching = el("rc-watching", "Watching nothing");
        final PFlowPanel grid = Element.newPFlowPanel();
        grid.addStyleName("rc-grid");
        final PLabel[] cells = new PLabel[CELLS];
        final double[] prices = new double[CELLS];
        for (int i = 0; i < CELLS; i++) {
            prices[i] = 100 + ThreadLocalRandom.current().nextDouble(50);
            cells[i] = Element.newPLabel(String.format(Locale.ROOT, "%.2f", prices[i]));
            cells[i].addStyleName("rc-cell");
            grid.add(cells[i]);
        }
        // region watch: the watched price kept in the page's address, which a reload keeps
        final int page = pageId;
        final int[] watched = { -1 };
        final java.util.function.IntConsumer watch = i -> {
            if (watched[0] >= 0) cells[watched[0]].removeStyleName("rc-watched");
            watched[0] = i;
            cells[i].addStyleName("rc-watched");
            uiContext.getHistory().newItem(CLIENT_TOKEN + page + "/" + i, false);
        };
        for (int i = 0; i < CELLS; i++) {
            final int cell = i;
            cells[i].addDomHandler((PClickHandler) e -> {
                watch.accept(cell);
                watching.setInnerText("Watching price " + (cell + 1) + ": kept in the page's address");
            }, PClickEvent.TYPE);
        }
        if (restored >= 0 && restored < CELLS) {
            watch.accept(restored);
            watching.setInnerText("Watching price " + (restored + 1) + ": restored from the page's address by session #"
                + uiContext.getID());
        }
        // endregion
        // region carry: a note typed in the frame, carried to the next server by a move (ADR-0018)
        final PTextBox note = Element.newPTextBox(uiContext.getMovedState().getString("note", ""));
        note.setPlaceholder("A note: Move server carries it");
        uiContext.onMove(state -> state.putString("note", note.getText()));
        // endregion
        note.addStyleName("rc-note-box");
        final PElement carried = el("rc-carried", uiContext.getMovedState().has("note")
            ? "Note carried from the previous server, sealed, through the page" : "");
        root.add(watching, note, carried);
        final PFlowPanel rowsHead = Element.newPFlowPanel();
        rowsHead.addStyleName("rc-rows-head");
        final PLabel rowCount = Element.newPLabel("0 rows");
        final PButton clear = Element.newPButton("Clear");
        rowsHead.add(rowCount, clear);
        final PFlowPanel rows = Element.newPFlowPanel();
        rows.addStyleName("rc-rows");
        root.add(grid, rowsHead, rows);
        PWindow.getMain().add(reconnectionNotice());

        final int[] added = { 0 };
        clear.addClickHandler(e -> {
            rows.clear();
            added[0] = 0;
            rowCount.setText("0 rows");
        });
        PScheduler.scheduleAtFixedRate(() -> {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < CELLS; i++) {
                prices[i] = Math.max(1, prices[i] + random.nextGaussian() * 0.05);
                cells[i].setText(String.format(Locale.ROOT, "%.2f", prices[i]));
            }
            // Only the frame's current session takes the rows: a session the frame left (it reloaded) keeps ticking
            // until the server destroys it
            final int n = client.id == uiContext.getID() ? client.rowsToAdd.getAndSet(0) : 0;
            for (int k = 0; k < n; k++) {
                rows.add(Element.newPLabel("row " + ++added[0] + " · " + String.format(Locale.ROOT, "%.2f", prices[k % CELLS])));
            }
            if (n > 0) rowCount.setText(added[0] + " rows");
        }, Duration.ofMillis(100));
    }
}
