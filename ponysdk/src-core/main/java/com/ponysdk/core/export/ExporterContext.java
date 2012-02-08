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

package com.ponysdk.core.export;

import java.util.List;

import com.ponysdk.core.datamodel.ExportableData;

public class ExporterContext<T> {

    private ExportableData<List<T>> exportableDTO;

    private String requestKey;

    private String timezone;

    private String dateFormat;

    private Object object;

    public ExporterContext(ExportableData<List<T>> exportableDTO) {
        setExportableDTO(exportableDTO);
    }

    public ExporterContext(String requestKey, ExportableData<List<T>> exportableDTO) {
        this(requestKey, exportableDTO, null);
    }

    public ExporterContext(String requestKey, ExportableData<List<T>> exportableDTO, Object object) {
        setExportableDTO(exportableDTO);
        setRequestKey(requestKey);
        setObject(object);
    }

    public ExportableData<List<T>> getExportableDTO() {
        return exportableDTO;
    }

    public void setExportableDTO(ExportableData<List<T>> exportableDTO) {
        this.exportableDTO = exportableDTO;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timeZone) {
        this.timezone = timeZone;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
