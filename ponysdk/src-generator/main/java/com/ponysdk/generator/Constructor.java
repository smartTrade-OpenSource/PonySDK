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

package com.ponysdk.generator;

import java.util.ArrayList;
import java.util.List;

public class Constructor {

    private List<Parameter> constructorParameters = new ArrayList<Parameter>();

    private List<Parameter> superConstructorParameters = new ArrayList<Parameter>();

    public Constructor() {}

    public Constructor(List<Parameter> constructorParameters, List<Parameter> superConstructorParameters) {
        this.constructorParameters = constructorParameters;
        this.superConstructorParameters = superConstructorParameters;
    }

    public List<Parameter> getConstructorParameters() {
        return constructorParameters;
    }

    public void setConstructorParameters(List<Parameter> constructorParameters) {
        this.constructorParameters = constructorParameters;
    }

    public List<Parameter> getSuperConstructorParameters() {
        return superConstructorParameters;
    }

    public void setSuperConstructorParameters(List<Parameter> superConstructorParameters) {
        this.superConstructorParameters = superConstructorParameters;
    }
}
