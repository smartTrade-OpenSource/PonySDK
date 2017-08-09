
package com.ponysdk.core.listener;

public interface ProcessListener {

    void process(byte[] payload, int offset, int len);

    void process(String text);

    void onClose();

}