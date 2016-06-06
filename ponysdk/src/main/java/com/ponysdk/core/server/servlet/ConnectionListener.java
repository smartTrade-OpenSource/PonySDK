
package com.ponysdk.core.server.servlet;

public interface ConnectionListener {

    void onOpen();

    void onClose();
}
