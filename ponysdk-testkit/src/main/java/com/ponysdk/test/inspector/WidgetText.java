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

package com.ponysdk.test.inspector;

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBoxBase;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * Internal testkit utility that centralizes widget text extraction.
 * <p>
 * Both {@code InspectorWidget#getText()} and
 * {@code com.ponysdk.test.inspector.predicate.Predicates#text(String)} route through this class
 * so the matching rules stay in sync: if the predicate says "text equals X" and the inspector
 * reports "my text is X", they must agree on how X is computed.
 * </p>
 *
 * <h2>Resolution order</h2>
 * <ol>
 *     <li>{@link PHTML} &rarr; {@link PHTML#getHTML()} (checked before {@link PLabel} because
 *         {@code PHTML extends PLabel})</li>
 *     <li>{@link PLabel} &rarr; {@link PLabel#getText()}</li>
 *     <li>{@link PButton} &rarr; {@link PButton#getText()}</li>
 *     <li>{@link PCheckBox} &rarr; {@link PCheckBox#getText()}</li>
 *     <li>{@link PTextBoxBase} &rarr; {@link PTextBoxBase#getText()}</li>
 *     <li>{@link PElement} &rarr; {@link PElement#getInnerText()}</li>
 *     <li>Any other widget (or {@code null}) &rarr; {@code null}</li>
 * </ol>
 *
 * <p>This class is intentionally not part of the testkit's public extension surface; consumers
 * should rely on {@code InspectorWidget.getText()} instead.</p>
 */
public final class WidgetText {

    private WidgetText() {
        // utility class
    }

    /**
     * Returns the text carried by the given widget, following the resolution order documented on
     * this class.
     *
     * @param widget the widget to inspect (may be {@code null})
     * @return the widget's text, or {@code null} if the widget has no text concept
     */
    public static String extract(final PWidget widget) {
        if (widget instanceof PHTML) return ((PHTML) widget).getHTML();
        if (widget instanceof PLabel) return ((PLabel) widget).getText();
        if (widget instanceof PButton) return ((PButton) widget).getText();
        if (widget instanceof PCheckBox) return ((PCheckBox) widget).getText();
        if (widget instanceof PTextBoxBase) return ((PTextBoxBase) widget).getText();
        if (widget instanceof PElement) return ((PElement) widget).getInnerText();
        return null;
    }
}
