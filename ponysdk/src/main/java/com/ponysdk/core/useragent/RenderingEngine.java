/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.useragent;

public enum RenderingEngine {

    /**
     * Trident is the the Microsoft layout engine, mainly used by Internet
     * Explorer.
     */
    TRIDENT("Trident"),
    /**
     * HTML parsing and rendering engine of Microsoft Office Word, used by some
     * other products of the Office suite instead of Trident.
     */
    WORD("Microsoft Office Word"),
    /**
     * Open source and cross platform layout engine, used by Firefox and many
     * other browsers.
     */
    GECKO("Gecko"),
    /**
     * Layout engine based on KHTML, used by Safari, Chrome and some other
     * browsers.
     */
    WEBKIT("WebKit"),
    /**
     * Proprietary layout engine by Opera Software ASA
     */
    PRESTO("Presto"),
    /**
     * Original layout engine of the Mozilla browser and related products.
     * Predecessor of Gecko.
     */
    MOZILLA("Mozilla"),
    /**
     * Layout engine of the KDE project
     */
    KHTML("KHTML"),
    /**
     * Other or unknown layout engine.
     */
    OTHER("Other");

    String name;

    RenderingEngine(final String name) {
        this.name = name;
    }

}
