
package com.ponysdk.ui.server.form2.dataconverter;

public interface DataConverter<T1, T2> {

    public T1 from(T2 t);

    public T2 to(T1 t);

}
