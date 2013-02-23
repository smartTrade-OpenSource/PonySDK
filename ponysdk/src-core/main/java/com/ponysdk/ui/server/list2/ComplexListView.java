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

package com.ponysdk.ui.server.list2;

import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PToolbar;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public interface ComplexListView extends SimpleListView {

    public void setSearchResultInformation(String text);

    public void addCustomInformation(String text);

    public void addAction(String caption, PClickHandler clickHandler);

    public PAcceptsOneWidget getTopListLayout();

    public PToolbar getToolbarLayout();

    public PAcceptsOneWidget getBottomListLayout();

    public PAcceptsOneWidget getFormLayout();

    public void setFloatableToolBar(PScrollPanel ancestorScrollPanel);

    public PAcceptsOneWidget getPagingLayout();

    PSimplePanel getPreferencesLayout();

    public void updateView();

}
