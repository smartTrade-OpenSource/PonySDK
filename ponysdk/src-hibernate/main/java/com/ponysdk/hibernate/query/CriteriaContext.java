
package com.ponysdk.hibernate.query;

import com.ponysdk.core.query.Criterion;

public class CriteriaContext {

    private Criterion criterion;

    private OrderingCriteria STCriteria;

    public OrderingCriteria getSTCriteria() {
        return STCriteria;
    }

    public void setSTCriteria(OrderingCriteria STCriteria) {
        this.STCriteria = STCriteria;
    }

    public Criterion getCriterion() {
        return criterion;
    }

    public void setCriterion(Criterion criterion) {
        this.criterion = criterion;
    }

}
