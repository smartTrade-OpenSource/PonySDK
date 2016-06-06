
package com.ponysdk.impl.webapplication.notification.renderer;

import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PWidget;

public class PHTMLRenderer implements NotificationTypeRenderer<String> {

    @Override
    public PWidget getWidget(final String source) {
        return new PHTML(source);
    }
}