/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.terminal.ui.widget.mask;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.TextBox;

public class TextBoxMaskedDecorator {

    private static final RegExp regExp = RegExp.compile("{{([A0]+)}}", "g");
    private final InputValue value = new InputValue();
    private final TextBox textBox;

    public TextBoxMaskedDecorator(final TextBox textBox) {
        this.textBox = textBox;
        this.textBox.addKeyDownHandler(event -> {
            final int pos = textBox.getCursorPos();
            if (event.getNativeKeyCode() == KeyCodes.KEY_BACKSPACE) {

                if (textBox.getSelectionLength() > 0) {
                    clearSelection();
                    cancelKey();
                    return;
                }

                final String oldValue = value.getText();

                final int from = pos - 1;
                final int to = from + 1;

                int np = pos - 1;
                if (np < 0) np = 0;

                if (value.remove(from, to)) refresh(np);
                else setCursorPos(np);

                ValueChangeEvent.fireIfNotEqual(textBox, oldValue, value.getText());
                cancelKey();
            } else if (event.getNativeKeyCode() == KeyCodes.KEY_DELETE) {

                if (textBox.getSelectionLength() > 0) {
                    clearSelection();
                    cancelKey();
                    return;
                }

                final String oldValue = value.getText();

                final int to = pos + 1;

                if (value.remove(pos, to)) refresh(pos);
                else setCursorPos(pos);

                ValueChangeEvent.fireIfNotEqual(textBox, oldValue, value.getText());
                cancelKey();
            }
        });
        this.textBox.addKeyPressHandler(event -> {
            final int pos = textBox.getCursorPos();
            if (textBox.getSelectionLength() > 0) clearSelection();

            final String oldValue = value.getText();

            value.remove(pos, pos + 1);
            final int nextPos = value.insert(pos, event.getCharCode());
            if (nextPos != -1) refresh(nextPos);

            ValueChangeEvent.fireIfNotEqual(textBox, oldValue, value.getText());

            cancelKey();
        });
        this.textBox.addFocusHandler(event -> Scheduler.get().scheduleDeferred(() -> {
            setText(value.getText());
            setCursorPos(0);
        }));
    }

    public void setMask(final String mask, final boolean showMask, final char freeSymbol) {
        final List<Flag> flags = new ArrayList<>();

        int current = 0;
        MatchResult matchResult = regExp.exec(mask);
        while (matchResult != null) {
            final String group = matchResult.getGroup(1);

            // Fill forced
            for (int i = current; i < matchResult.getIndex(); i++) {
                final char c = mask.charAt(current);
                flags.add(new Const(c, showMask, freeSymbol));
                current++;
            }

            // start of group '{{'
            current += 2;

            for (int i = 0; i < group.length(); i++) {
                if (Character.isDigit(group.charAt(i))) flags.add(new Digit(freeSymbol));
                else flags.add(new Char(freeSymbol));
                current++;
            }

            // end of group '}}'
            current += 2;

            matchResult = regExp.exec(mask);
        }

        // Fill forced
        for (int i = current; i < mask.length(); i++) {
            final char c = mask.charAt(current);
            flags.add(new Const(c, showMask, freeSymbol));
            current++;
        }

        value.init(flags);

        final String currentText = textBox.getText();
        for (int c = 0; c < currentText.length(); c++) {
            final int insert = value.insert(c, currentText.charAt(c));
            if (insert == -1) break;
        }

        final String text = value.getText();
        setText(text);
        setMaxLength(text.length());
    }

    private void refresh(final int newPosition) {
        setText(value.getText());
        setCursorPos(newPosition);
    }

    private void clearSelection() {
        final int from = textBox.getCursorPos();
        final int to = from + textBox.getSelectionLength();
        if (value.remove(from, to)) refresh(from);
        else setCursorPos(from);
    }

    // Delegates
    public void cancelKey() {
        textBox.cancelKey();
    }

    public void setCursorPos(final int pos) {
        textBox.setCursorPos(pos);
    }

    public void setText(final String text) {
        textBox.setText(text);
    }

    public void setMaxLength(final int length) {
        textBox.setMaxLength(length);
    }

    public static abstract class Flag {

        protected boolean set = false;
        protected char c;

        protected char freeSymbol;
        protected boolean showMask;

        public Flag() {
        }

        public Flag(final char c, final boolean showMask) {
            this.c = c;
            this.set = true;
            this.showMask = showMask;
        }

        public abstract boolean match(final char c);

        public boolean isDigit() {
            return false;
        }

        public boolean isChar() {
            return false;
        }

        public boolean isConst() {
            return false;
        }

        public boolean isSet() {
            return set;
        }

        public void unset() {
            this.set = false;
        }

        public void set(final char c) {
            this.c = c;
            this.set = true;
        }

        @Override
        public String toString() {
            return isSet() ? Character.toString(c) : " ";
        }

        public char getValue(final boolean filling) {
            return set ? c : freeSymbol;
        }

    }

    public static class Digit extends Flag {

        public Digit(final char freeSymbol) {
            super();
            this.freeSymbol = freeSymbol;
        }

        @Override
        public boolean isDigit() {
            return true;
        }

        @Override
        public boolean match(final char c) {
            return Character.isDigit(c);
        }

        @Override
        public String toString() {
            return isSet() ? Character.toString(c) : "0";
        }
    }

    public static class Char extends Flag {

        public Char(final char freeSymbol) {
            super();
            this.freeSymbol = freeSymbol;
        }

        @Override
        public boolean isChar() {
            return true;
        }

        @Override
        public boolean match(final char c) {
            return Character.isLetter(c);
        }

        @Override
        public String toString() {
            return isSet() ? Character.toString(c) : "@";
        }
    }

    public static class Const extends Flag {

        public Const(final char c, final boolean showMask, final char freeSymbol) {
            super(c, showMask);
            this.freeSymbol = freeSymbol;
        }

        @Override
        public boolean isConst() {
            return true;
        }

        @Override
        public boolean match(final char c) {
            return c == this.c;
        }

        @Override
        public char getValue(final boolean filling) {
            if (!filling) return showMask ? c : freeSymbol;
            else return set ? c : freeSymbol;
        }
    }

    public static class InputValue {

        private int insertPosition = 0;
        private List<Flag> flags;

        public void init(final List<Flag> flags) {
            this.flags = flags;
        }

        public void reset() {
            for (final Flag f : flags) {
                if (!f.isConst()) f.unset();
            }
        }

        public String getText() {
            boolean updateInsert = true;
            insertPosition = 0;
            int pos = 0;
            final StringBuilder sb = new StringBuilder();
            for (final Flag f : flags) {
                sb.append(f.getValue(updateInsert));
                if (!f.isSet()) {
                    updateInsert = false;
                }
                pos++;
                if (updateInsert) insertPosition = pos;
            }
            return sb.toString();
        }

        int getInsertPosition() {
            return insertPosition;
        }

        public int insert(final int pos, final char charCode) {
            // if full, return
            if (pos >= flags.size()) return -1;

            int nextPositon;
            Flag next = null;
            for (nextPositon = pos; nextPositon < flags.size(); nextPositon++) {
                next = flags.get(nextPositon);
                if (next.isConst()) {
                    if (next.match(charCode)) {
                        // consume key and exit
                        return -1;
                    }
                } else {
                    break;
                }
            }

            if (next == null) return -1;

            // if not assignable, return
            if (!next.match(charCode)) return -1;

            // we can set the new value, shift if required
            if (!next.isSet()) {
                next.set(charCode);
            } else {
                insert(pos + 1, next.c);
                next.set(charCode);
            }

            return nextPositon + 1;
        }

        public boolean remove(final int from, final int to) {
            if (from < 0) return false;

            for (int f = from; f < to; f++) {
                final Flag flag = flags.get(f);
                if (!flag.isConst()) flag.unset();
            }
            shift(from);

            return true;
        }

        private void shift(final int pos) {
            if (pos >= flags.size()) return;

            // go to next "settable"
            int next;
            Flag flag = null;
            for (next = pos; next < flags.size(); next++) {
                flag = flags.get(next);
                if (!flag.isConst() && !flag.isSet()) break;
            }

            if (flag == null) return;

            next++;

            // find next "value"
            for (int i = next; i < flags.size(); i++) {
                final Flag n = flags.get(i);
                if (n.isConst()) continue;
                if (!n.isSet()) continue;

                if (flag.match(n.c)) {
                    flag.set(n.c);
                    n.unset();
                }
                break;
            }

            shift(next);
        }

    }

}
