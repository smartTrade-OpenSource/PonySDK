/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.ui.datagrid2.cell;

import java.util.function.BiConsumer;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * @author mbagdouri
 */
public class LabelCell<V> implements Cell<V> {

    private final PLabel label = Element.newPLabel();
    private final PLabel pendingLabel = Element.newPLabel("...");
    private CellController<V> cellController;

    public LabelCell(final BiConsumer<V, String> columnEditFn, final int width) {
        label.addDoubleClickHandler(e -> {
            if (cellController != null) cellController.extendedMode(new TextBoxExtendedCell<>(label.getText(), columnEditFn, width));
        });
    }

    @Override
    public PLabel asWidget() {
        return label;
    }

    @Override
    public PWidget asPendingWidget() {
        return pendingLabel;
    }

    @Override
    public void render(final V data, final Object renderingHelper) {
        label.setText(renderingHelper.toString());
    }

    @Override
    public void setController(final CellController<V> cellController) {
        this.cellController = cellController;
    }

    @Override
    public void select() {
    }

    @Override
    public void unselect() {
    }

}