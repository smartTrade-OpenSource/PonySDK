/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.model;

/**
 * Binary protocol versions. The terminal announces the highest version it understands with the
 * {@link ClientToServerModel#PROTOCOL_VERSION} WebSocket handshake parameter; the server only uses a
 * feature when the connection announced it, so old terminals keep receiving the version 1 format and
 * new terminals still decode it (it is a strict subset).
 */
public final class ProtocolVersion {

    /** Original format (no parameter sent). */
    public static final int V1 = 1;

    /** Update runs: {@link ServerToClientModel#UPDATE_NEXT} / {@code _DELTA} / {@code _ID} inside an update block. */
    public static final int UPDATE_RUNS = 2;

    /**
     * Create in parent: a create block may end with {@link ServerToClientModel#PARENT_OBJECT_ID}, which adds the
     * new object to that parent ({@code [CREATE x ... PARENT p END]} instead of {@code [CREATE x ... END][ADD x
     * PARENT p END]}). Sent when the add immediately follows the create: every leaf widget.
     */
    public static final int CREATE_IN_PARENT = 3;
    /**
     * Listeners (docs/decisions/0003-protocol-v4-listeners.md): {@link ServerToClientModel#TYPE_REMOVE_HANDLER} is
     * followed by {@link ServerToClientModel#WIDGET_ID}, its object; DOM events are listened to by name
     * ({@link ServerToClientModel#LISTEN_EVENT}, {@link ServerToClientModel#UNLISTEN_EVENT}).
     */
    public static final int LISTENERS = 4;

    /** Highest version implemented by this terminal / server. */
    public static final int CURRENT = LISTENERS;

    private ProtocolVersion() {
    }
}
