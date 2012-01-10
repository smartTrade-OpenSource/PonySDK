
package com.ponysdk.core.export;

import java.util.List;

public interface Exporter<T> {

    public String name();

    /**
     * @return custom success message
     * @throws Exception
     */
    public String export(List<ExportableField> exportableFields, List<T> records) throws Exception;

}
