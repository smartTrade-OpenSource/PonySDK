package com.ponysdk.ui.terminal.instruction;

public interface InstructionCallBack<T> {
    void onSuccess(T t);

    void onFailure(Throwable t);
}