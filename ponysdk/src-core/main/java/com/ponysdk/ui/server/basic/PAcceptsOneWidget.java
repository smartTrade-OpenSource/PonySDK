/*
 * Copyright 2008 Google Inc.
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

import com.ponysdk.ui.terminal.ui.PTSimplePanel;

public interface PAcceptsOneWidget {

    /**
     * Set the only widget of the receiver, replacing the previous widget if there was one.
     * 
     * @param w
     *            the widget, or <code>null</code> to remove the widget
     * @see PTSimplePanel
     */
    void setWidget(IsPWidget w);
}