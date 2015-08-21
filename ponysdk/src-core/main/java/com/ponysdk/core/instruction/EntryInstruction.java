
package com.ponysdk.core.instruction;

import javax.json.JsonObject;

import com.ponysdk.ui.terminal.model.Model;

public class EntryInstruction {

    public Model key;
    public String stringValue;
    public long longValue;
    public boolean booleanValue;
    public JsonObject jsonObject;

    public EntryInstruction(final Model key, final String value) {
        this.key = key;
        this.stringValue = value;
    }

    public EntryInstruction(final Model key, final long value) {
        this.key = key;
        this.longValue = value;
    }

    public EntryInstruction(final Model key, final boolean value) {
        this.key = key;
        this.booleanValue = value;
    }

    public EntryInstruction(final Model key, final JsonObject value) {
        this.key = key;
        this.jsonObject = value;
    }
}
