/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.binder.Binder;
import com.ponysdk.core.ui.binder.Converters;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;

/** A form's fields carried by a move to the next server (Binder.carry, ADR-0018): a sealed state, two contexts. */
public class BinderCarryTest {

    private static final byte[] KEY = new byte[32];

    private final List<Application> applications = new ArrayList<>();
    private WebSocket socket;

    @AfterEach
    public void tearDown() {
        applications.forEach(SessionManager.get()::unregisterApplication);
        UIContext.remove();
    }

    private UIContext context(final Map<String, List<String>> parameters) {
        socket = Mockito.mock(WebSocket.class);
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setApplicationID("orders");
        config.setStateTransferKey(KEY);
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        final Application application = new Application("orders", Mockito.mock(HttpSession.class), config);
        SessionManager.get().registerApplication(application);
        applications.add(application);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new UpgradeRequestData(parameters, "test", Mockito.mock(HttpSession.class)));
        final TxnContext txn = new TxnContext(socket);
        txn.setApplication(application);
        final UIContext uiContext = new UIContext(socket, txn, config);
        application.registerUIContext(uiContext);
        return uiContext;
    }

    /** Moves the context and gives what its client would offer the next server. */
    private Map<String, List<String>> moved(final UIContext uiContext) throws InterruptedException {
        final CountDownLatch destroyed = new CountDownLatch(1);
        uiContext.addContextDestroyListener(c -> destroyed.countDown());
        uiContext.moveByAdministrator();
        assertTrue(destroyed.await(5, TimeUnit.SECONDS));
        final ArgumentCaptor<Object> token = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(socket).encode(Mockito.eq(ServerToClientModel.CONTEXT_STATE), token.capture());
        return Map.of(ClientToServerModel.MOVED_STATE.toStringValue(), List.of((String) token.getValue()));
    }

    /** One screen of an order: the same bindings, in the same order, on both servers. */
    private static final class Form {

        final Field<String> instrument = new Field<>();
        final Field<String> quantity = new Field<>();
        final Field<Boolean> confirmed = new Field<>();
        final Field<Date> date = new Field<>();
        final Field<BigDecimal> price = new Field<>(); // no type a move carries
        final Field<Integer> lots = new Field<>();
        final Field<Long> account = new Field<>();
        final Field<Double> rate = new Field<>();
        final Field<Side> side = new Field<>();
        final Binder<Order> binder = new Binder<>();
        HandlerRegistration carried;

        Form() {
            binder.forField(instrument).bind(Order::getInstrument, Order::setInstrument);
            binder.forField(quantity).withConverter(Converters.toInteger("Not a number")).bind(Order::getQuantity, Order::setQuantity);
            binder.forField(confirmed).bind(Order::getConfirmed, Order::setConfirmed);
            binder.forField(date).bind(Order::getDate, Order::setDate);
            binder.forField(price).bind(Order::getPrice, Order::setPrice);
            binder.forField(lots).bind(Order::getLots, Order::setLots);
            binder.forField(account).bind(Order::getAccount, Order::setAccount);
            binder.forField(rate).bind(Order::getRate, Order::setRate);
            binder.forField(side).bind(Order::getSide, Order::setSide);
            binder.readBean(new Order());
            carried = binder.carry("order");
        }
    }

    @Test
    public void whatWasTypedComesBackOnTheNextServerInvalidTextIncluded() throws Exception {
        final UIContext first = context(Map.of());
        final Form[] typed = new Form[1];
        first.execute(() -> {
            typed[0] = new Form();
            typed[0].instrument.typed("EUR/USD");
            typed[0].quantity.typed("12x");
            typed[0].confirmed.typed(true);
            typed[0].date.typed(new Date(1_798_000_000_000L));
            typed[0].price.typed(new BigDecimal("1.0845"));
            typed[0].lots.typed(3);
            typed[0].account.typed(40_000_000_001L);
            typed[0].rate.typed(1.0845);
            typed[0].side.typed(Side.SELL);
        });
        final Map<String, List<String>> offered = moved(first);

        final UIContext next = context(offered);
        final Form[] shown = new Form[1];
        next.execute(() -> shown[0] = new Form());

        assertEquals("EUR/USD", shown[0].instrument.getValue());
        assertEquals("12x", shown[0].quantity.getValue());
        assertEquals(Boolean.TRUE, shown[0].confirmed.getValue());
        assertEquals(new Date(1_798_000_000_000L), shown[0].date.getValue());
        assertNull(shown[0].price.getValue(), "not carried: no type of the protocol");
        assertEquals(3, shown[0].lots.getValue());
        assertEquals(40_000_000_001L, shown[0].account.getValue());
        assertEquals(1.0845, shown[0].rate.getValue());
        assertEquals(Side.SELL, shown[0].side.getValue());
        final Binder.Status status = shown[0].binder.getStatus();
        assertTrue(status.hasChanges(), "the changes to save are there again");
        assertFalse(status.valid(), "and so is the error of the quantity");
    }

    @Test
    public void aFieldOfAnotherTypeOnTheNextServerIsLeftAsItIs() throws Exception {
        final UIContext first = context(Map.of());
        first.execute(() -> {
            final Binder<Order> binder = new Binder<>();
            final Field<String> text = new Field<>();
            binder.forField(text).bind(Order::getInstrument, Order::setInstrument);
            binder.carry("order");
            text.typed("EUR/USD");
        });
        final Map<String, List<String>> offered = moved(first);

        final UIContext next = context(offered);
        final Field<Boolean> flag = new Field<>();
        next.execute(() -> {
            final Binder<Order> binder = new Binder<>();
            flag.typed(false);
            binder.forField(flag).bind(Order::getConfirmed, Order::setConfirmed);
            binder.carry("order");
        });

        assertEquals(Boolean.FALSE, flag.getValue());
    }

    @Test
    public void aFormThatStoppedCarryingGivesNothingToTheMove() throws Exception {
        final UIContext first = context(Map.of());
        first.execute(() -> {
            final Form form = new Form();
            form.instrument.typed("EUR/USD");
            form.carried.removeHandler(); // the screen left before the context did
        });
        final CountDownLatch destroyed = new CountDownLatch(1);
        first.addContextDestroyListener(c -> destroyed.countDown());
        first.moveByAdministrator();
        assertTrue(destroyed.await(5, TimeUnit.SECONDS));

        Mockito.verify(socket, Mockito.never()).encode(Mockito.eq(ServerToClientModel.CONTEXT_STATE), Mockito.any());
    }

    @Test
    public void theApplicationsOwnStateAndTheFormsGoTogether() throws Exception {
        final UIContext first = context(Map.of());
        first.execute(() -> {
            UIContext.get().onMove(state -> state.putString("tab", "blotter"));
            new Form().instrument.typed("EUR/USD");
        });
        final Map<String, List<String>> offered = moved(first);

        final UIContext next = context(offered);
        assertEquals("blotter", next.getMovedState().getString("tab", null));
        assertEquals("EUR/USD", next.getMovedState().get("order/0"));
    }

    /** An enum renamed between the two servers' versions: the field shows what the bean holds. */
    @Test
    public void aConstantTheNextServerDoesNotKnowIsLeft() throws Exception {
        final StateSeal seal = new StateSeal(KEY, "orders", java.time.Clock.systemUTC());
        // the side is the ninth binding of the form: a class gone, then a constant gone
        for (final String carried : List.of("com.example.Gone:BUY", Side.class.getName() + ":SHORT")) {
            final MovedState state = new MovedState().putString("order/8#enum", carried);
            final UIContext next = context(Map.of(ClientToServerModel.MOVED_STATE.toStringValue(), List.of(seal.seal(state))));
            final Form[] shown = new Form[1];
            next.execute(() -> shown[0] = new Form());

            assertNull(shown[0].side.getValue(), carried);
        }
    }

    public enum Side {
        BUY, SELL
    }

    /** A field as a widget is, without the widget: what the user types sets it, and its handlers are told. */
    private static final class Field<T> implements HasPValue<T> {

        private final List<PValueChangeHandler<T>> handlers = new ArrayList<>();
        private T value;

        void typed(final T typed) {
            value = typed;
            handlers.forEach(h -> h.onValueChange(new PValueChangeEvent<>(this, typed)));
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public void setValue(final T value) {
            this.value = value;
        }

        @Override
        public void addValueChangeHandler(final PValueChangeHandler<T> handler) {
            handlers.add(handler);
        }

        @Override
        public boolean removeValueChangeHandler(final PValueChangeHandler<T> handler) {
            return handlers.remove(handler);
        }

        @Override
        public Collection<PValueChangeHandler<T>> getValueChangeHandlers() {
            return handlers;
        }
    }

    public static final class Order {

        private String instrument;
        private Integer quantity;
        private Boolean confirmed;
        private Date date;
        private BigDecimal price;
        private Integer lots;
        private Long account;
        private Double rate;
        private Side side;

        public Side getSide() {
            return side;
        }

        public void setSide(final Side side) {
            this.side = side;
        }

        public Integer getLots() {
            return lots;
        }

        public void setLots(final Integer lots) {
            this.lots = lots;
        }

        public Long getAccount() {
            return account;
        }

        public void setAccount(final Long account) {
            this.account = account;
        }

        public Double getRate() {
            return rate;
        }

        public void setRate(final Double rate) {
            this.rate = rate;
        }

        public String getInstrument() {
            return instrument;
        }

        public void setInstrument(final String instrument) {
            this.instrument = instrument;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        public Boolean getConfirmed() {
            return confirmed;
        }

        public void setConfirmed(final Boolean confirmed) {
            this.confirmed = confirmed;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(final Date date) {
            this.date = date;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(final BigDecimal price) {
            this.price = price;
        }
    }
}
