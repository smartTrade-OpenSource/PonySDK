
package com.ponysdk.core.server.service.query;

public class test {

    public static void main(final String[] args) {
        final Query query = new Query();
        query.addCriterion(Restriction.OR(new Criterion("A"), Restriction.OR(new Criterion("B"), new Criterion("C"))));

    }

}
