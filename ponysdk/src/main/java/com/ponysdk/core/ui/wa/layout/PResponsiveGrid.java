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

import java.util.Map;

import com.ponysdk.core.ui.component.PWebComponent;

/**
 * Responsive grid layout component supporting configurable breakpoints.
 * <p>
 * Provides a CSS grid with a 12-column system by default, with breakpoint-specific
 * column and gap configurations for mobile (0-599px), tablet (600-1023px),
 * and desktop (1024px+).
 * </p>
 */
public class PResponsiveGrid extends PWebComponent<ResponsiveGridProps> {

    public PResponsiveGrid() {
        super(ResponsiveGridProps.defaults());
    }

    public PResponsiveGrid(final ResponsiveGridProps initialProps) {
        super(initialProps);
    }

    @Override
    protected Class<ResponsiveGridProps> getPropsClass() {
        return ResponsiveGridProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "responsive-grid";
    }

    public void setColumns(final int columns) {
        final ResponsiveGridProps current = getCurrentProps();
        setProps(new ResponsiveGridProps(columns, current.gap(), current.breakpoints(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setGap(final String gap) {
        final ResponsiveGridProps current = getCurrentProps();
        setProps(new ResponsiveGridProps(current.columns(), gap, current.breakpoints(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setBreakpoints(final Map<String, BreakpointConfig> breakpoints) {
        final ResponsiveGridProps current = getCurrentProps();
        setProps(new ResponsiveGridProps(current.columns(), current.gap(), breakpoints,
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setHideOnMobile(final boolean hide) {
        final ResponsiveGridProps current = getCurrentProps();
        setProps(new ResponsiveGridProps(current.columns(), current.gap(), current.breakpoints(),
            hide, current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setHideOnTablet(final boolean hide) {
        final ResponsiveGridProps current = getCurrentProps();
        setProps(new ResponsiveGridProps(current.columns(), current.gap(), current.breakpoints(),
            current.hideOnMobile(), hide, current.hideOnDesktop()));
    }

    public void setHideOnDesktop(final boolean hide) {
        final ResponsiveGridProps current = getCurrentProps();
        setProps(new ResponsiveGridProps(current.columns(), current.gap(), current.breakpoints(),
            current.hideOnMobile(), current.hideOnTablet(), hide));
    }
}
