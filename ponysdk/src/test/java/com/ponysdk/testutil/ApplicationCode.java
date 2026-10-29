/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.testutil;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;

/** Code outside the framework's packages: where the debug mode says an application created a widget. */
public final class ApplicationCode {

    private ApplicationCode() {
    }

    public static PLabel newLabel() {
        return Element.newPLabel();
    }
}
