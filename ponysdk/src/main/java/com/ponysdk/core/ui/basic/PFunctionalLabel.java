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

package com.ponysdk.core.ui.basic;

import java.util.Arrays;
import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A PLabel that contains a text, <i>not</i> interpreted as HTML, that can be either forced using
 * <code>setText(String)</code> or calculated using a <code>PFunction</code> and an array of arguments. This widget
 * uses a &lt;div&gt; element, causing it to be displayed with block layout.
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Label { }</li>
 * </ul>
 */
public class PFunctionalLabel extends PLabel {

    private TextFunction textFunction;
    private PFunction pFunction;
    private Object[] args;

    PFunctionalLabel(final TextFunction textFunction) {
        this.textFunction = textFunction;
    }

    PFunctionalLabel(final TextFunction textFunction, final Object... args) {
        this.textFunction = textFunction;
        this.args = args;
    }

    @Override
    protected void enrichForUpdate(final ModelWriter writer) {
        super.enrichForUpdate(writer);
        writer.write(ServerToClientModel.FUNCTION_ID, this.pFunction.getID());
        if (this.args != null) writer.write(ServerToClientModel.FUNCTION_ARGS, this.args);
    }

    @Override
    protected boolean attach(final PWindow window, final PFrame frame) {
        this.pFunction = window.getPFunction(textFunction);
        return super.attach(window, frame);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FUNCTIONAL_LABEL;
    }

    @Override
    public String getText() {
        return text != null ? text : args != null ? textFunction.getJavaFunction().apply(args) : null;
    }

    public TextFunction getTextFunction() {
        return textFunction;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(final Object... args) {
        this.text = null;
        if (Arrays.equals(this.args, args)) return;
        this.args = args;
        if (initialized) saveUpdate(ServerToClientModel.FUNCTION_ARGS, this.args);
    }

    public void setTextFunction(final TextFunction textFunction) {
        if (Objects.equals(this.textFunction, textFunction)) return;
        this.textFunction = textFunction;
        final PWindow window = getWindow();
        if (window == null) return;
        this.pFunction = window.getPFunction(textFunction);
        if (initialized) saveUpdate(ServerToClientModel.FUNCTION_ID, this.pFunction.getID());
    }

    @Override
    public void setText(final String text) {
        this.args = null;
        super.setText(text);
    }

    @Override
    public String toString() {
        return super.toString() + ", textFunction=" + textFunction + ", args=" + Arrays.toString(args);
    }

}
