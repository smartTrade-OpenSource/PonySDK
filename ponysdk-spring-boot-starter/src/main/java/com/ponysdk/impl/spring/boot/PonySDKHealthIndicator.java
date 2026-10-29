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

package com.ponysdk.impl.spring.boot;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import com.ponysdk.core.server.application.Drain;

/**
 * The actuator's view of what {@code /<application>/health} answers to a load balancer: UP, then OUT_OF_SERVICE (HTTP 503)
 * while the live pages move to the other servers (ADR-0017).
 */
final class PonySDKHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(final Health.Builder builder) {
        if (Drain.isDraining()) builder.status(Status.OUT_OF_SERVICE).withDetail("draining", true);
        else builder.up();
    }
}
