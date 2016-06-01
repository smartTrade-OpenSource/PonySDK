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

package com.ponysdk.sample.client.page;

import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PNotificationManager.Notification;
import com.ponysdk.ui.server.basic.PVerticalPanel;

public class MenuBarPageActivity extends SamplePageActivity {

	public MenuBarPageActivity() {
		super("MenuBar", "Lists and Menus");
	}

	@Override
	protected void onFirstShowPage() {
		super.onFirstShowPage();

		final PVerticalPanel panel = new PVerticalPanel();

		final PMenuBar menuBar1 = createMenuBar(false);
		final PMenuBar menuBar2 = createMenuBar(true);
		final PMenuBar menuBar3 = createMenuBar(true);
		final PMenuBar menuBar4 = createMenuBar(true);

		panel.add(new PLabel("Horizontal Menu Bar [Default Style]"));
		panel.add(menuBar1);
		panel.add(new PLabel("Vertical Menu Bar [Default Style]"));
		panel.add(menuBar2);
		panel.add(new PLabel("Vertical Menu Bar [Light Style]"));
		panel.add(menuBar3);
		panel.add(new PLabel("Vertical Menu Bar [Toolbar Style]"));
		panel.add(menuBar4);

		menuBar2.setStylePrimaryName("samplePopup");
		// menuBar3.addStyleName(PonySDKTheme.MENUBAR_LIGHT);
		menuBar4.setStyleName("pony-ActionToolbar");

		examplePanel.setWidget(panel);
	}

	private PMenuBar createMenuBar(final boolean vertical) {
		final PMenuBar menuBar = new PMenuBar();
		final PMenuBar fileBar = new PMenuBar(vertical);

		menuBar.addItem("File", fileBar);

		final PMenuItem newItem = new PMenuItem("New");
		newItem.setCommand(
				() -> PNotificationManager.notify("Menu Selection, " + newItem.getText(), Notification.HUMANIZED));
		final PMenuItem openItem = new PMenuItem("Open");
		openItem.setCommand(
				() -> PNotificationManager.notify("Menu Selection, " + openItem.getText(), Notification.HUMANIZED));

		final PMenuItem closeItem = new PMenuItem("Close");
		openItem.setCommand(
				() -> PNotificationManager.notify("Menu Selection, " + closeItem.getText(), Notification.HUMANIZED));

		fileBar.addItem(newItem);
		fileBar.addItem(openItem);
		fileBar.addItem(closeItem);
		fileBar.addSeparator();

		final PMenuBar recentItem = new PMenuBar(vertical);

		fileBar.addItem("Recent", recentItem);

		final PMenuItem recent1 = new PMenuItem("recent1");
		recent1.setCommand(
				() -> PNotificationManager.notify("Menu Selection, " + recent1.getText(), Notification.HUMANIZED));
		final PMenuItem recent2 = new PMenuItem("recent2");
		recent2.setCommand(
				() -> PNotificationManager.notify("Menu Selection, " + recent2.getText(), Notification.HUMANIZED));

		recentItem.addItem(recent1);
		recentItem.addSeparator();
		recentItem.addItem(recent2);

		return menuBar;
	}
}
