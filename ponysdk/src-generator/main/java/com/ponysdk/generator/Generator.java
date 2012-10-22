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
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

public class Generator {

    public static void main(final String[] args) {
        try {
            final Generator generator = new Generator();
            if (args.length == 2) {
                generator.generateAll(args[0], args[1]);
            } else {
                generator.generateAll(args[0], null);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void generateAll(final String directories, final String srcGeneratedDirectory) throws Exception {

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

                final ServiceGenerator serviceGenerator = new ServiceGenerator(root.getDomain());
                if (srcGeneratedDirectory != null) {
                    serviceGenerator.setSrcGeneratedDirectory(srcGeneratedDirectory);
                }
                serviceGenerator.generate();

                final UiGenerator uiGenerator = new UiGenerator(root.getDomain());
                if (srcGeneratedDirectory != null) {
                    uiGenerator.setSrcGeneratedDirectory(srcGeneratedDirectory);
                }
                uiGenerator.generate();
            }
        }
    }

    private List<File> read(final File f) {
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

}
