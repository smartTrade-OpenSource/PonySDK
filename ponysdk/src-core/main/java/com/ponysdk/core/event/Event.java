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

package com.ponysdk.core.event;

import java.util.UUID;

public abstract class Event<H extends EventHandler> {

    public static class Type<H> {

        private static int nextHashCode;

        private final int index;

        public Type() {
            index = ++nextHashCode;
        }

        @Override
        public final int hashCode() {
            return index;
        }

        @Override
        public String toString() {
            return "Event type";
        }
    }

    private static long count = 0;

    private final long eventID;

    private UUID uuid;

    private Object source;

    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    protected Event(final Object source) {
        this.source = source;
        eventID = count++;
    }

    protected Event(final Object source, final UUID uuid) {
        this.source = source;
        this.uuid = uuid;
        eventID = count++;
    }

    public Object getSource() {
        return source;
    }

    public String toDebugString() {
        String name = this.getClass().getName();
        name = name.substring(name.lastIndexOf(".") + 1);
        return "event: " + name + ":";
    }

    @Override
    public String toString() {
        return "An event type";
    }

    public long getEventID() {
        return eventID;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(final UUID uuid) {
        this.uuid = uuid;
    }

    void setSource(final Object source) {
        this.source = source;
    }

    public abstract Type<H> getAssociatedType();

    protected abstract void dispatch(H handler);
}
