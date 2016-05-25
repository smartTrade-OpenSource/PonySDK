
package com.ponysdk.impl.webapplication.notification.renderer;

import com.ponysdk.ui.server.basic.PWidget;

public interface NotificationTypeRenderer<T> {

    PWidget getWidget(T source);

}
