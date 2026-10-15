
package com.ponysdk.core.server.servlet;

import com.ponysdk.core.server.application.Application;

public interface ApplicationListener {

    void onApplicationCreated(Application application);

    void onApplicationDestroyed(Application application);

    default void onApplicationDestroyed(Application application, int statusCode) {
        onApplicationDestroyed(application);
    }
}
