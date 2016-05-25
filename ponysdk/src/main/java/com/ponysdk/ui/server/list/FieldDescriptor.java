
package com.ponysdk.ui.server.list;

public class FieldDescriptor {

    private String ID;
    private String key;
    private Class<?> type;
    private String listCaption;
    private String criteriaKey;
    private String exportCaption;

    public FieldDescriptor(final String iD, final String key, final Class<?> type, final String listCaption, final String criteriaKey, final String exportCaption) {
        this.ID = iD;
        this.key = key;
        this.type = type;
        this.listCaption = listCaption;
        this.criteriaKey = criteriaKey;
        this.exportCaption = exportCaption;
    }

    public String getID() {
        return ID;
    }

    public void setID(final String iD) {
        ID = iD;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(final Class<?> type) {
        this.type = type;
    }

    public String getListCaption() {
        return listCaption;
    }

    public void setListCaption(final String listCaption) {
        this.listCaption = listCaption;
    }

    public String getCriteriaKey() {
        return criteriaKey;
    }

    public void setCriteriaKey(final String criteriaKey) {
        this.criteriaKey = criteriaKey;
    }

    public String getExportCaption() {
        return exportCaption;
    }

    public void setExportCaption(final String exportCaption) {
        this.exportCaption = exportCaption;
    }

}
