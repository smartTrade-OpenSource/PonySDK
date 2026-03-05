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
 * Container layout component for constraining content width.
 * <p>
 * Provides a centered, max-width-constrained wrapper for page content,
 * similar to Bootstrap's {@code .container} class.
 * </p>
 */
public class PContainer extends PWebComponent<ContainerProps> {

    public PContainer() {
        super(ContainerProps.defaults());
    }

    public PContainer(final ContainerProps initialProps) {
        super(initialProps);
    }

    @Override
    protected Class<ContainerProps> getPropsClass() {
        return ContainerProps.class;
    }

    @Override
    protected String getComponentSignature() {
        return "wa-container";
    }

    public void setMaxWidth(final String maxWidth) {
        final ContainerProps current = getCurrentProps();
        setProps(new ContainerProps(maxWidth, current.padding(), current.centered(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setPadding(final String padding) {
        final ContainerProps current = getCurrentProps();
        setProps(new ContainerProps(current.maxWidth(), padding, current.centered(),
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setCentered(final boolean centered) {
        final ContainerProps current = getCurrentProps();
        setProps(new ContainerProps(current.maxWidth(), current.padding(), centered,
            current.hideOnMobile(), current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setHideOnMobile(final boolean hide) {
        final ContainerProps current = getCurrentProps();
        setProps(new ContainerProps(current.maxWidth(), current.padding(), current.centered(),
            hide, current.hideOnTablet(), current.hideOnDesktop()));
    }

    public void setHideOnTablet(final boolean hide) {
        final ContainerProps current = getCurrentProps();
        setProps(new ContainerProps(current.maxWidth(), current.padding(), current.centered(),
            current.hideOnMobile(), hide, current.hideOnDesktop()));
    }

    public void setHideOnDesktop(final boolean hide) {
        final ContainerProps current = getCurrentProps();
        setProps(new ContainerProps(current.maxWidth(), current.padding(), current.centered(),
            current.hideOnMobile(), current.hideOnTablet(), hide));
    }
}
