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
package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.List;

public class PRadioButtonGroup {

    private final List<PRadioButton> radioButtons = new ArrayList<PRadioButton>();

    private final String group;

    public PRadioButtonGroup(String group) {
        this.group = group;
    }

    public void registerRadioButton(PRadioButton radioButton) {
        radioButtons.add(radioButton);
        radioButton.setName(group);
    }

    public void unregisterRadioButton(PRadioButton radioButton) {
        radioButtons.remove(radioButton);
    }

    public PRadioButton getValue() {
        for (final PRadioButton radioButton : radioButtons) {
            if (radioButton.getValue()) {
                return radioButton;
            }
        }
        return null;
    }
}
