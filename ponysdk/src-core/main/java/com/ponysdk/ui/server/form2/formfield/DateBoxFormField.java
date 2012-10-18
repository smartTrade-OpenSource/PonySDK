
package com.ponysdk.ui.server.form2.formfield;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.form2.dataconverter.DataConverter;
import com.ponysdk.ui.server.form2.dataconverter.DateConverter;

public class DateBoxFormField extends FormField<Date> {

    private final PDateBox dateBox;

    public DateBoxFormField() {
        this(new PDateBox(), new DateConverter());
    }

    public DateBoxFormField(final String dateFormat) {
        this(new PDateBox(new SimpleDateFormat(dateFormat)), new DateConverter(new SimpleDateFormat(dateFormat)));
    }

    public DateBoxFormField(final SimpleDateFormat dateFormat) {
        this(new PDateBox(dateFormat), new DateConverter(dateFormat));
    }

    public DateBoxFormField(final PDateBox dateBox) {
        this(dateBox, new DateConverter(dateBox.getDateFormat()));
    }

    public DateBoxFormField(final PDateBox dateBox, final DataConverter<String, Date> dataConverter) {
        super(dataConverter);
        this.dateBox = dateBox;
    }

    @Override
    public PWidget asWidget() {
        return dateBox;
    }

    @Override
    public void reset0() {
        dateBox.setValue(null);
    }

    @Override
    public Date getValue() {
        return dateBox.getValue();
    }

    @Override
    public void setValue(final Date value) {
        dateBox.setValue(value);
    }

    @Override
    protected String getStringValue() {
        return dataProvider.from(getValue());
    }

    public PDateBox getDateBox() {
        return dateBox;
    }

}
