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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ClassWriter {

    private final String destinationFolder;

    private final String packageName;

    private final String className;

    private boolean isAbstract = false;

    private boolean isInterface = false;

    private boolean generateGetter = false;

    private boolean generateSetter = false;

    private final CodeWriter codeWriter = new CodeWriter();

    private final Set<String> classImport = new HashSet<String>();

    private final Set<String> classExtensions = new HashSet<String>();

    private final Set<String> classImplements = new HashSet<String>();

    private final List<Parameter> classMembers = new ArrayList<Parameter>();

    private final List<Constructor> constructors = new ArrayList<Constructor>();

    private final List<String> constantes = new ArrayList<String>();

    private final List<MethodHolder> methodTemplates = new ArrayList<MethodHolder>();

    private final CodeWriter methodWriter;

    private final List<String> classAnnotations = new ArrayList<String>();

    private final Object generator;

    public ClassWriter(final Object generator, final String destinationFolder, final String packageName, final String className) {
        this.generator = generator;
        this.destinationFolder = destinationFolder;
        this.packageName = packageName;
        this.className = className;
        this.methodWriter = new CodeWriter();
    }

    public void addImport(final Class<?> clazz) {
        classImport.add(clazz.getCanonicalName());
    }

    public void addConstructor(final Constructor construtor) {
        this.constructors.add(construtor);
    }

    public void addImport(final String importFull) {
        classImport.add(importFull);
    }

    public void addClassAnnotation(final String line) {
        classAnnotations.add(line);
    }

    public void addConstants(final String constants) {
        constantes.add(constants);
    }

    public void addImplements(final Class<?> clazz) {
        addImplements(clazz.getCanonicalName());
    }

    public void addImplements(final String fullClazz) {
        classImplements.add(fullClazz);
    }

    public void addExtend(final Class<?> clazz) {
        classExtensions.add(clazz.getCanonicalName());
    }

    public void addExtend(final String fullyQualifiedClazz) {
        classExtensions.add(fullyQualifiedClazz);
    }

    public void addClassMembers(final Parameter parameter) {
        classMembers.add(parameter);
    }

    public void addMethod(final String template, final Object... params) {
        methodTemplates.add(new MethodHolder(template, params));
    }

    protected void printHeader() {
        codeWriter.addLine("/*");
        codeWriter.addLine(" * Copyright (c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        codeWriter.addLine(" *  Owners:");
        codeWriter.addLine(" *  Luciano Broussal  <luciano.broussal AT gmail.com>");
        codeWriter.addLine(" *  Mathieu Barbier   <mathieu.barbier AT gmail.com>");
        codeWriter.addLine(" *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>");
        codeWriter.addLine(" *  ");
        codeWriter.addLine(" *  WebSite:");
        codeWriter.addLine(" *  https://github.com/PonySDK/PonySDK");
        codeWriter.addLine(" * ");
        codeWriter.addLine(" * Licensed under the Apache License, Version 2.0 (the \"License\"); you may not");
        codeWriter.addLine(" * use this file except in compliance with the License. You may obtain a copy of");
        codeWriter.addLine(" * the License at");
        codeWriter.addLine(" *");
        codeWriter.addLine(" * http://www.apache.org/licenses/LICENSE-2.0");
        codeWriter.addLine(" *");
        codeWriter.addLine(" * Unless required by applicable law or agreed to in writing, software");
        codeWriter.addLine(" * distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT");
        codeWriter.addLine(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the");
        codeWriter.addLine(" * License for the specific language governing permissions and limitations under");
        codeWriter.addLine(" * the License.");
        codeWriter.addLine(" *");
        codeWriter.addLine(" * Warning: This file has been generated by " + generator.getClass() + "");
        codeWriter.addLine(" * DO NOT EDIT BY HAND");
        codeWriter.addLine(" *");
        codeWriter.addLine(" */");
        codeWriter.addLine("");
    }

    @SuppressWarnings("synthetic-access")
    public void generateContentAndStore() throws Exception {

        printHeader();

        // package
        codeWriter.addLine("package " + packageName + ";");
        codeWriter.addNewLine();

        // import
        if (!classImport.isEmpty()) {
            for (final String imp : classImport) {
                codeWriter.addLine("import " + imp + ";");
            }
            codeWriter.addNewLine();
        }

        if (!classAnnotations.isEmpty()) {
            for (final String line : classAnnotations) {
                codeWriter.addLine(line);
            }
        }

        // class definition
        codeWriter.append("public ");
        codeWriter.append(isAbstract ? "abstract " : "");
        codeWriter.append(isInterface ? "interface " : "class ");
        codeWriter.append(className + " ");
        if (!classExtensions.isEmpty()) {
            codeWriter.append("extends ");
            codeWriter.append(GeneratorHelper.collectionToString(classExtensions, ", "));
            codeWriter.append(" ");
        }
        if (!classImplements.isEmpty()) {
            codeWriter.append("implements ");
            codeWriter.append(GeneratorHelper.collectionToString(classImplements, ", "));
            codeWriter.append(" ");
        }
        codeWriter.append(" {");
        codeWriter.addNewLine();
        codeWriter.addNewLine();
        codeWriter.indentBlock();

        // static member
        if (!constantes.isEmpty()) {
            for (final String constants : constantes) {
                codeWriter.addLine(constants);
            }
            codeWriter.addNewLine();
        }

        // class members
        if (!classMembers.isEmpty()) {
            for (final Parameter parameter : classMembers) {
                codeWriter.addLine("protected " + GeneratorHelper.getClassName(parameter) + " " + parameter.getName() + ";");
            }
            codeWriter.addNewLine();
        }

        if (!isInterface) {
            for (final Constructor constructor : constructors) {
                String constructorParameters = "";
                final Iterator<Parameter> it = constructor.getConstructorParameters().iterator();
                while (it.hasNext()) {
                    final Parameter parameter = it.next();
                    constructorParameters += GeneratorHelper.getClassName(parameter) + " " + parameter.getName();
                    if (it.hasNext()) constructorParameters += ", ";
                }

                codeWriter.addLine("public " + className + "(" + constructorParameters + ") {");

                final Iterator<Parameter> it2 = constructor.getSuperConstructorParameters().iterator();
                while (it2.hasNext()) {
                    final Parameter parameter = it2.next();
                    codeWriter.addLine("super(" + parameter.getName() + ");");
                }

                for (final Parameter parameter : constructor.getConstructorParameters()) {
                    if (!constructor.getSuperConstructorParameters().contains(parameter)) {
                        codeWriter.addLine("this." + parameter.getName() + " = " + parameter.getName() + ";");
                    }
                }

                codeWriter.addLine("}");
                codeWriter.addNewLine();
            }
        }

        // getter
        if (generateGetter && !classMembers.isEmpty()) {
            for (final Parameter parameter : classMembers) {
                final String clazz = GeneratorHelper.getClassName(parameter);
                String nameUp = parameter.getName();
                nameUp = nameUp.substring(0, 1).toUpperCase() + nameUp.substring(1);
                String prefixe = "get";
                if (clazz.equals(boolean.class.getCanonicalName())) prefixe = "is";
                codeWriter.addLine("public " + clazz + " " + prefixe + nameUp + "() {");
                codeWriter.indentBlock();
                codeWriter.addLine("return " + parameter.getName() + ";");
                codeWriter.unindentBlock();
                codeWriter.addLine("}");
                codeWriter.addNewLine();
            }
        }

        // setter
        if (generateSetter && !classMembers.isEmpty()) {
            for (final Parameter parameter : classMembers) {
                final String clazz = GeneratorHelper.getClassName(parameter);
                String nameUp = parameter.getName();
                nameUp = nameUp.substring(0, 1).toUpperCase() + nameUp.substring(1);
                codeWriter.addLine("public void set" + nameUp + "(" + clazz + " arg) {");
                codeWriter.indentBlock();
                codeWriter.addLine("this. " + parameter.getName() + " = arg;");
                codeWriter.unindentBlock();
                codeWriter.addLine("}");
                codeWriter.addNewLine();
            }
        }

        // methods
        if (!methodTemplates.isEmpty()) {
            for (final MethodHolder methodHolder : methodTemplates) {
                // TODO better readable template
                final String result = String.format(methodHolder.template, methodHolder.params);
                codeWriter.addLine(result);
                codeWriter.addNewLine();
            }
        }

        // methodWriter
        final String customMethod = methodWriter.getContent();
        if (customMethod != null && !customMethod.isEmpty()) {
            codeWriter.addLine(customMethod);
        }

        codeWriter.unindentBlock();
        codeWriter.addLine("}");

        storeFile();
    }

    private void storeFile() {
        try {
            final String pathToClass = destinationFolder + "/" + packageName.replaceAll("\\.", "/") + "/";

            final File file = new File(pathToClass);
            if (!file.exists()) file.mkdirs();
            final FileWriter writer = new FileWriter(pathToClass + className + ".java");
            writer.write(codeWriter.getContent());
            writer.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private class MethodHolder {

        final String template;

        private final Object[] params;

        public MethodHolder(final String template, final Object[] params) {
            this.template = template;
            this.params = params;
        }

    }

    public CodeWriter getCodeWriter() {
        return codeWriter;
    }

    public void addLine(final String line) {
        methodWriter.addLine(line);
    }

    public void addNewLine() {
        methodWriter.addNewLine();
    }

    public void indentBlock() {
        methodWriter.indentBlock();
    }

    public void unindentBlock() {
        methodWriter.unindentBlock();
    }

    public void setAbstract(final boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public void setInterface(final boolean isInterface) {
        this.isInterface = isInterface;
    }

    public void setGenerateGetter(final boolean generateGetter) {
        this.generateGetter = generateGetter;
    }

    public void setGenerateSetter(final boolean generateSetter) {
        this.generateSetter = generateSetter;
    }

}
