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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.WidgetType;

public class PNumberTextBox extends PTextBox {

    private double min;
    private double max;
    private double step;
    private int page;
    private int decimals;

    public PNumberTextBox(final Options options) {
        this("", options);
    }

    public PNumberTextBox(final String text, final Options options) {
        super(text);

        if (options.min != null) {
            min = options.min;
            create.put(Dictionnary.PROPERTY.MIN, String.valueOf(options.min));
        }

        if (options.max != null) {
            max = options.max;
            create.put(Dictionnary.PROPERTY.MAX, String.valueOf(options.max));
        }

        if (options.step != null) {
            step = options.step;
            create.put(Dictionnary.PROPERTY.STEP, String.valueOf(options.step));
        }

        if (options.page != null) {
            page = options.page;
            create.put(Dictionnary.PROPERTY.PAGE, options.page);
        }

        if (options.decimals != null) {
            decimals = options.decimals;
            create.put(Dictionnary.PROPERTY.DECIMAL, options.decimals);
        }
    }

    public void setOptions(final Options options) {
        if (options.min != null) {
            min = options.min;
            update(Dictionnary.PROPERTY.MIN, String.valueOf(options.min));
        }

        if (options.max != null) {
            max = options.max;
            update(Dictionnary.PROPERTY.MAX, String.valueOf(options.max));
        }

        if (options.step != null) {
            step = options.step;
            update(Dictionnary.PROPERTY.STEP, String.valueOf(options.step));
        }

        if (options.page != null) {
            page = options.page;
            update(Dictionnary.PROPERTY.PAGE, options.page);
        }

        if (options.decimals != null) {
            decimals = options.decimals;
            update(Dictionnary.PROPERTY.DECIMAL, options.decimals);
        }
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.NUMBERBOX;
    }

    public void setMin(final double min) {
        if (this.min == min) return;

        this.min = min;
        update(Dictionnary.PROPERTY.MIN, min);
    }

    public void setMax(final double max) {
        if (this.max == max) return;

        this.max = max;
        update(Dictionnary.PROPERTY.MAX, max);
    }

    public void setStep(final double step) {
        if (this.step == step) return;

        this.step = step;
        update(Dictionnary.PROPERTY.STEP, step);
    }

    public void setPage(final int page) {
        if (this.page == page) return;

        this.page = page;
        update(Dictionnary.PROPERTY.PAGE, page);
    }

    public void setDecimals(final int decimals) {
        if (this.decimals == decimals) return;

        this.decimals = decimals;
        update(Dictionnary.PROPERTY.DECIMAL, decimals);
    }

    private void update(final String property, final Object v) {
        final Update update = new Update(ID);
        update.put(property, v);
        getUIContext().stackInstruction(update);
    }

    public static class Options {

        protected Double min;
        protected Double max;
        protected Double step;
        protected Integer page;
        protected Integer decimals;

        public Options() {}

        public Options withMin(final Double m) {
            this.min = m;
            return this;
        }

        public Options withMax(final Double m) {
            this.max = m;
            return this;
        }

        public Options withPage(final Integer p) {
            this.page = p;
            return this;
        }

        public Options withStep(final Double s) {
            this.step = s;
            return this;
        }

        public Options withNumberFormat(final Integer nf) {
            this.decimals = nf;
            return this;
        }
    }

}
