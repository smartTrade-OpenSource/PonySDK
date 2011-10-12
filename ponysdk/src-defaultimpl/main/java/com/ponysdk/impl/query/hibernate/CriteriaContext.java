package com.ponysdk.impl.query.hibernate;

import com.ponysdk.core.query.CriterionField;

public class CriteriaContext {

    private CriterionField criterion;
    private OrderingCriteria STCriteria;

    public OrderingCriteria getSTCriteria() {
        return STCriteria;
    }

    public void setSTCriteria(OrderingCriteria STCriteria) {
        this.STCriteria = STCriteria;
    }

    public CriterionField getCriterion() {
        return criterion;
    }

    public void setCriterion(CriterionField criterion) {
        this.criterion = criterion;
    }

}
