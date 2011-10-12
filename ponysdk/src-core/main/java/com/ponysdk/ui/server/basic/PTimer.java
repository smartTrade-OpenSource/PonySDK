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

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Remove;
import com.ponysdk.ui.terminal.instruction.Update;

public abstract class PTimer extends PObject {

    @Override
    protected WidgetType getType() {
        return WidgetType.TIMER;
    }

    public void scheduleRepeating(int delayMillis) {
        final Update update = new Update(ID);
        update.setMainPropertyValue(PropertyKey.DELAY, delayMillis);
        PonySession.getCurrent().stackInstruction(update);
    }

    @Override
    public void onEventInstruction(EventInstruction instruction) {
        if (HandlerType.TIMER.equals(instruction.getHandlerType())) {
            run();
        } else {
            super.onEventInstruction(instruction);
        }
    }

    public void cancel() {
        final Remove remove = new Remove(ID, -1);
        PonySession.getCurrent().stackInstruction(remove);
    }

    public abstract void run();

}
