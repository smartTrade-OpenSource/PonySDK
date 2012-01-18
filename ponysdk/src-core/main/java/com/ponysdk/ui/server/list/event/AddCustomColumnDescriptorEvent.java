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
package com.ponysdk.ui.server.list.event;

import com.ponysdk.core.event.SystemEvent;
import com.ponysdk.ui.server.list.ColumnDescriptorFieldHolder;

public class AddCustomColumnDescriptorEvent extends
		SystemEvent<AddCustomColumnDescriptorHandler> {


	public static final Type<AddCustomColumnDescriptorHandler> TYPE = new Type<AddCustomColumnDescriptorHandler>();
	private final ColumnDescriptorFieldHolder descriptorHolder;

	public AddCustomColumnDescriptorEvent(Object sourceComponent,
			ColumnDescriptorFieldHolder descriptorHolder) {
        super(sourceComponent);
		this.descriptorHolder = descriptorHolder;
    }

    @Override
	protected void dispatch(AddCustomColumnDescriptorHandler handler) {
        handler.onAddCustomColumnDescriptor(this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
	public Type<AddCustomColumnDescriptorHandler> getAssociatedType() {
        return (Type) TYPE;
    }

	public ColumnDescriptorFieldHolder getDescriptorHolder() {
		return descriptorHolder;
	}

}
