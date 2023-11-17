/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.ui.list;

import com.ponysdk.core.ui.basic.PAcceptsOneWidget;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.rich.PToolbar;

public interface ComplexListView extends SimpleListView {

    void setSearchResultInformation(String text);

    void addCustomInformation(String text);

    void addAction(String caption, PClickHandler clickHandler);

    PAcceptsOneWidget getTopListLayout();

    PToolbar getToolbarLayout();

    PAcceptsOneWidget getBottomListLayout();

    PAcceptsOneWidget getFormLayout();

    void setFloatableToolBar(PScrollPanel ancestorScrollPanel);

    PAcceptsOneWidget getPagingLayout();

    PSimplePanel getPreferencesLayout();

    void updateView();

}
