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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class UiGenerator extends BaseGenerator {

    private String srcGeneratedDirectory = "src-generated-application";

    public UiGenerator(final Domain domain) {
        super(domain);
    }

    public void generate() throws Exception {
        generateListDescriptor();
    }

    private void generateListDescriptor() throws Exception {
        if (domain.getUi() == null) return;

        final ClassWriter classWriter = new ClassWriter(getSrcGeneratedDirectory(), GeneratorHelper.getUiPackage(domain), GeneratorHelper.getListDescriptorClassName(domain));

        classWriter.indentBlock();

        classWriter.addNewLine();
        classWriter.addLine("public static final Class<" + domain.getUi().getType() + "> DATATYPE = " + domain.getUi().getType() + ".class;");

        classWriter.addImport(Map.class);
        classWriter.addImport(LinkedHashMap.class);
        classWriter.addImport(Collections.class);
        classWriter.addImport("com.ponysdk.ui.server.list2.FieldDescriptor");

        classWriter.addNewLine();
        final ListProperties listProperties = domain.getUi().getListProperties();
        if (listProperties != null && listProperties.getCaption() != null) {
            classWriter.addLine("public static final String CAPTION = \"" + listProperties.getCaption() + "\";");
        }

        classWriter.addNewLine();
        for (final Field field : domain.getUi().getFields()) {
            classWriter.addLine("public static final String FIELD_" + field.getId().toUpperCase() + " = \"" + field.getId() + "\";");
        }

        classWriter.addNewLine();
        classWriter.addLine("private static final Map<String, Class<?>> fieldTypeByID = new LinkedHashMap<String, Class<?>>();");
        classWriter.addLine("private static final Map<String, String> fieldKeyByID = new LinkedHashMap<String, String>();");
        classWriter.addLine("private static final Map<String, String> fieldListCaptionByID = new LinkedHashMap<String, String>();");
        classWriter.addLine("private static final Map<String, String> fieldCriteriaByID = new LinkedHashMap<String, String>();");
        classWriter.addLine("private static final Map<String, String> fieldExportCaptionByID = new LinkedHashMap<String, String>();");
        classWriter.addLine("private static final Map<String, FieldDescriptor> fieldDescriptorByID = new LinkedHashMap<String, FieldDescriptor>();");

        classWriter.addNewLine();
        classWriter.addLine("static {");
        classWriter.indentBlock();

        for (final Field field : domain.getUi().getFields()) {
            classWriter.addLine("fieldTypeByID.put(FIELD_" + field.getId().toUpperCase() + ", " + field.getType() + ".class);");
        }

        classWriter.addNewLine();
        for (final Field field : domain.getUi().getFields()) {
            classWriter.addLine("fieldKeyByID.put(FIELD_" + field.getId().toUpperCase() + ", \"" + field.getKey() + "\");");
        }

        classWriter.addNewLine();
        for (final Field field : domain.getUi().getFields()) {
            final String caption = getListCaption(field);
            if (caption != null) {
                classWriter.addLine("fieldListCaptionByID.put(FIELD_" + field.getId().toUpperCase() + ", \"" + caption + "\");");
            }
        }

        classWriter.addNewLine();
        for (final Field field : domain.getUi().getFields()) {
            final String key = getCriteriaKey(field);
            if (key != null) {
                classWriter.addLine("fieldCriteriaByID.put(FIELD_" + field.getId().toUpperCase() + ", \"" + key + "\");");
            }
        }

        classWriter.addNewLine();
        for (final Field field : domain.getUi().getFields()) {
            final String caption = getExportCaption(field);
            if (caption != null) {
                classWriter.addLine("fieldExportCaptionByID.put(FIELD_" + field.getId().toUpperCase() + ", \"" + caption + "\");");
            }
        }

        classWriter.addNewLine();
        for (final Field field : domain.getUi().getFields()) {
            String listCaption = getListCaption(field);
            String criteriaKey = getCriteriaKey(field);
            String exportCaption = getExportCaption(field);
            if (listCaption != null) listCaption = "\"" + listCaption + "\"";
            if (criteriaKey != null) criteriaKey = "\"" + criteriaKey + "\"";
            if (exportCaption != null) exportCaption = "\"" + exportCaption + "\"";
            classWriter.addLine("fieldDescriptorByID.put(FIELD_" + field.getId().toUpperCase() + ", new FieldDescriptor(\"" + field.getId() + "\",\"" + field.getKey() + "\"," + field.getType() + ".class," + listCaption + "," + criteriaKey
                    + "," + exportCaption + "));");
        }

        // public FieldDescriptor(final String iD, final String key, final Class<?> type, final String
        // listCaption, final String criteriaKey, final String exportCaption)

        classWriter.unindentBlock();
        classWriter.addLine("}");

        classWriter.addNewLine();
        classWriter.addLine("public static Map<String, Class<?>> getFieldTypeByID() { return Collections.unmodifiableMap(fieldTypeByID); }");
        classWriter.addLine("public static Map<String, String> getFieldKeyByID() { return Collections.unmodifiableMap(fieldKeyByID); }");
        classWriter.addLine("public static Map<String, String> getFieldListCaptionByID() { return Collections.unmodifiableMap(fieldListCaptionByID); }");
        classWriter.addLine("public static Map<String, String> getFieldCriteriaByID() { return Collections.unmodifiableMap(fieldCriteriaByID); }");
        classWriter.addLine("public static Map<String, String> getFieldExportCaptionByID() { return Collections.unmodifiableMap(fieldExportCaptionByID); }");
        classWriter.addLine("public static Map<String, FieldDescriptor> getFieldDescriptorByID() { return Collections.unmodifiableMap(fieldDescriptorByID); }");

        classWriter.unindentBlock();

        classWriter.generateContentAndStore();
    }

    private String getListCaption(final Field field) {
        final ListFieldProperties properties = field.getListFieldProperties();
        if (properties == null) return field.getCaption();
        if (properties.isVisible()) return properties.getCaption();
        return null;
    }

    private String getExportCaption(final Field field) {
        final ExportFieldProperties properties = field.getExportFieldProperties();
        if (properties == null) return field.getCaption();
        if (properties.isExportable()) return properties.getCaption();
        return null;
    }

    private String getCriteriaKey(final Field field) {
        final CriteriaFieldProperties properties = field.getCriteriaFieldProperties();
        if (properties == null) return field.getKey();
        if (properties.isQueriable()) return properties.getKey();
        return null;
    }

    public String getSrcGeneratedDirectory() {
        return srcGeneratedDirectory;
    }

    public void setSrcGeneratedDirectory(final String srcGeneratedDirectory) {
        this.srcGeneratedDirectory = srcGeneratedDirectory;
    }
}
