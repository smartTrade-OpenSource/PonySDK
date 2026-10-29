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

import java.time.DateTimeException;
import java.util.function.Function;

/**
 * Between what a field holds (its presentation, a String for a text box) and what the bean holds (its model). The
 * way to the model can fail, with the message to show; the way back cannot.
 */
public interface Converter<P, M> {

    Result<M> toModel(P presentation);

    P toPresentation(M model);

    /**
     * A converter from two functions. What {@code toModel} throws as an {@link IllegalArgumentException} (a
     * {@code NumberFormatException} is one) or a {@link DateTimeException} is a conversion error, with {@code error}
     * as its message.
     */
    static <P, M> Converter<P, M> of(final Function<P, M> toModel, final Function<M, P> toPresentation, final String error) {
        return new Converter<>() {

            @Override
            public Result<M> toModel(final P presentation) {
                try {
                    return Result.ok(toModel.apply(presentation));
                } catch (final IllegalArgumentException | DateTimeException e) {
                    return Result.error(error);
                }
            }

            @Override
            public P toPresentation(final M model) {
                return toPresentation.apply(model);
            }
        };
    }

}
