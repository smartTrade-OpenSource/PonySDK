/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.core.ui.form2;

import com.ponysdk.core.ui.form2.api.ValidationResult;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValidationResultTest extends PSuite {

    @Test
    public void testValidationResult() {
        final ValidationResult result = ValidationResult.OK("Message", "Data");
        Assertions.assertTrue(result.isValid());
        Assertions.assertEquals("Message", result.getErrorMessage());
        Assertions.assertEquals("Data", result.getData());
        Assertions.assertEquals("ValidationResult{valid=true, errorMessage='Message', data=Data}", result.toString());
    }

    @Test
    public void testOKWithoutData() {
        final ValidationResult result = ValidationResult.OK();
        Assertions.assertTrue(result.isValid());
        Assertions.assertEquals(null, result.getErrorMessage());
        Assertions.assertEquals(null, result.getData());
        Assertions.assertEquals("ValidationResult{valid=true, errorMessage='null', data=null}", result.toString());
    }

    @Test
    public void testOKWithData() {
        final ValidationResult result = ValidationResult.OK("Data");
        Assertions.assertTrue(result.isValid());
        Assertions.assertEquals(null, result.getErrorMessage());
        Assertions.assertEquals("Data", result.getData());
        Assertions.assertEquals("ValidationResult{valid=true, errorMessage='null', data=Data}", result.toString());
    }

    @Test
    public void testKOWithoutData() {
        final ValidationResult result = ValidationResult.KO("Message");
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals("Message", result.getErrorMessage());
        Assertions.assertEquals(null, result.getData());
        Assertions.assertEquals("ValidationResult{valid=false, errorMessage='Message', data=null}", result.toString());
    }

    @Test
    public void testKOWithData() {
        final ValidationResult result = ValidationResult.KO("Message", "Data");
        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals("Message", result.getErrorMessage());
        Assertions.assertEquals("Data", result.getData());
        Assertions.assertEquals("ValidationResult{valid=false, errorMessage='Message', data=Data}", result.toString());
    }
}
