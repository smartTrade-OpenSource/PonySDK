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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseGenerator {

    protected Domain domain;

    protected final Map<String, CriteriaFieldProperties> criteriaFieldByID = new HashMap<String, CriteriaFieldProperties>();

    protected final Map<String, ExportFieldProperties> exportFieldByID = new HashMap<String, ExportFieldProperties>();

    protected final Map<String, ListFieldProperties> listFieldByID = new HashMap<String, ListFieldProperties>();

    public BaseGenerator() {}

    public BaseGenerator(final Domain domain) {
        this.domain = domain;
        loadDescription();
    }

    private void loadDescription() {
        if (domain.getUi() == null) return;
        final List<Field> fields = domain.getUi().getFields();
        for (final Field field : fields) {
            if (field.getListFieldProperties() != null) {
                listFieldByID.put(field.getId(), field.getListFieldProperties());
            }
            if (field.getCriteriaFieldProperties() != null) {
                criteriaFieldByID.put(field.getId(), field.getCriteriaFieldProperties());
            }
            if (field.getExportFieldProperties() != null) {
                exportFieldByID.put(field.getId(), field.getExportFieldProperties());
            }
        }
    }

}
