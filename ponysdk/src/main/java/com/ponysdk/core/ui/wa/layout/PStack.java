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

package com.ponysdk.core.ui.wa.layout;

import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Stack layout component supporting vertical and horizontal orientation.
 * <p>
 * Arranges child components in a flex-based stack with configurable gap,
 * alignment (cross-axis), justification (main-axis), and wrap behavior.
 * </p>
 */
public class PStack extends PWebComponent<StackProps> {

    public PStack() {
        super(StackProps.defaults());
    }

    public PStack(final StackProps initialProps) {
        super(initialProps);
    }

    @Override
    protected Class<StackProps> getPropsClass() {
        return StackProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "wa-stack";
    }

    public void setOrientation(final String orientation) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(orientation, current.gap(), current.alignment(),
            current.justification(), current.wrap(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setGap(final String gap) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(current.orientation(), gap, current.alignment(),
            current.justification(), current.wrap(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setAlignment(final String alignment) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(current.orientation(), current.gap(), alignment,
            current.justification(), current.wrap(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setJustification(final String justification) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(current.orientation(), current.gap(), current.alignment(),
            justification, current.wrap(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setWrap(final boolean wrap) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(current.orientation(), current.gap(), current.alignment(),
            current.justification(), wrap,
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setHideOnMobile(final boolean hide) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(current.orientation(), current.gap(), current.alignment(),
            current.justification(), current.wrap(),
            hide, current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setHideOnTablet(final boolean hide) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(current.orientation(), current.gap(), current.alignment(),
            current.justification(), current.wrap(),
            current.hideOnMobile(), hide, current.hideOnDesktop()));
    }

    public void setHideOnDesktop(final boolean hide) {
        final StackProps current = getCurrentProps();
        setProps(new StackProps(current.orientation(), current.gap(), current.alignment(),
            current.justification(), current.wrap(),
            current.hideOnMobile(), current.hideOnTablet(), hide));
    }
}
