
package com.ponysdk.jetty.test.bench.mock;

import org.json.JSONObject;

import com.ponysdk.jetty.test.bench.UI;

public class UIMock {

    protected final long objectID;

    public UIMock(final long objectID) {
        this.objectID = objectID;
    }

    public void update(final UI ui, final JSONObject instruction) throws Exception {}

}
