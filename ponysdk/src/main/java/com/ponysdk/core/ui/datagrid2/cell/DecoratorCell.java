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

import java.util.Optional;

import com.ponysdk.core.ui.basic.PWidget;

/**
 * @author mbagdouri
 */
public class DecoratorCell<V, DELEGATE extends PrimaryCell<V>> implements PrimaryCell<V> {

    private final DELEGATE cell;

    public DecoratorCell(final DELEGATE cell) {
        this.cell = cell;
    }

    @Override
    public PWidget asWidget() {
        return cell.asWidget();
    }

    @Override
    public PWidget asPendingWidget() {
        return cell.asPendingWidget();
    }

    @Override
    public Optional<ExtendedCell<V>> genExtended() {
        return cell.genExtended();
    }

    @Override
    public void render(final V data, final Object renderingHelper) {
        cell.render(data, renderingHelper);
    }

    @Override
    public void setController(final PrimaryCellController<V> cellController) {
        cell.setController(cellController);
    }

    @Override
    public void select() {
        cell.select();
    }

    @Override
    public void unselect() {
        cell.unselect();
    }

    public DELEGATE getDelegate() {
        return cell;
    }
}