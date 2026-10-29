/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.binder;

/**
 * The outcome of a conversion or a validation: a value, or the message to show on the field.
 */
public final class Result<R> {

    private static final Result<?> OK_NULL = new Result<>(null, null);

    private final R value;
    private final String message;

    private Result(final R value, final String message) {
        this.value = value;
        this.message = message;
    }

    @SuppressWarnings("unchecked")
    public static <R> Result<R> ok(final R value) {
        return value == null ? (Result<R>) OK_NULL : new Result<>(value, null);
    }

    public static <R> Result<R> error(final String message) {
        return new Result<>(null, message);
    }

    public boolean isError() {
        return message != null;
    }

    /** The message of an error, null for a value. */
    public String getMessage() {
        return message;
    }

    /** The value, null for an error. */
    public R getValue() {
        return value;
    }

}
