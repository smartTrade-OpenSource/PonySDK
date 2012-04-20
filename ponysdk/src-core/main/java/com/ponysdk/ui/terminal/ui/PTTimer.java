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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.user.client.Timer;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTTimer extends AbstractPTObject {

    private Timer timer;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        timer = new Timer() {

            @Override
            public void run() {
                final PTInstruction eventInstruction = new PTInstruction();
                eventInstruction.setObjectID(create.getObjectID());
                eventInstruction.put(TYPE.KEY, TYPE.EVENT);
                eventInstruction.put(HANDLER.KEY, HANDLER.TIMER);
                uiService.triggerEvent(eventInstruction);
            }
        };
    }

    @Override
    public void remove(final PTInstruction remove, final UIService uiService) {
        timer.cancel();
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.REPEATING_DELAY)) {
            timer.scheduleRepeating(update.getInt(PROPERTY.REPEATING_DELAY));
        } else if (update.containsKey(PROPERTY.DELAY)) {
            timer.schedule(update.getInt(PROPERTY.DELAY));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void gc(final PTInstruction gc, final UIService uiService) {
        timer.cancel();
    }
}
