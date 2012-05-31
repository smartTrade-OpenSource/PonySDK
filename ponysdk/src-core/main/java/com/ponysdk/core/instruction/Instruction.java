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

package com.ponysdk.core.instruction;

import java.util.Collection;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;

public class Instruction extends JSONObject {

    private static final Logger log = LoggerFactory.getLogger(Instruction.class);

    public Instruction(final JSONTokener jsonTokener) throws JSONException {
        super(jsonTokener);
    }

    public Instruction() {}

    public Instruction(final long objectID) {
        put(PROPERTY.OBJECT_ID, objectID);
    }

    public Instruction(final long objectID, final long parentID) {
        put(PROPERTY.OBJECT_ID, objectID);
        put(PROPERTY.PARENT_ID, parentID);
    }

    public long getObjectID() {
        return getLong(PROPERTY.OBJECT_ID);
    }

    public Long getParentID() {
        return getLong(PROPERTY.PARENT_ID);
    }

    public void setAddOnSignature(final String addOnSignature) {
        put("addOnSignature", addOnSignature);
    }

    public JSONObject put(final String arg0) {
        try {
            return super.put(arg0, "");
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public JSONObject put(final String arg0, final Collection arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public JSONObject put(final String arg0, final boolean arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public JSONObject put(final String arg0, final double arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public JSONObject put(final String arg0, final int arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    public JSONObject put(final String arg0, final String arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public JSONObject put(final String arg0, final long arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public JSONObject put(final String arg0, final Map arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public JSONObject put(final String arg0, final Object arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public JSONObject putOnce(final String arg0, final Object arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public JSONObject putOpt(final String arg0, final Object arg1) {
        try {
            return super.put(arg0, arg1);
        } catch (final JSONException e) {
            log.error("Cannot update Instruction", e);
        }
        return null;
    }

    @Override
    public String getString(final String arg0) {
        try {
            return super.getString(arg0);
        } catch (final JSONException e) {
            log.error("Cannot getString", e);
        }
        return null;
    }

    @Override
    public long getLong(final String arg0) {
        try {
            return super.getLong(arg0);
        } catch (final JSONException e) {
            log.error("Cannot getLong", e);
        }
        return -1;
    }

    @Override
    public boolean getBoolean(final String arg0) {
        try {
            return super.getBoolean(arg0);
        } catch (final JSONException e) {
            log.error("Cannot getBoolean", e);
        }
        return false;
    }

    @Override
    public int getInt(final String arg0) {
        try {
            return super.getInt(arg0);
        } catch (final JSONException e) {
            log.error("Cannot getInt", e);
        }
        return -1;
    }

    @Override
    public double getDouble(final String arg0) {
        try {
            return super.getDouble(arg0);
        } catch (final JSONException e) {
            log.error("Cannot getDouble", e);
        }
        return -1;
    }

}
