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

package com.ponysdk.core.server.service.query;

import java.io.Serializable;
import java.time.Duration;

public class Result<T> implements Serializable {

    private static final long serialVersionUID = -8598967363564331854L;

    private T data;

    private int fullSize;

    private Duration executionDuration;

    public Result() {
        this(null);
    }

    public Result(final T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(final T data) {
        this.data = data;
    }

    public int getFullSize() {
        return fullSize;
    }

    public void setFullSize(final int fullSize) {
        this.fullSize = fullSize;
    }

    public Duration getExecutionDuration() {
        return executionDuration;
    }

    public void setExecutionDuration(final Duration executionTime) {
        this.executionDuration = executionTime;
    }

}
