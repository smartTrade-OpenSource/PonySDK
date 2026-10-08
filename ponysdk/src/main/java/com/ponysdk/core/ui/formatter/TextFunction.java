/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.ui.formatter;

import java.util.function.Function;

/**
 * An immutable function that takes an array as argument and returns a String.
 * It wraps a Java {@link Function} and the name of its JS equivalent, a global function defined by
 * the application in its own static script.
 * It can be used to create a PFunction in a PWindow
 */
public class TextFunction {

    private final Function<Object[], String> javaFunction;
    private final String jsFunctionName;

    private TextFunction(final Function<Object[], String> javaFunction, final String jsFunctionName) {
        super();
        this.javaFunction = javaFunction;
        this.jsFunctionName = jsFunctionName;
    }

    /**
     * Sample :
     * <ul>
     * <li><pre>javaFunction : args -&#62; args[0] + " " + args[1]</pre></li>
     * <li>jsFunctionName : joinArgs, defined in an application script as
     * <pre>function joinArgs(args) { return args[0] + ' ' + args[1]; }</pre></li>
     * </ul>
     *
     * @param javaFunction   The java function.
     * @param jsFunctionName The name of the global js function, which receives the args array and returns a string.
     * @return the text function
     */
    public static TextFunction named(final Function<Object[], String> javaFunction, final String jsFunctionName) {
        return new TextFunction(javaFunction, jsFunctionName);
    }

    public String getJsFunctionName() {
        return jsFunctionName;
    }

    public Function<Object[], String> getJavaFunction() {
        return javaFunction;
    }

    @Override
    public String toString() {
        return "function " + jsFunctionName + "(args)";
    }

}
