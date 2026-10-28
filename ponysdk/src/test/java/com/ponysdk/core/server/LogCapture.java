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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * The events a logger writes for the time of a test, as "LEVEL message", removed on close.
 */
public final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final AbstractAppender appender;
    private final List<String> events = new CopyOnWriteArrayList<>();

    private LogCapture(final Class<?> type) {
        logger = (Logger) LogManager.getLogger(type);
        appender = new AbstractAppender("capture-" + type.getSimpleName(), null, null, true, Property.EMPTY_ARRAY) {

            @Override
            public void append(final LogEvent event) {
                events.add(event.getLevel() + " " + event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
    }

    public static LogCapture of(final Class<?> type) {
        return new LogCapture(type);
    }

    public List<String> events() {
        return events;
    }

    @Override
    public void close() {
        logger.removeAppender(appender);
        appender.stop();
    }
}
