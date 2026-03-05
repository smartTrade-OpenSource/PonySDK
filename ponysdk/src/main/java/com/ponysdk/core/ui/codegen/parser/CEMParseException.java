/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.codegen.parser;

/**
 * Exception thrown when CEM parsing fails.
 */
public class CEMParseException extends Exception {
    
    private final int lineNumber;
    private final String fieldPath;
    
    public CEMParseException(String message) {
        this(message, -1, null);
    }
    
    public CEMParseException(String message, Throwable cause) {
        this(message, -1, null, cause);
    }
    
    public CEMParseException(String message, int lineNumber, String fieldPath) {
        super(formatMessage(message, lineNumber, fieldPath));
        this.lineNumber = lineNumber;
        this.fieldPath = fieldPath;
    }
    
    public CEMParseException(String message, int lineNumber, String fieldPath, Throwable cause) {
        super(formatMessage(message, lineNumber, fieldPath), cause);
        this.lineNumber = lineNumber;
        this.fieldPath = fieldPath;
    }
    
    private static String formatMessage(String message, int lineNumber, String fieldPath) {
        StringBuilder sb = new StringBuilder(message);
        if (fieldPath != null) {
            sb.append(" at field '").append(fieldPath).append("'");
        }
        if (lineNumber > 0) {
            sb.append(" (line ").append(lineNumber).append(")");
        }
        return sb.toString();
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getFieldPath() {
        return fieldPath;
    }
}
