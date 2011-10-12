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
package com.ponysdk.ui.server;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.ponysdk.ui.terminal.PonyAddOn;

public class AddonListGenerator extends Generator {

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {

        final List<JClassType> ponyAddons = new ArrayList<JClassType>();

        final TypeOracle typeOracle = context.getTypeOracle();
        assert (typeOracle != null);

        JClassType classType = null;
        try {
            classType = typeOracle.getType(typeName);
        } catch (final NotFoundException e) {
            e.printStackTrace();
            return null;
        }

        final SourceWriter src = getSourceWriter(classType, context, logger);
        if (src == null) {
            return typeName + "Generated";
        }

        final JClassType[] types = typeOracle.getTypes();

        System.out.println("Generating AddonListGenerator " + typeName);
        System.out.println("\tAnalysing " + types.length + " types ...");

        for (final JClassType jClassType : types) {
            if (jClassType.isAnnotationPresent(PonyAddOn.class)) {
                System.out.println("\tAdding Pony Addon " + jClassType.getParameterizedQualifiedSourceName());
                ponyAddons.add(jClassType);
            }
        }

        // Here you would retrieve the metadata based on typeName for this Screen
        src.println("public List<AddonFactory> getAddonFactoryList() {");
        src.indent();
        src.println("List<AddonFactory> addonList = new ArrayList();");
        for (final JClassType addon : ponyAddons) {
            src.println("addonList.add(new AddonFactory(){");
            src.indent();
            src.println("public Addon newAddon(){");
            src.indent();
            src.println("Addon addon =  new " + addon.getParameterizedQualifiedSourceName() + "();");
            src.println("return addon;");
            src.outdent();
            src.println("}");
            src.println();

            src.println("public String getSignature(){");
            src.indent();
            src.println("return \"" + addon.getParameterizedQualifiedSourceName() + "\";");
            src.outdent();
            src.println("}");
            src.println();
            src.outdent();
            src.println("});");
        }
        src.println("return addonList;");
        src.outdent();
        src.println("}");

        src.commit(logger);
        System.out.println("Generating for: " + typeName);
        return typeName + "Generated";
    }

    public SourceWriter getSourceWriter(JClassType classType, GeneratorContext context, TreeLogger logger) {
        final String packageName = classType.getPackage().getName();
        final String simpleName = classType.getSimpleSourceName() + "Generated";
        final ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, simpleName);
        composer.setSuperclass(classType.getName());
        composer.addImplementedInterface("com.ponysdk.ui.terminal.AddonList");
        // Need to add whatever imports your generated class needs.
        composer.addImport("com.ponysdk.ui.terminal.AddonList");
        composer.addImport("com.ponysdk.ui.terminal.AddonFactory");
        composer.addImport("com.ponysdk.ui.terminal.PonyAddonList");
        composer.addImport("java.util.*");
        final PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
        if (printWriter == null) {
            return null;
        } else {
            final SourceWriter sw = composer.createSourceWriter(context, printWriter);
            return sw;
        }
    }

}
