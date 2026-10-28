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

package com.ponysdk.core.server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * The level of a logger for the time of a test, restored on close.
 */
public final class LogLevel implements AutoCloseable {

    private final String logger;
    private final Level previous;

    private LogLevel(final String logger, final Level previous) {
        this.logger = logger;
        this.previous = previous;
    }

    /** @param level a Log4j level name: OFF, ERROR, WARN, INFO, DEBUG, TRACE */
    public static LogLevel set(final Class<?> logger, final String level) {
        return set(logger.getName(), level);
    }

    public static LogLevel set(final String logger, final String level) {
        final LogLevel set = new LogLevel(logger, LogManager.getLogger(logger).getLevel());
        Configurator.setLevel(logger, Level.toLevel(level));
        return set;
    }

    @Override
    public void close() {
        Configurator.setLevel(logger, previous);
    }
}
