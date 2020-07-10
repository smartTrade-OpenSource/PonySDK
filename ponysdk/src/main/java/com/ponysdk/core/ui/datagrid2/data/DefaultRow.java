/*
 * Copyright (c) 2020 PonySDK
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

package com.ponysdk.core.ui.datagrid2.data;

import java.util.Objects;

/**
 *
 */
public class DefaultRow<V> {

    private final int id;
    private V data;
    private boolean accepted;

    public DefaultRow(final int id, final V data) {
        this.id = id;
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public int hashCode() {
        // FIXME : This fix was implemented to avoid duplicated keys
        // (only different id) in DB dataSrc
        return data.hashCode();
        // return id;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DefaultRow)
            return ((DefaultRow<V>) obj).getData().equals(data) && ((DefaultRow<V>) obj).isAccepted() == accepted;
        else return false;
    }

    @Override
    public String toString() {
        return "Row [id=" + id + ", data=" + data + ", accepted=" + accepted + "]";
    }

    public V getData() {
        return data;
    }

    public void setData(final V data) {
        this.data = data;
    }

    public int getID() {
        return id;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAcceptance(final boolean accepted) {
        this.accepted = accepted;
    }
}
