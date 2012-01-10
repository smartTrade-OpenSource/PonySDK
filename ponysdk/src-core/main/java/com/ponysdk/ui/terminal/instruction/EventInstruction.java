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
package com.ponysdk.ui.terminal.instruction;

import com.ponysdk.ui.terminal.HandlerType;

public class EventInstruction extends Instruction {

	private static final long serialVersionUID = -3194913240376942319L;
	
	private HandlerType handlerType;

    public EventInstruction() {
    }

    public EventInstruction(long objectID, HandlerType handlerType) {
        super(objectID);
        this.handlerType = handlerType;
    }

    public HandlerType getHandlerType() {
        return this.handlerType;
    }

    public void setHandlerType(HandlerType handlerType) {
        this.handlerType = handlerType;
    }

    @Override
    public String toString() {
        return "EventInstruction [handlerType=" + handlerType + ", objectID=" + objectID + ", parentID=" + parentID + ", property=" + property + "]";
    }

}
