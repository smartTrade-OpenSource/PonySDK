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

package com.ponysdk.sample.client.playground;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * Main layout orchestrator for the playground interface.
 * <p>
 * Creates a horizontal split layout with:
 * <ul>
 *   <li>Left panel (25% width): Component selection list</li>
 *   <li>Right panel (75% width): Property panel with preview and controls</li>
 * </ul>
 * </p>
 * <p>
 * Requirements: 2.4, 2.5, 8.1
 * </p>
 */
public class PlaygroundLayout extends PHorizontalPanel {

    private static final String WIDTH_LEFT = "25%";
    private static final String WIDTH_RIGHT = "75%";

    private final PWidget leftPanel;
    private final PWidget rightPanel;

    /**
     * Creates a new PlaygroundLayout with the specified left and right panels.
     *
     * @param leftPanel  the component list panel (25% width), must not be null
     * @param rightPanel the property panel (75% width), must not be null
     * @throws IllegalArgumentException if either panel is null
     */
    public PlaygroundLayout(final PWidget leftPanel, final PWidget rightPanel) {
        if (leftPanel == null) {
            throw new IllegalArgumentException("leftPanel must not be null");
        }
        if (rightPanel == null) {
            throw new IllegalArgumentException("rightPanel must not be null");
        }

        this.leftPanel = leftPanel;
        this.rightPanel = rightPanel;

        initializeLayout();
    }

    /**
     * Initializes the layout structure with proper sizing.
     */
    private void initializeLayout() {
        // Set up the horizontal panel
        setWidth("100%");
        setHeight("100%");
        addStyleName("playground-layout");

        // Create header
        final PVerticalPanel mainContainer = Element.newPVerticalPanel();
        mainContainer.setWidth("100%");
        mainContainer.setHeight("100%");
        
        final PHorizontalPanel header = Element.newPHorizontalPanel();
        header.setWidth("100%");
        header.addStyleName("playground-header");
        
        final PLabel title = Element.newPLabel("🎨 Web Awesome Component Playground");
        title.addStyleName("playground-title");
        
        final PLabel subtitle = Element.newPLabel("Interactive testing environment for PonySDK components");
        subtitle.addStyleName("playground-subtitle");
        
        final PVerticalPanel headerContent = Element.newPVerticalPanel();
        headerContent.add(title);
        headerContent.add(subtitle);
        
        header.add(headerContent);
        
        // Create content area
        final PHorizontalPanel contentArea = Element.newPHorizontalPanel();
        contentArea.setWidth("100%");
        contentArea.setHeight("100%");
        contentArea.addStyleName("playground-content");

        // Add left panel with 25% width
        contentArea.add(leftPanel);
        contentArea.setCellWidth(leftPanel, WIDTH_LEFT);

        // Add right panel with 75% width
        contentArea.add(rightPanel);
        contentArea.setCellWidth(rightPanel, WIDTH_RIGHT);
        
        // Assemble layout
        mainContainer.add(header);
        mainContainer.add(contentArea);
        mainContainer.setCellHeight(contentArea, "100%");
        
        // Add to this panel
        add(mainContainer);
    }

    /**
     * Gets the left panel (component list).
     *
     * @return the left panel widget
     */
    public PWidget getLeftPanel() {
        return leftPanel;
    }

    /**
     * Gets the right panel (property panel).
     *
     * @return the right panel widget
     */
    public PWidget getRightPanel() {
        return rightPanel;
    }
}
