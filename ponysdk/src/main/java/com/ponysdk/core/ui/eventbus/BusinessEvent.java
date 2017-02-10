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

package com.ponysdk.core.ui.eventbus;

public abstract class BusinessEvent<H extends EventHandler> extends Event<H> {

    private String businessMessage;
    private Level level = Level.INFO;

    public BusinessEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    public String getBusinessMessage() {
        return businessMessage;
    }

    public void setBusinessMessage(final String businessMessage) {
        this.businessMessage = businessMessage;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(final Level level) {
        this.level = level;
    }

    public enum Level {
        INFO,
        WARNING,
        ERROR
    }

}
