
package com.ponysdk.impl.webapplication.notification.renderer;

import com.ponysdk.core.ui.basic.PWidget;

public interface NotificationTypeRenderer<T> {

    PWidget getWidget(T source);

}
