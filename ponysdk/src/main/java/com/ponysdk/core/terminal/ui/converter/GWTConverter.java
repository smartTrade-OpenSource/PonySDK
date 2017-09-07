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

package com.ponysdk.core.terminal.ui.converter;

import com.google.gwt.layout.client.Layout.Alignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.RichTextArea.FontSize;
import com.google.gwt.user.client.ui.RichTextArea.Justification;
import com.ponysdk.core.model.PAlignment;
import com.ponysdk.core.model.PFontSize;
import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PJustification;
import com.ponysdk.core.model.PVerticalAlignment;

public final class GWTConverter {

    public static final HorizontalAlignmentConstant asHorizontalAlignmentConstant(final byte byteValue) {
        final PHorizontalAlignment alignment = PHorizontalAlignment.fromRawValue(byteValue);
        switch (alignment) {
            case ALIGN_LEFT:
                return HasHorizontalAlignment.ALIGN_LEFT;
            case ALIGN_CENTER:
                return HasHorizontalAlignment.ALIGN_CENTER;
            case ALIGN_RIGHT:
                return HasHorizontalAlignment.ALIGN_RIGHT;
            default:
                throw new IllegalArgumentException("Undefined alignement");
        }
    }

    public static final VerticalAlignmentConstant asVerticalAlignmentConstant(final byte byteValue) {
        final PVerticalAlignment alignment = PVerticalAlignment.fromRawValue(byteValue);
        switch (alignment) {
            case ALIGN_TOP:
                return HasVerticalAlignment.ALIGN_TOP;
            case ALIGN_MIDDLE:
                return HasVerticalAlignment.ALIGN_MIDDLE;
            case ALIGN_BOTTOM:
                return HasVerticalAlignment.ALIGN_BOTTOM;
            default:
                throw new IllegalArgumentException("Undefined alignement");
        }
    }

    public static final Alignment asAlignment(final byte byteValue) {
        final PAlignment alignment = PAlignment.fromRawValue(byteValue);
        switch (alignment) {
            case BEGIN:
                return Alignment.BEGIN;
            case END:
                return Alignment.END;
            case STRETCH:
                return Alignment.STRETCH;
            default:
                throw new IllegalArgumentException("Undefined alignement");
        }
    }

    public static final FontSize asFontSize(final byte byteValue) {
        final PFontSize fontSize = PFontSize.fromRawValue(byteValue);
        switch (fontSize) {
            case XX_LARGE:
                return FontSize.XX_LARGE;
            case X_LARGE:
                return FontSize.X_LARGE;
            case LARGE:
                return FontSize.LARGE;
            case MEDIUM:
                return FontSize.MEDIUM;
            case SMALL:
                return FontSize.SMALL;
            case X_SMALL:
                return FontSize.X_SMALL;
            case XX_SMALL:
                return FontSize.XX_SMALL;
            default:
                throw new IllegalArgumentException("Undefined font size");
        }
    }

    public static final Justification asJustification(final byte byteValue) {
        final PJustification justification = PJustification.fromRawValue(byteValue);
        switch (justification) {
            case CENTER:
                return Justification.CENTER;
            case FULL:
                return Justification.FULL;
            case LEFT:
                return Justification.LEFT;
            case RIGHT:
                return Justification.RIGHT;
            default:
                throw new IllegalArgumentException("Undefined justification");
        }
    }

}
