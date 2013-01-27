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

package com.ponysdk.spring;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.ponysdk.generator.BaseGenerator;
import com.ponysdk.generator.ClassWriter;
import com.ponysdk.generator.GeneratorHelper;
import com.ponysdk.generator.ObjectFactory;
import com.ponysdk.generator.Root;

public class ProxyBuilderGenerator extends BaseGenerator {

    private final List<Root> domains;

    private String srcGeneratedDirectory = "src-generated-application";

    private String packageName = "com.ponysdk.service";

    public static void main(final String[] args) {
        try {
            final List<Root> definitions = getDefinitions(args[0]);
            final ProxyBuilderGenerator generator = new ProxyBuilderGenerator(definitions);
            if (args.length >= 2) generator.setSrcGeneratedDirectory(args[1]);
            if (args.length >= 3) generator.setPackageName(args[2]);

            generator.generate();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Root> getDefinitions(final String directories) throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        final List<Root> definitions = new ArrayList<Root>();

        final String[] split = directories.split(",");
        for (final String d : split) {

            final List<File> files = read(new File(d));
            for (final File file : files) {
                System.out.println("file = " + file);
                final Root root = (Root) unmarshaller.unmarshal(file);
                definitions.add(root);
            }
        }
        return definitions;
    }

    private static List<File> read(final File f) {
        final List<File> files = new ArrayList<File>();
        if (f.isDirectory()) {
            final File[] childs = f.listFiles();
            for (final File child : childs) {
                files.addAll(read(child));
            }
        } else {
            if (f.getName().endsWith(".pony.xml")) {
                files.add(f);
            }
        }
        return files;
    }

    public ProxyBuilderGenerator(final List<Root> domains) {
        this.domains = domains;
    }

    public void generate() throws Exception {
        final ClassWriter classWriter = new ClassWriter(this, getSrcGeneratedDirectory(), getPackageName(), "ProxyBuilder");
        classWriter.addImplements(ApplicationContextAware.class);
        classWriter.addNewLine();
        classWriter.addLine("private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProxyBuilder.class);");

        classWriter.addLine("@Override");
        classWriter.addLine("public void setApplicationContext(" + ApplicationContext.class.getCanonicalName() + " context) throws " + BeansException.class.getCanonicalName() + " {");
        classWriter.addNewLine();

        for (final Root root : domains) {
            this.domain = root.getDomain();
            classWriter.addLine("try{");
            classWriter.indentBlock();
            classWriter.addLine("com.ponysdk.core.service.PonyServiceRegistry.registerPonyService(context.getBean(" + GeneratorHelper.getServiceFullClassName(domain) + ".class));");
            classWriter.unindentBlock();
            classWriter.addLine("}catch(org.springframework.beans.factory.NoSuchBeanDefinitionException e){");
            classWriter.indentBlock();
            classWriter.addLine("log.warn(\"No service defined for " + domain.getName() + "\");");
            classWriter.unindentBlock();
            classWriter.addLine("}");
            classWriter.addLine("catch(Exception e){");
            classWriter.indentBlock();
            classWriter.addLine("String errorMessage = \"Error when starting service " + domain.getName() + "\";");
            classWriter.addLine("log.error(errorMessage, e);");
            classWriter.addLine("throw new " + RuntimeException.class.getCanonicalName() + "(errorMessage, e);");

            classWriter.unindentBlock();
            classWriter.addLine("}");
        }
        classWriter.addLine("}");

        classWriter.generateContentAndStore();
    }

    public void setSrcGeneratedDirectory(final String srcGeneratedDirectory) {
        this.srcGeneratedDirectory = srcGeneratedDirectory;
    }

    public String getSrcGeneratedDirectory() {
        return srcGeneratedDirectory;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }
}
