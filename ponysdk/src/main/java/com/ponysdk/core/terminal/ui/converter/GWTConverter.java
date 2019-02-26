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

import com.google.gwt.dom.client.Style.Unit;
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
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.PVerticalAlignment;

public final class GWTConverter {

    public static final HorizontalAlignmentConstant asHorizontalAlignmentConstant(final int rawValue) {
        final PHorizontalAlignment alignment = PHorizontalAlignment.fromRawValue(rawValue);
        if (PHorizontalAlignment.ALIGN_LEFT == alignment) return HasHorizontalAlignment.ALIGN_LEFT;
        else if (PHorizontalAlignment.ALIGN_CENTER == alignment) return HasHorizontalAlignment.ALIGN_CENTER;
        else if (PHorizontalAlignment.ALIGN_RIGHT == alignment) return HasHorizontalAlignment.ALIGN_RIGHT;
        else throw new IllegalArgumentException("Undefined alignement : " + rawValue);
    }

    public static final VerticalAlignmentConstant asVerticalAlignmentConstant(final int rawValue) {
        final PVerticalAlignment alignment = PVerticalAlignment.fromRawValue(rawValue);
        if (PVerticalAlignment.ALIGN_TOP == alignment) return HasVerticalAlignment.ALIGN_TOP;
        else if (PVerticalAlignment.ALIGN_MIDDLE == alignment) return HasVerticalAlignment.ALIGN_MIDDLE;
        else if (PVerticalAlignment.ALIGN_BOTTOM == alignment) return HasVerticalAlignment.ALIGN_BOTTOM;
        else throw new IllegalArgumentException("Undefined alignement : " + rawValue);
    }

    public static final Alignment asAlignment(final int rawValue) {
        final PAlignment alignment = PAlignment.fromRawValue(rawValue);
        if (PAlignment.BEGIN == alignment) return Alignment.BEGIN;
        else if (PAlignment.END == alignment) return Alignment.END;
        else if (PAlignment.STRETCH == alignment) return Alignment.STRETCH;
        else throw new IllegalArgumentException("Undefined alignement : " + rawValue);
    }

    public static final FontSize asFontSize(final int rawValue) {
        final PFontSize fontSize = PFontSize.fromRawValue(rawValue);
        if (PFontSize.XX_LARGE == fontSize) return FontSize.XX_LARGE;
        else if (PFontSize.X_LARGE == fontSize) return FontSize.X_LARGE;
        else if (PFontSize.LARGE == fontSize) return FontSize.LARGE;
        else if (PFontSize.MEDIUM == fontSize) return FontSize.MEDIUM;
        else if (PFontSize.SMALL == fontSize) return FontSize.SMALL;
        else if (PFontSize.X_SMALL == fontSize) return FontSize.X_SMALL;
        else if (PFontSize.XX_SMALL == fontSize) return FontSize.XX_SMALL;
        else throw new IllegalArgumentException("Undefined font size : " + rawValue);
    }

    public static final Justification asJustification(final int rawValue) {
        final PJustification justification = PJustification.fromRawValue(rawValue);
        if (PJustification.CENTER == justification) return Justification.CENTER;
        else if (PJustification.FULL == justification) return Justification.FULL;
        else if (PJustification.LEFT == justification) return Justification.LEFT;
        else if (PJustification.RIGHT == justification) return Justification.RIGHT;
        else throw new IllegalArgumentException("Undefined justification : " + rawValue);
    }

    public static final Unit asUnit(final int rawValue) {
        final PUnit unit = PUnit.fromRawValue(rawValue);
        if (PUnit.PX == unit) return Unit.PX;
        else if (PUnit.EM == unit) return Unit.EM;
        else if (PUnit.PCT == unit) return Unit.PCT;
        else if (PUnit.CM == unit) return Unit.CM;
        else if (PUnit.EX == unit) return Unit.EX;
        else if (PUnit.IN == unit) return Unit.IN;
        else if (PUnit.MM == unit) return Unit.MM;
        else if (PUnit.PC == unit) return Unit.PC;
        else if (PUnit.PT == unit) return Unit.PT;
        else throw new IllegalArgumentException("Undefined unit : " + rawValue);
    }

}
