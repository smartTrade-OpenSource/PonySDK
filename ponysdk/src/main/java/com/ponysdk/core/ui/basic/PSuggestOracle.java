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

package com.ponysdk.core.ui.basic;

import java.util.Collection;

public abstract class PSuggestOracle extends PObject {

    PSuggestOracle() {
    }

    public abstract void add(final String suggestion);

    public abstract void addAll(final Collection<String> collection);

    /**
     * Suggestion supplied by the {@link PSuggestOracle}. Each suggestion has a display string and a
     * replacement string. The display string is what is shown in the PSuggestBox's list of
     * suggestions. The interpretation of the display string depends upon the value of its oracle's.
     * The replacement string is the string that is entered into the PSuggestBox's text box when the
     * suggestion is selected from the list.
     * <p>
     * Replacement strings are useful when the display form of a suggestion differs from the input
     * format for the data. For example, suppose that a company has a webpage with a form which
     * requires the user to enter the e-mail address of an employee. Since users are likely to know
     * the name of the employee, a PSuggestBox is used to provide name suggestions as the user
     * types. When the user types the letter
     * <i>f</i>, a suggestion with the display string <i>foo bar</i> appears. When the user chooses
     * this suggestion, the replacement string, <i>foobar@company.com</i>, is entered into the
     * PSuggestBox's text box.
     * </p>
     * <p>
     * This is an example where the input data format for the suggestion is not as user-friendly as
     * the display format. In the eventbus that the display of a suggestion exactly matches the
     * input data format, the <code>Suggestion</code> interface would be implemented in such a way
     * that the display string and replacement string would be identical.
     * </p>
     * <h2>Associating Data Transfer Objects (DTOs) with Suggestion Objects</h2> Some applications
     * retrieve suggestions from a server, and may want to send back a DTO with each suggestion. In
     * the previous example, a DTO returned with the suggestion may provide additional contact
     * information about the selected employee, and this information could be used to fill out other
     * fields on the form. To send back a DTO with each suggestion, extend the
     * <code>Suggestion</code> interface and define a getter method that has a return value of the
     * DTO's type. Define a class that implements this sub-interface and use it to encapsulate each
     * suggestion.
     * <p>
     * To access a suggestion's DTO when the suggestion is selected, add a
     * {@link com.ponysdk.core.ui.basic.event.PChangeHandler} to the PSuggestBox (see PSuggestBox's
     * documentation for more information). In the
     * <code>SuggestionHandler.onSuggestionSelected(PSuggestionEvent eventbus)</code> method, obtain
     * the selected <code>Suggestion</code> object from the
     * {@link com.ponysdk.core.ui.basic.event.PChangeHandler} object, and downcast the
     * <code>Suggestion</code> object to the sub-interface. Then, access the DTO using the DTO
     * getter method that was defined on the sub-interface.
     * </p>
     */
    public interface PSuggestion {

        /**
         * Gets the display string associated with this suggestion. The interpretation of the
         * display string depends upon the value of its oracle's.
         *
         * @return the display string for this suggestion
         */
        String getDisplayString();

        /**
         * Gets the replacement string associated with this suggestion. When this suggestion is
         * selected, the replacement string will be entered into the PSuggestBox's text box.
         *
         * @return the string to be entered into the PSuggestBox's text box when this suggestion is
         *         selected
         */
        String getReplacementString();
    }
}