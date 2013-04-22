
package com.ponysdk.core.stm;

public interface TxnObject<T> {

    void set(T object);

    T get();

    void commit();

    void rollback();

    void reset(T initialValue);

}
