package com.ponysdk.ui.server.list;

public class ColumnDescriptorFieldHolder {
	private final String caption;
	private final String fieldPath;
	private final Class<?> fieldType;
	private final String tableName;

	public ColumnDescriptorFieldHolder(String caption, String fieldPath,
			Class<?> fieldType, String tableName) {
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
