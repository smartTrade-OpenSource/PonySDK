/**
 *
 */

package com.ponysdk.core.server.service.query;

import java.util.List;

public interface PResultSet<D> extends AutoCloseable {

    /**
     *
     * @return true if data has been fetched
     */
    boolean next();

    /**
     * This method must be called only if {@link #next()} returned true.
     *
     * @return fetched data
     */
    List<D> getData();

    @Override
    void close();

    default PResultSet<D> withLimit(final int limit) {
        return new ResultSetWithLimit<>(this, limit);
    }

    static <D> PResultSet<D> of(final List<D> data) {
        return new ResultSetListImpl<>(data);
    }

    class ResultSetListImpl<D> implements PResultSet<D> {

        private boolean next = true;
        private final List<D> data;

        public ResultSetListImpl(final List<D> data) {
            this.data = data;
        }

        @Override
        public boolean next() {
            if (next) {
                next = false;
                return true;
            }
            return false;
        }

        @Override
        public List<D> getData() {
            return data;
        }

        @Override
        public void close() {
            //nothing to close
        }

    }

    class ResultSetWithLimit<D> implements PResultSet<D> {

        private final PResultSet<D> delegate;
        private int beginPosition = 0;
        private int lastSize = 0;

        private final int limit;

        public ResultSetWithLimit(final PResultSet<D> delegate, final int limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public boolean next() {
            if (beginPosition + lastSize >= limit) return false;
            final boolean next = delegate.next();
            if (next) {
                beginPosition += lastSize;
                lastSize = delegate.getData().size();
            }
            return next;
        }

        @Override
        public List<D> getData() {
            if (beginPosition + lastSize >= limit) return delegate.getData().subList(0, limit - beginPosition);
            return delegate.getData();
        }

        @Override
        public void close() {
            delegate.close();
        }

    }

}
