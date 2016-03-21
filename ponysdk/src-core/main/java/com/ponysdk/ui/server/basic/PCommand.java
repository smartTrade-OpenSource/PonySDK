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

package com.ponysdk.ui.server.basic;

/**
 * Encapsulates an action for later execution, often from a different context.
 * <p>
 * The Command interface provides a layer of separation between the code specifying some behavior and the code
 * invoking that behavior. This separation aids in creating reusable code. For example, a {@link PMenuItem}
 * can have a PCommand associated with it that it executes when the menu item is chosen by the user.
 * Importantly, the code that constructed the PCommand to be executed when the menu item is invoked knows
 * nothing about the internals of the MenuItem class and vice-versa.
 * </p>
 */

@FunctionalInterface
public interface PCommand {

    void execute();
}
