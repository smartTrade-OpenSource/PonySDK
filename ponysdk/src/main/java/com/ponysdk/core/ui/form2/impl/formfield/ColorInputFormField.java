package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.PTextBox;

public class ColorInputFormField extends AbstractInputFormField<ColorInputFormField.Color> {
    public ColorInputFormField(String caption) {
        super(caption);
    }

    @Override
    protected PTextBox createInnerWidget() {
        PTextBox input = super.createInnerWidget();
        input.setAttribute("type", "color");
        return input;
    }

    @Override
    public Color getValue() {
        if (input.getText().isEmpty())
            return null;
        else
            return Color.from(input.getText());
    }

    @Override
    public void setValue(Color color) {
        input.setText(color.toHexString());
    }

    public static class Color {
        private final int value;

        private Color(int r, int g, int b) {
            this(r, g, b, 255);
        }

        public Color(int r, int g, int b, int a) {
            value = ((a & 0xFF) << 24) |
                    ((r & 0xFF) << 16) |
                    ((g & 0xFF) << 8) |
                    ((b & 0xFF));
        }

        public static Color from(String color) {
            int i = Integer.decode(color);
            return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
        }

        public int getRed() {
            return (value >> 16) & 0xFF;
        }

        public int getGreen() {
            return (value >> 8) & 0xFF;
        }

        public int getBlue() {
            return (value) & 0xFF;
        }

        public int getAlpha() {
            return (value >> 24) & 0xff;
        }

        public String toHexString() {
            return Integer.toHexString(value);
        }

        public String toString() {
            return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + ",a=" + getAlpha() + "]";
        }


    }

}
