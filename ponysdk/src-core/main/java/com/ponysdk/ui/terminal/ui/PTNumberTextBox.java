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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
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

    public static class NumberBox extends Composite implements MouseDownHandler, MouseUpHandler, KeyDownHandler, KeyUpHandler, com.google.gwt.event.logical.shared.ValueChangeHandler<String> {

        private static final String ZEROS = "0000000000000000";

        private final FlowPanel wrapper = new FlowPanel();
        private final InlineHTML up = new InlineHTML("<span class=\"u\"></span>");
        private final InlineHTML down = new InlineHTML("<span class=\"d\"></span>");
        private final TextBox textBox = new TextBox();

        private boolean timerScheduled = false;

        private boolean paged = false;
        private boolean increment = true;
        private final boolean enabled = true;

        private Double value = null;

        private boolean hasMin = false;
        private boolean hasMax = false;

        private double min = 0;
        private double max = 0;
        private double step = 1;
        private int page = 10;
        private int decimal = 0;

        private final List<ValueChangeHandler> handlers = new ArrayList<ValueChangeHandler>();

        private final RefreshCommand command;

        private class RefreshCommand implements RepeatingCommand {

            @Override
            public boolean execute() {

                if (increment) {
                    increase();
                } else {
                    decrease();
                }

                if (!timerScheduled) {
                    paged = false;
                    fire();
                }

                return timerScheduled;
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
            down.addMouseDownHandler(this);
            down.addMouseUpHandler(this);
            textBox.addKeyDownHandler(this);
            textBox.addKeyUpHandler(this);
            textBox.addValueChangeHandler(this);

            command = new RefreshCommand();

            applyOptions(options);
        }

        public void enabled(final boolean enabled) {
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
                if (paged) value -= (step * page);
                else value -= step;
            }

            if (hasMin) {
                if (value < min) {
                    value = min;
                    timerScheduled = false;
                }
            }

            refreshTextBox();
        }

        protected void increase() {

            if (value == null) {
                value = min;
            } else {
                if (paged) value += (step * page);
                else value += step;
            }

            if (hasMax) {
                if (value > max) {
                    value = max;
                    timerScheduled = false;
                }
            }

            refreshTextBox();
        }

        public void applyOptions(final PTInstruction options) {
            if (options.containsKey(PROPERTY.MIN)) {
                hasMin = true;
                min = options.getDouble(PROPERTY.MIN);
            }
            if (options.containsKey(PROPERTY.MAX)) {
                hasMax = true;
                max = options.getDouble(PROPERTY.MAX);
            }
            if (options.containsKey(PROPERTY.STEP)) step = options.getDouble(Dictionnary.PROPERTY.STEP);
            if (options.containsKey(PROPERTY.PAGE)) page = options.getInt(Dictionnary.PROPERTY.PAGE);
            if (options.containsKey(PROPERTY.DECIMAL)) decimal = options.getInt(Dictionnary.PROPERTY.DECIMAL);

            if (options.containsKey(PROPERTY.TEXT)) {
                textBox.setText(options.getString(PROPERTY.TEXT));
            } else if (options.containsKey(PROPERTY.ENABLED)) {
                textBox.setEnabled(options.getBoolean(PROPERTY.ENABLED));
            }
        }

        @Override
        public void onValueChange(final ValueChangeEvent<String> event) {
            final String valueAsString = event.getValue();
            if (valueAsString != null && !valueAsString.isEmpty()) {
                try {
                    final Double newvalue = Double.parseDouble(textBox.getText());
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
            if (decimal == 0) diff++;

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
                    Scheduler.get().scheduleFixedDelay(command, 120);
                }
            }
        }

        @Override
        public void onKeyUp(final KeyUpEvent event) {
            timerScheduled = false;
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
                Scheduler.get().scheduleFixedDelay(command, 120);
            }
        }

        @Override
        public void onMouseUp(final MouseUpEvent event) {
            timerScheduled = false;
        }

        protected void fireIfNotEqual(final String prev, final String text) {
            if (prev != text && (prev == null || !prev.equals(text))) {
                fire();
            }
        }

        protected void fire() {
            final String text = textBox.getText();
            value = Double.parseDouble(text);
            for (final ValueChangeHandler h : handlers) {
                h.onValueChange(text);
            }
        }

    }

}
