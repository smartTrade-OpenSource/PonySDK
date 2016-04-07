
package com.ponysdk.core;

import java.util.Collection;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.ponysdk.ui.terminal.model.Model;

public interface Parser {

    void reset();

    void beginObject();

    void endObject();

    void parse(Model model);

    void parse(JsonValue jsonObject);

    void parse(Model model, JsonObjectBuilder builder);

    void parse(Model model, Collection<String> collection);

    void parse(Model model, JsonValue jsonObject);

    void parse(Model model, Object value);

}
