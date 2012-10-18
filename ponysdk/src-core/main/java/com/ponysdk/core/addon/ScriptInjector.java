
package com.ponysdk.core.addon;

import java.util.Collection;

public interface ScriptInjector {

    public Addon getAddon();

    public Collection<String> getScripts();
}
