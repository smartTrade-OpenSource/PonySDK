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

package com.ponysdk.ui.terminal.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.TextBox;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTNumberTextBox extends PTWidget<Composite> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new NumberBox(create));
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        cast().applyOptions(update);
        super.update(update, uiService);
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String handler = addHandler.getString(HANDLER.KEY);

        if (HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER.equals(handler)) {
            cast().addValueChangeHandler(new ValueChangeHandler() {

                @Override
                public void onValueChange(final String v) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER);
                    eventInstruction.put(PROPERTY.VALUE, v);
                    uiService.sendDataToServer(eventInstruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    @Override
    public NumberBox cast() {
        return (NumberBox) uiObject;
    }

    public static interface ValueChangeHandler {

        public void onValueChange(String v);
    }

    public static class NumberBox extends Composite implements MouseDownHandler, MouseUpHandler, KeyDownHandler, KeyUpHandler, com.google.gwt.event.logical.shared.ValueChangeHandler<String>, MouseOutHandler {

        private static final String ZEROS = "0000000000000000";

        private final FlowPanel wrapper = new FlowPanel();
        private final InlineHTML up = new InlineHTML("<span class=\"u\"></span>");
        private final InlineHTML down = new InlineHTML("<span class=\"d\"></span>");
        private final TextBox textBox = new TextBox();

        private boolean timerScheduled = false;

        private boolean paged = false;
        private boolean increment = true;
        private boolean enabled = true;

        private BigDecimal lastSendValue = null;
        private BigDecimal value = null;

        private boolean hasMin = false;
        private boolean hasMax = false;

        private final int initialDelay = 200;
        private int currentDelay = initialDelay;

        private int page = 10;
        private int decimal = 0;
        private BigDecimal min = new BigDecimal(0);
        private BigDecimal max = new BigDecimal(0);
        private BigDecimal step = new BigDecimal(1);
        private BigDecimal pagedStep = new BigDecimal(10);

        private final List<ValueChangeHandler> handlers = new ArrayList<ValueChangeHandler>();

        private class RefreshCommand implements RepeatingCommand {

            @Override
            public boolean execute() {

                if (increment) {
                    increase();
                } else {
                    decrease();
                }

                if (!timerScheduled) {
                    // Timer cancelled, fire change
                    paged = false;
                    currentDelay = initialDelay;
                    fire();
                } else {
                    // Re-schedule
                    currentDelay -= 20;
                    if (currentDelay < 10) currentDelay = 10;
                    Scheduler.get().scheduleFixedDelay(new RefreshCommand(), currentDelay);
                }

                return false;
            }

        }

        public NumberBox(final PTInstruction options) {

            initWidget(wrapper);

            wrapper.add(textBox);
            wrapper.add(up);
            wrapper.add(down);

            wrapper.setStyleName("spinner");
            up.setStyleName("up");
            down.setStyleName("down");
            textBox.addStyleName("in");
            up.addStyleName("arrow");
            down.addStyleName("arrow");

            up.addMouseDownHandler(this);
            up.addMouseUpHandler(this);
            up.addMouseOutHandler(this);
            down.addMouseDownHandler(this);
            down.addMouseUpHandler(this);
            down.addMouseOutHandler(this);
            textBox.addKeyDownHandler(this);
            textBox.addKeyUpHandler(this);
            textBox.addValueChangeHandler(this);

            applyOptions(options);
        }

        public void enabled(final boolean enabled) {
            this.enabled = enabled;
            textBox.setEnabled(enabled);
            if (enabled) {
                wrapper.addStyleName("enabled");
                wrapper.removeStyleName("disabled");
            } else {
                wrapper.removeStyleName("enabled");
                wrapper.addStyleName("disabled");
            }
        }

        public void addValueChangeHandler(final ValueChangeHandler h) {
            handlers.add(h);
        }

        protected void decrease() {
            if (value == null) {
                value = min;
            } else {
                if (paged) {
                    value = value.subtract(pagedStep);
                } else {
                    value = value.subtract(step);
                }
            }

            checkMinMax();

            refreshTextBox();
        }

        private void checkMinMax() {
            if (hasMin) {
                if (value.compareTo(min) < 0) {
                    value = min;
                    timerScheduled = false;
                }
            }
            if (hasMax) {
                if (value.compareTo(max) > 0) {
                    value = max;
                    timerScheduled = false;
                }
            }
        }

        protected void increase() {

            if (value == null) {
                value = min;
            } else {
                if (paged) {
                    value = value.add(pagedStep);
                } else {
                    value = value.add(step);
                }
            }

            checkMinMax();

            refreshTextBox();
        }

        public void applyOptions(final PTInstruction options) {
            if (options.containsKey(PROPERTY.MIN)) {
                hasMin = true;
                min = new BigDecimal(options.getString(PROPERTY.MIN));
            }
            if (options.containsKey(PROPERTY.MAX)) {
                hasMax = true;
                max = new BigDecimal(options.getString(PROPERTY.MAX));
            }
            if (options.containsKey(PROPERTY.STEP)) {
                step = new BigDecimal(options.getString(Dictionnary.PROPERTY.STEP));
                pagedStep = step.multiply(new BigDecimal(page));
            }
            if (options.containsKey(PROPERTY.PAGE)) {
                page = options.getInt(Dictionnary.PROPERTY.PAGE);
                pagedStep = step.multiply(new BigDecimal(page));
            }
            if (options.containsKey(PROPERTY.DECIMAL)) decimal = options.getInt(Dictionnary.PROPERTY.DECIMAL);

            if (options.containsKey(PROPERTY.TEXT)) {
                final String text = options.getString(PROPERTY.TEXT);
                if (text == null || text.isEmpty()) {
                    value = null;
                    lastSendValue = null;
                    textBox.setText(text);
                } else {
                    try {
                        value = new BigDecimal(text);
                        lastSendValue = value;
                        textBox.setText(format());
                    } catch (final NumberFormatException e) {}
                }
            } else if (options.containsKey(PROPERTY.ENABLED)) {
                enabled(options.getBoolean(PROPERTY.ENABLED));
            } else if (options.containsKey(PROPERTY.FOCUSED)) {
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                    @Override
                    public void execute() {
                        textBox.setFocus(options.getBoolean(PROPERTY.FOCUSED));
                    }
                });
            }
        }

        @Override
        public void onValueChange(final ValueChangeEvent<String> event) {
            final String valueAsString = event.getValue();
            if (valueAsString != null && !valueAsString.isEmpty()) {
                try {
                    final BigDecimal newvalue = new BigDecimal(textBox.getText());
                    if (!newvalue.equals(value)) {
                        value = newvalue;
                        refreshTextBox();
                        fire();
                    }
                } catch (final NumberFormatException e) {
                    //
                    refreshTextBox();
                }
            } else {
                if (value != null) {
                    value = null;
                    fire();
                }
            }
        }

        private void refreshTextBox() {
            textBox.setText(format());
        }

        private String format() {

            if (value == null) return "";

            String v = value.toString();
            final int actual = decimals(v);
            int diff = actual - decimal;
            if (decimal == 0 && diff == 1) diff++;

            if (diff == 0) return v;
            if (diff > 0) return v.substring(0, v.length() - diff);

            if (actual == 0) v = v.concat(".");
            return v.concat(ZEROS.substring(0, -diff));
        }

        private static int decimals(final String v) {
            if (v == null) return 0;

            final int dotIndex = v.indexOf(".");
            if (dotIndex < 0) return 0;

            return (v.length() - (dotIndex + 1));
        }

        @Override
        public void onKeyDown(final KeyDownEvent event) {
            if (enabled && !timerScheduled) {

                boolean trigger = false;
                if (event.isDownArrow()) {
                    trigger = true;
                    paged = false;
                    increment = false;
                } else if (event.isUpArrow()) {
                    trigger = true;
                    paged = false;
                    increment = true;
                } else if (event.getNativeKeyCode() == KeyCodes.KEY_PAGEDOWN) {
                    trigger = true;
                    paged = true;
                    increment = false;
                } else if (event.getNativeKeyCode() == KeyCodes.KEY_PAGEUP) {
                    trigger = true;
                    paged = true;
                    increment = true;
                }

                if (trigger) {
                    timerScheduled = true;
                    Scheduler.get().scheduleFixedDelay(new RefreshCommand(), initialDelay);
                }
            }
        }

        @Override
        public void onKeyUp(final KeyUpEvent event) {
            timerScheduled = false;

            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                fire();
            }
        }

        @Override
        public void onMouseDown(final MouseDownEvent event) {
            if (enabled) {
                final Object sender = event.getSource();
                if (sender == up) {
                    increment = true;
                } else {
                    increment = false;
                }
                timerScheduled = true;
                Scheduler.get().scheduleFixedDelay(new RefreshCommand(), initialDelay);
            }
        }

        @Override
        public void onMouseUp(final MouseUpEvent event) {
            timerScheduled = false;
        }

        @Override
        public void onMouseOut(final MouseOutEvent event) {
            timerScheduled = false;
        }

        protected void fireIfNotEqual(final String prev, final String text) {
            if (prev != text && (prev == null || !prev.equals(text))) {
                fire();
            }
        }

        protected void fire() {
            final String text = textBox.getText();
            if (text != null && !text.isEmpty()) {
                try {
                    value = new BigDecimal(text);
                } catch (final NumberFormatException e) {}
            } else {
                value = null;
            }

            if (!valueChanged()) return;
            lastSendValue = value;

            for (final ValueChangeHandler h : handlers) {
                h.onValueChange(text);
            }
        }

        private boolean valueChanged() {
            if (lastSendValue == null) {
                if (value != null) return true;
            } else if (!lastSendValue.equals(value)) return true;
            return false;
        }
    }

    public static native void log(String msg) /*-{
                                              console.log(msg);
                                              }-*/;
}
