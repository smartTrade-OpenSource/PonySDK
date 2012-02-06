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

import java.io.FileWriter;
import java.io.StringWriter;

public class CodeWriter {

    private StringWriter stringwriter = null;

    private int tabposition = 0;

    public CodeWriter() {
        stringwriter = new StringWriter();
    }

    public int getTabposition() {
        return tabposition;
    }

    public void clear() {
        stringwriter.flush();
    }

    public void addNewLine() {
        addLine("");
    }

    public void resetTab() {
        tabposition = 0;
    }

    public void indentBlock() {
        tabposition++;
    }

    public void unindentBlock() {
        tabposition--;
    }

    public void setTabposition(int atabposition) {
        tabposition = atabposition;
    }

    public String getContent() {
        return stringwriter.toString();
    }

    protected String equivalentTabString(int atabposition) {
        String res = "";

        for (int k = 0; k <= (atabposition - 1); k++)
            res = res + "\t";

        return res;
    }

    public void addLine(int tabpos, String line) {
        stringwriter.write(equivalentTabString(tabpos) + line + "\n");
    }

    public void addLine(String line) {
        addLine(getTabposition(), line);
    }

    public void append(String line) {
        if (line == null) return;
        stringwriter.write(line);
    }

    public void saveToFile(String filename) {
        try {
            final FileWriter fw = new FileWriter(filename);
            fw.write(getContent());
            fw.flush();
            fw.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
