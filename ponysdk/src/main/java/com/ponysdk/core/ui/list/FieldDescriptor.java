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

package com.ponysdk.core.ui.list;

public class FieldDescriptor {

    private String ID;
    private String key;
    private Class<?> type;
    private String listCaption;
    private String criteriaKey;
    private String exportCaption;

    public FieldDescriptor(final String iD, final String key, final Class<?> type, final String listCaption, final String criteriaKey,
                           final String exportCaption) {
        this.ID = iD;
        this.key = key;
        this.type = type;
        this.listCaption = listCaption;
        this.criteriaKey = criteriaKey;
        this.exportCaption = exportCaption;
    }

    public String getID() {
        return ID;
    }

    public void setID(final String iD) {
        ID = iD;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(final Class<?> type) {
        this.type = type;
    }

    public String getListCaption() {
        return listCaption;
    }

    public void setListCaption(final String listCaption) {
        this.listCaption = listCaption;
    }

    public String getCriteriaKey() {
        return criteriaKey;
    }

    public void setCriteriaKey(final String criteriaKey) {
        this.criteriaKey = criteriaKey;
    }

    public String getExportCaption() {
        return exportCaption;
    }

    public void setExportCaption(final String exportCaption) {
        this.exportCaption = exportCaption;
    }

}
