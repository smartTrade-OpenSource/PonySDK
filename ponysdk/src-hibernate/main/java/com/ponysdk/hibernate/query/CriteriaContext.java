
package com.ponysdk.hibernate.query;

import com.ponysdk.core.query.Criterion;

public class CriteriaContext {

    private Criterion criterion;

    private OrderingCriteria orderingCriteria;

    public Criterion getCriterion() {
        return criterion;
    }

    public void setCriterion(final Criterion criterion) {
        this.criterion = criterion;
    }

    public OrderingCriteria getOrderingCriteria() {
        return orderingCriteria;
    }

    public void setOrderingCriteria(final OrderingCriteria orderingCriteria) {
        this.orderingCriteria = orderingCriteria;
    }

}
