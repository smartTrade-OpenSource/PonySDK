
package com.ponysdk.core.export;

import java.util.List;

public interface Exporter<T> {

    String name();

    /**
     * @return custom success message
     * @throws Exception
     */
    String export(List<ExportableField> exportableFields, List<T> records) throws Exception;

}
