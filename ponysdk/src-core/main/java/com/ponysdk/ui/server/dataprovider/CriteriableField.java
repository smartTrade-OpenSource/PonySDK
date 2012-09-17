
package com.ponysdk.ui.server.dataprovider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ponysdk.core.query.Criterion;
import com.ponysdk.ui.server.form2.formfield.FormField;

public class CriteriableField implements Criteriable {

    private final String key;
    private final FormField<?> formField;

    public CriteriableField(final FormField<?> formField, final String key) {
        this.key = key;
        this.formField = formField;
    }

    @Override
    public List<Criterion> getCriteria() {
        if (formField.getValue() == null) return Collections.emptyList();

        final Criterion criterion = new Criterion(key);
        criterion.setValue(formField.getValue());
        return Arrays.asList(criterion);
    }

    @Override
    public void setCriteria(final List<Criterion> criteria) {

    }

}
