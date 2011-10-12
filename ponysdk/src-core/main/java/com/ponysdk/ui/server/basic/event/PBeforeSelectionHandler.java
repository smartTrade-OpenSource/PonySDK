package com.ponysdk.ui.server.basic.event;

public interface PBeforeSelectionHandler<I> {

    void onBeforeSelection(I index);
}