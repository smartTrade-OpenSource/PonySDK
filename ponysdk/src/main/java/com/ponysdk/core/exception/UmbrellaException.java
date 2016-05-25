
package com.ponysdk.core.exception;

import java.util.Iterator;
import java.util.Set;

public class UmbrellaException extends RuntimeException {

    private static final long serialVersionUID = -647912793032725942L;

    private static final String MULTIPLE = " exceptions caught: ";
    private static final String ONE = "Exception caught: ";

    private final Set<Throwable> causes;

    protected static Throwable makeCause(final Set<Throwable> causes) {
        final Iterator<Throwable> iterator = causes.iterator();
        if (!iterator.hasNext()) { return null; }

        return iterator.next();
    }

    protected static String makeMessage(final Set<Throwable> causes) {
        final int count = causes.size();
        if (count == 0) { return null; }

        final StringBuilder b = new StringBuilder(count == 1 ? ONE : count + MULTIPLE);
        boolean first = true;
        for (final Throwable t : causes) {
            if (first) {
                first = false;
            } else {
                b.append("; ");
            }
            b.append(t.getMessage());
        }

        return b.toString();
    }

    public UmbrellaException(final Set<Throwable> causes) {
        super(makeMessage(causes), makeCause(causes));
        this.causes = causes;
    }

    public Set<Throwable> getCauses() {
        return causes;
    }
}
