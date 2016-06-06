
package com.ponysdk.core.ui.form.dataconverter;

/**
 * Used to convert from Business to UI and from UI to Business
 * 
 * @param <T1>
 * @param <T2>
 */
public interface DataConverter<T1, T2> {

    T1 from(T2 t);

    T2 to(T1 t);

}
