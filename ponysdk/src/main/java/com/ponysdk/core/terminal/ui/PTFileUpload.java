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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTFileUpload extends PTWidget<FormPanel> {

	private static Frame frame;

	private final FileUpload fileUpload = new FileUpload();

	private static Frame getFrame() {
		/* Frame for stream resource handling */
		if (frame == null) {
			frame = new Frame();
			frame.setWidth("0px");
			frame.setHeight("0px");
			frame.getElement().getStyle().setProperty("visibility", "hidden");
			frame.getElement().getStyle().setProperty("position", "fixed");
		}
		return frame;
	}

	@Override
	public void create(final ReaderBuffer buffer, final int objectId,
			final UIBuilder uiService) {
		super.create(buffer, objectId, uiService);

		uiObject.setEncoding(FormPanel.ENCODING_MULTIPART);
		uiObject.setMethod(FormPanel.METHOD_POST);
		final VerticalPanel panel = new VerticalPanel();
		panel.setSize("100%", "100%");
		uiObject.setWidget(panel);
		panel.add(fileUpload);

		uiObject.addSubmitCompleteHandler(new SubmitCompleteHandler() {

			@Override
			public void onSubmitComplete(final SubmitCompleteEvent event) {
				final PTInstruction instruction = new PTInstruction(objectId);
				instruction.put(ClientToServerModel.HANDLER_SUBMIT_COMPLETE);
				uiService.sendDataToServer(uiObject, instruction);
			}
		});
	}

	@Override
	protected FormPanel createUIObject() {
		return new FormPanel();
	}

	@Override
	public void addHandler(final ReaderBuffer buffer,
			final HandlerModel handlerModel, final UIBuilder uiService) {
		if (HandlerModel.HANDLER_CHANGE.equals(handlerModel)) {
			fileUpload.addChangeHandler(new ChangeHandler() {

				@Override
				public void onChange(final ChangeEvent event) {
					final PTInstruction eventInstruction = new PTInstruction(
							getObjectID());
					eventInstruction.put(ClientToServerModel.HANDLER_CHANGE,
							fileUpload.getFilename());
					uiService.sendDataToServer(fileUpload, eventInstruction);
				}
			});
		} else if (HandlerModel.HANDLER_STREAM_REQUEST.equals(handlerModel)) {
			final String action = GWT.getHostPageBaseURL() + "stream?"
					+ ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "="
					+ PonySDK.uiContextId + "&"
					+ ClientToServerModel.STREAM_REQUEST_ID.toStringValue()
					+ "=" + buffer.readBinaryModel().getIntValue();
			getFrame().setUrl(action);
		} else if (HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST
				.equals(handlerModel)) {
			final String action = GWT.getHostPageBaseURL() + "stream?"
					+ ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "="
					+ PonySDK.uiContextId + "&"
					+ ClientToServerModel.STREAM_REQUEST_ID.toStringValue()
					+ "=" + buffer.readBinaryModel().getIntValue();
			uiObject.setAction(action);
			uiObject.submit();
		} else {
			super.addHandler(buffer, handlerModel, uiService);
		}
	}

	@Override
	public boolean update(final ReaderBuffer buffer,
			final BinaryModel binaryModel) {
		if (ServerToClientModel.NAME.equals(binaryModel.getModel())) {
			fileUpload.setName(binaryModel.getStringValue());
			return true;
		}
		if (ServerToClientModel.ENABLED.equals(binaryModel.getModel())) {
			fileUpload.setEnabled(binaryModel.getBooleanValue());
			return true;
		}
		return super.update(buffer, binaryModel);
	}

}
