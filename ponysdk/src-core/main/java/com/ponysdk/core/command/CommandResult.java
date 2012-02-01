
package com.ponysdk.core.command;

public class CommandResult<T> {

    private T result;
    private Throwable throwable;

    public CommandResult(final T result) {
        super();
        this.result = result;
    }

    public CommandResult(final Throwable throwable) {
        super();
        this.throwable = throwable;
    }

    public T getResult() {
        return result;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean isSuccessfull() {
        return throwable != null;
    }

}
