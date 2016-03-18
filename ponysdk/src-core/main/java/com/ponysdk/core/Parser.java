
package com.ponysdk.core;

import java.util.Collection;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.ponysdk.ui.terminal.model.Model;

public interface Parser {

    void reset();

    void beginObject();

    void endObject();

    void beginArray();

    void endArray();

    void comma();

    void quote();

    void parseKey(byte[] key);

    void parse(JsonValue jsonObject);

    void parse(Model model);

    void parse(Model model, String value);

    void parse(Model model, JsonObjectBuilder builder);

    void parse(Model model, boolean value);

    void parse(Model model, long value);

    void parse(Model model, int value);

    void parse(Model model, double value);

    void parse(Model model, Collection<String> collection);

    void parse(Model model, JsonValue jsonObject);

}
