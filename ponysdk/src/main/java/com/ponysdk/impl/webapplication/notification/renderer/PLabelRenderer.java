
package com.ponysdk.impl.webapplication.notification.renderer;

import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;

public class PLabelRenderer implements NotificationTypeRenderer<String> {

    @Override
    public PWidget getWidget(final String source) {
        return new PLabel(source);
    }
}
