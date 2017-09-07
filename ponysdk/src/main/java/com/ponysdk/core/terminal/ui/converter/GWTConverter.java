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

    public static final HorizontalAlignmentConstant asHorizontalAlignmentConstant(final byte byteValue) {
        final PHorizontalAlignment alignment = PHorizontalAlignment.fromRawValue(byteValue);
        if (PHorizontalAlignment.ALIGN_LEFT.equals(alignment)) return HasHorizontalAlignment.ALIGN_LEFT;
        else if (PHorizontalAlignment.ALIGN_CENTER.equals(alignment)) return HasHorizontalAlignment.ALIGN_CENTER;
        else if (PHorizontalAlignment.ALIGN_RIGHT.equals(alignment)) return HasHorizontalAlignment.ALIGN_RIGHT;
        else throw new IllegalArgumentException("Undefined alignement : " + byteValue);
    }

    public static final VerticalAlignmentConstant asVerticalAlignmentConstant(final byte byteValue) {
        final PVerticalAlignment alignment = PVerticalAlignment.fromRawValue(byteValue);
        if (PVerticalAlignment.ALIGN_TOP.equals(alignment)) return HasVerticalAlignment.ALIGN_TOP;
        else if (PVerticalAlignment.ALIGN_MIDDLE.equals(alignment)) return HasVerticalAlignment.ALIGN_MIDDLE;
        else if (PVerticalAlignment.ALIGN_BOTTOM.equals(alignment)) return HasVerticalAlignment.ALIGN_BOTTOM;
        else throw new IllegalArgumentException("Undefined alignement : " + byteValue);
    }

    public static final Alignment asAlignment(final byte byteValue) {
        final PAlignment alignment = PAlignment.fromRawValue(byteValue);
        if (PAlignment.BEGIN.equals(alignment)) return Alignment.BEGIN;
        else if (PAlignment.END.equals(alignment)) return Alignment.END;
        else if (PAlignment.STRETCH.equals(alignment)) return Alignment.STRETCH;
        else throw new IllegalArgumentException("Undefined alignement : " + byteValue);
    }

    public static final FontSize asFontSize(final byte byteValue) {
        final PFontSize fontSize = PFontSize.fromRawValue(byteValue);
        if (PFontSize.XX_LARGE.equals(fontSize)) return FontSize.XX_LARGE;
        else if (PFontSize.X_LARGE.equals(fontSize)) return FontSize.X_LARGE;
        else if (PFontSize.LARGE.equals(fontSize)) return FontSize.LARGE;
        else if (PFontSize.MEDIUM.equals(fontSize)) return FontSize.MEDIUM;
        else if (PFontSize.SMALL.equals(fontSize)) return FontSize.SMALL;
        else if (PFontSize.X_SMALL.equals(fontSize)) return FontSize.X_SMALL;
        else if (PFontSize.XX_SMALL.equals(fontSize)) return FontSize.XX_SMALL;
        else throw new IllegalArgumentException("Undefined font size : " + byteValue);
    }

    public static final Justification asJustification(final byte byteValue) {
        final PJustification justification = PJustification.fromRawValue(byteValue);
        if (PJustification.CENTER.equals(justification)) return Justification.CENTER;
        else if (PJustification.FULL.equals(justification)) return Justification.FULL;
        else if (PJustification.LEFT.equals(justification)) return Justification.LEFT;
        else if (PJustification.RIGHT.equals(justification)) return Justification.RIGHT;
        else throw new IllegalArgumentException("Undefined justification : " + byteValue);
    }

    public static final Unit asUnit(final byte byteValue) {
        final PUnit unit = PUnit.fromRawValue(byteValue);
        if (PUnit.PX.equals(unit)) return Unit.PX;
        else if (PUnit.EM.equals(unit)) return Unit.EM;
        else if (PUnit.PCT.equals(unit)) return Unit.PCT;
        else if (PUnit.CM.equals(unit)) return Unit.CM;
        else if (PUnit.EX.equals(unit)) return Unit.EX;
        else if (PUnit.IN.equals(unit)) return Unit.IN;
        else if (PUnit.MM.equals(unit)) return Unit.MM;
        else if (PUnit.PC.equals(unit)) return Unit.PC;
        else if (PUnit.PT.equals(unit)) return Unit.PT;
        else throw new IllegalArgumentException("Undefined unit : " + byteValue);
    }

}
