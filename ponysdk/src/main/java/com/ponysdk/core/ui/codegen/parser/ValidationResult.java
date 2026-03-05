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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of CEM file validation.
 */
public class ValidationResult {
    
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(List<String> errors, List<String> warnings) {
        this.errors = Collections.unmodifiableList(errors);
        this.warnings = Collections.unmodifiableList(warnings);
    }
    
    public static ValidationResult success() {
        return new ValidationResult(Collections.emptyList(), Collections.emptyList());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public static class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public Builder addError(String error) {
            errors.add(error);
            return this;
        }
        
        public Builder addWarning(String warning) {
            warnings.add(warning);
            return this;
        }
        
        public ValidationResult build() {
            return new ValidationResult(errors, warnings);
        }
    }
}
