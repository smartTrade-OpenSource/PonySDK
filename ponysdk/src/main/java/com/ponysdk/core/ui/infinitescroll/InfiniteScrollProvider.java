/*
 * Copyright (c) 2021 PonySDK
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

package com.ponysdk.core.ui.infinitescroll;

import java.util.List;
import java.util.function.Consumer;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickHandler;

/**
 *
 */

public interface InfiniteScrollProvider<D> {

    class Wrapper<T> implements IsPWidget {

        final PFlowPanel label = Element.newPFlowPanel();
        final PTextBox textBox = Element.newPTextBox();
        final PButton button = Element.newPButton();
        private T t;

        public Wrapper() {
            label.add(textBox);
            label.add(button);
            button.addStyleName("btn");
            button.addStyleName("fa fa-trash");

            //this.button.addClickHandler(e -> System.err.println(t));
        }

        @Override
        public PWidget asWidget() {
            return label;
        }

        public void setData(final T t) {
            this.t = t;
            textBox.setText(t.toString());

        }

        public void setVisible(final boolean b) {
            label.setVisible(b);
        }

        public void addClickerHandler(final PClickHandler pClickHandler) {
            this.button.addClickHandler(pClickHandler);
        }

        public T getData() {
            return t;
        }

    }

    List<D> getData(int beginIndex, int maxSize);

    long getSize();

    Wrapper buildItem(final D data);

    void updateItem(final int row, final D data, final Wrapper widget);

    void addHandler(Consumer<D> handler);

    //void onDataChange();

    //    default Wrapper buildItem(final D data) {
    //        final Wrapper wrapper = new Wrapper();
    //        wrapper.SetData(data);
    //        return wrapper;
    //    }

    //    default void updateItem(final int row, final D data, final Wrapper widget) {
    //        widget.SetData(data);
    //    }
}
