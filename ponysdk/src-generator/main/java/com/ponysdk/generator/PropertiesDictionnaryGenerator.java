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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesDictionnaryGenerator {

    private static Logger log = LoggerFactory.getLogger(PropertiesDictionnaryGenerator.class);

    private final String srcGeneratedDirectory = "src-generated/main/java";
    private final String terminalPackageName = "com.ponysdk.ui";
    private final String packageName = "com.ponysdk.ui.terminal";
    private final String fileName = "Dictionnary";

    private boolean verbose = false;

    public PropertiesDictionnaryGenerator() {}

    public void generate(final String[] args) throws Exception {

        if (args.length > 0) {
            for (final String arg : args) {
                final String[] keyValue = arg.split("=");
                if (keyValue.length != 2) log.error("Invalid argument: " + arg);
                else {
                    final String key = keyValue[0];
                    final String value = keyValue[1];
                    if ("verbose".equals(key)) {
                        if (Boolean.valueOf(value)) verbose = true;
                    } else {
                        log.error("Unsupported argument: " + arg);
                    }
                }
            }
        }

        generateXml();
        generateDictionnary();
    }

    private void generateXml() {

        log.info("Generating: Generated.gwt.xml");

        final CodeWriter writer = new CodeWriter();
        writer.addLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.addLine("<module>");
        writer.addLine("    <source path='terminal' />");
        writer.addLine("</module>");

        final String terminalGeneratedDirectory = srcGeneratedDirectory + "/" + GeneratorHelper.getDirectoryFromPackage(terminalPackageName);
        final File file = new File(terminalGeneratedDirectory);
        if (!file.exists()) file.mkdirs();

        writer.saveToFile(terminalGeneratedDirectory + "/Generated.gwt.xml");
    }

    @SuppressWarnings("unchecked")
    private void generateDictionnary() throws FileNotFoundException, JSONException {
        log.info("Generating " + fileName);

        final String pathToFile = srcGeneratedDirectory + "/" + GeneratorHelper.getDirectoryFromPackage(packageName) + "/";

        final TokenGenerator tokenGenerator = new TokenGenerator();

        final FileReader fileReader = new FileReader("src-core/main/resources/spec/propertiesDictionnary.json");
        final JSONTokener tokener = new JSONTokener(fileReader);

        final CodeWriter writer = new CodeWriter();
        writer.addLine("package " + packageName + ";");
        writer.addNewLine();
        writer.addLine("public interface " + fileName + " {");
        writer.indentBlock();

        final JSONObject dico = new JSONObject(tokener);
        final Iterator<String> domainKeys = dico.sortedKeys();
        while (domainKeys.hasNext()) {
            // Domain
            final String domainKey = domainKeys.next();
            writer.addLine("public interface " + domainKey.toUpperCase() + " {");
            writer.indentBlock();

            final JSONObject domain = dico.getJSONObject(domainKey);
            final Iterator<String> keys = domain.sortedKeys();
            while (keys.hasNext()) {
                final String key = keys.next();
                final String keyUpper = GeneratorHelper.toUpperUnderscore(key);
                final JSONArray values = domain.getJSONArray(key);
                if (verbose) writer.addLine("public static final String " + keyUpper + " = \"" + key + "\";");
                else writer.addLine("public static final String " + keyUpper + " = \"" + tokenGenerator.nextToken() + "\";");

                if (values.length() > 0) {
                    writer.addLine("public interface " + keyUpper + "_" + " {");
                    writer.indentBlock();
                    for (int i = 0; i < values.length(); i++) {
                        if (verbose) writer.addLine("public static final String " + GeneratorHelper.toUpperUnderscore(values.getString(i)) + " = \"" + values.getString(i) + "\";");
                        else writer.addLine("public static final String " + GeneratorHelper.toUpperUnderscore(values.getString(i)) + " = \"" + tokenGenerator.nextToken() + "\";");
                    }
                    writer.unindentBlock();
                    writer.addLine("}");
                }
            }

            writer.unindentBlock();
            writer.addLine("}");
            writer.addNewLine();
        }

        writer.unindentBlock();
        writer.addLine("}");

        final File file = new File(pathToFile);
        if (!file.exists()) file.mkdirs();

        writer.saveToFile(pathToFile + fileName + ".java");
    }

    private class TokenGenerator {

        private int last = 0;
        private final List<String> tokens = new ArrayList<String>();

        public TokenGenerator() {
            final List<Character> characters = new ArrayList<Character>();
            for (int i = 48; i <= 57; i++)
                characters.add(new Character((char) i));
            for (int i = 65; i <= 90; i++)
                characters.add(new Character((char) i));
            for (int i = 97; i <= 122; i++)
                characters.add(new Character((char) i));

            for (final Character character : characters) {
                tokens.add(new String("" + character));
            }

            for (final Character c0 : characters) {
                for (final Character c1 : characters) {
                    tokens.add(new String("" + c0 + c1));
                }
            }

            for (final Character c0 : characters) {
                for (final Character c1 : characters) {
                    for (final Character c2 : characters) {
                        tokens.add(new String("" + c0 + c1 + c2));
                    }
                }
            }
        }

        public String nextToken() {
            return tokens.get(last++);
        }

    }

    public static void main(final String[] args) {
        try {
            new PropertiesDictionnaryGenerator().generate(args);
        } catch (final Exception e) {
            log.error("generation failed", e);
        }
    }
}
