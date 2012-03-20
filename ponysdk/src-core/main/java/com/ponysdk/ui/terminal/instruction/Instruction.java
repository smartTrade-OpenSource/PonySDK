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

import java.io.Serializable;

import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;

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
public class Instruction implements Serializable {

    private static final long serialVersionUID = -8984390049516680155L;

    protected long objectID;

    protected Long parentID;

    protected String addOnSignature;

    protected Property property = new Property();

    public Instruction() {}

    public Instruction(final long objectID) {
        this.objectID = objectID;
    }

    public long getObjectID() {
        return objectID;
    }

    public Long getParentID() {
        return parentID;
    }

    public Property getMainProperty() {
        return property;
    }

    public void setMainProperty(final Property property) {
        this.property = property;
    }

    public void setMainPropertyKey(final PropertyKey key) {
        setMainPropertyValue(key, null);
    }

    public void setMainAddonPropertyKey(final String key) {
        setMainAddonPropertyValue(key, null);
    }

    public void setMainPropertyValue(final PropertyKey key, final String value) {
        this.property.setValue(value);
        this.property.setKey(key);
    }

    public void setMainAddonPropertyValue(final String key, final String value) {
        this.property.setValue(value);
        this.property.setAddonKey(key);
    }

    public void setMainPropertyValue(final PropertyKey key, final int value) {
        this.property.setValue(String.valueOf(value));
        this.property.setKey(key);
    }

    public void setMainAddonPropertyValue(final String key, final int value) {
        this.property.setValue(String.valueOf(value));
        this.property.setAddonKey(key);
    }

    public void setMainPropertyValue(final PropertyKey key, final long value) {
        this.property.setValue(String.valueOf(value));
        this.property.setKey(key);
    }

    public void setMainAddonPropertyValue(final String key, final long value) {
        this.property.setValue(String.valueOf(value));
        this.property.setAddonKey(key);
    }

    public void setMainPropertyValue(final PropertyKey key, final boolean value) {
        this.property.setValue(String.valueOf(value));
        this.property.setKey(key);
    }

    public void setMainAddonPropertyValue(final String key, final boolean value) {
        this.property.setValue(String.valueOf(value));
        this.property.setAddonKey(key);
    }

    public void setMainPropertyValue(final PropertyKey key, final double value) {
        this.property.setValue(String.valueOf(value));
        this.property.setKey(key);
    }

    public void setMainAddonPropertyValue(final String key, final double value) {
        this.property.setValue(String.valueOf(value));
        this.property.setAddonKey(key);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (objectID ^ (objectID >>> 32));
        result = prime * result + ((property == null) ? 0 : property.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Instruction other = (Instruction) obj;
        if (objectID != other.objectID) return false;
        if (property == null) {
            if (other.property != null) return false;
        } else if (!property.equals(other.property)) return false;
        return true;
    }

    public String getAddOnSignature() {
        return addOnSignature;
    }

    public void setAddOnSignature(final String addOnSignature) {
        this.addOnSignature = addOnSignature;
    }

    @Override
    public String toString() {
        return "Instruction [objectID=" + objectID + ", parentID=" + parentID + ", property=" + property + ", addonSignature=" + addOnSignature + "]";
    }

}
