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

/**
 * {@link Cell} made visible when {@link PrimaryCellController#setExtendedMode()} is called.<br><br>
 * {@link ExtendedCell}'s height is not constrained as it is the case for a {@link PrimaryCell}.
 * 
 * @author mbagdouri
 * @see Cell
 * @see PrimaryCellController#setExtendedMode()
 */
public interface ExtendedCell<V> extends Cell<V, ExtendedCellController<V>> {
}