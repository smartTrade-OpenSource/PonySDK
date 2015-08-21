
package com.ponysdk.ui.terminal.ui.widget.mask;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.TextBox;

public class TextBoxMaskedDecorator implements KeyPressHandler, FocusHandler, KeyDownHandler {

    public static abstract class Flag {

        protected boolean set = false;
        protected char c;

        protected char freeSymbol;
        protected boolean showMask;

        public Flag() {}

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
            if (isSet()) return Character.toString(c);
            return " ";
        }

        public char getValue(final boolean filling) {
            if (set) return c;
            return freeSymbol;
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
            if (Character.isDigit(c)) return true;
            return false;
        }

        @Override
        public String toString() {
            if (isSet()) return Character.toString(c);
            return "0";
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
            if (Character.isLetter(c)) return true;
            return false;
        }

        @Override
        public String toString() {
            if (isSet()) return Character.toString(c);
            return "@";
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
            if (!filling) {
                if (showMask) return c;
                return freeSymbol;
            }

            if (set) return c;
            return freeSymbol;
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

        public int getInsertPosition() {
            return insertPosition;
        }

        public int insert(final int pos, final char charCode) {

            // if full, return
            if (pos >= flags.size()) return -1;

            int nextPositon = -1;
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

    private static final RegExp regExp = RegExp.compile("{{([A0]+)}}", "g");

    private final InputValue value = new InputValue();
    private final TextBox textBox;

    public TextBoxMaskedDecorator(final TextBox textBox) {
        this.textBox = textBox;
        this.textBox.addKeyDownHandler(this);
        this.textBox.addKeyPressHandler(this);
        this.textBox.addFocusHandler(this);
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

        final String currentText = getText();
        for (int c = 0; c < currentText.length(); c++) {
            final int insert = value.insert(c, currentText.charAt(c));
            if (insert == -1) break;
        }

        final String text = value.getText();
        setText(text);
        setMaxLength(text.length());
    }

    @Override
    public void onFocus(final FocusEvent event) {
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                setInitialPosition();
            }
        });
    }

    protected void setInitialPosition() {
        setText(value.getText());
        setCursorPos(value.getInsertPosition());
    }

    private void refresh(final int newPosition) {
        setText(value.getText());
        setCursorPos(newPosition);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {

        final int pos = getCursorPos();
        if (getSelectionLength() > 0) clearSelection();

        final int nextPos = value.insert(pos, event.getCharCode());
        if (nextPos != -1) {
            refresh(nextPos);
        }

        cancelKey();
    }

    @Override
    public void onKeyDown(final KeyDownEvent event) {
        final int pos = getCursorPos();
        if (event.getNativeKeyCode() == KeyCodes.KEY_BACKSPACE) {

            if (getSelectionLength() > 0) {
                clearSelection();
                cancelKey();
                return;
            }

            final int from = pos - 1;
            final int to = from + 1;

            int np = pos - 1;
            if (np < 0) np = 0;

            if (value.remove(from, to)) {
                refresh(np);
            } else {
                setCursorPos(np);
            }

            cancelKey();
        } else if (event.getNativeKeyCode() == KeyCodes.KEY_DELETE) {

            if (getSelectionLength() > 0) {
                clearSelection();
                cancelKey();
                return;
            }

            final int from = pos;
            final int to = from + 1;

            if (value.remove(from, to)) {
                refresh(pos);
            } else {
                setCursorPos(pos);
            }

            cancelKey();
        }
    }

    private void clearSelection() {
        final int from = getCursorPos();
        final int to = from + getSelectionLength();
        if (value.remove(from, to)) {
            refresh(from);
        } else {
            setCursorPos(from);
        }
    }

    // Delegates
    public void cancelKey() {
        textBox.cancelKey();
    }

    public int getCursorPos() {
        return textBox.getCursorPos();
    }

    public String getText() {
        return textBox.getText();
    }

    public int getSelectionLength() {
        return textBox.getSelectionLength();
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

}
