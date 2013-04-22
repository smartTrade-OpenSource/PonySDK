
package com.ponysdk.ui.server.list;

@Deprecated
public class ColumnDescriptorFieldHolder {

    private final String caption;

    private final String fieldPath;

    private final Class<?> fieldType;

    private final String tableName;

    public ColumnDescriptorFieldHolder(final String caption, final String fieldPath, final Class<?> fieldType, final String tableName) {
        super();
        this.caption = caption;
        this.fieldPath = fieldPath;
        this.fieldType = fieldType;
        this.tableName = tableName;
    }

    public String getCaption() {
        return caption;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public String getTableName() {
        return tableName;
    }
}
