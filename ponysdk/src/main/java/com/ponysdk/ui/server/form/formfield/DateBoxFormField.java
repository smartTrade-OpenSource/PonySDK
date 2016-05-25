
package com.ponysdk.ui.server.form.formfield;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.form.dataconverter.DataConverter;
import com.ponysdk.ui.server.form.dataconverter.DateConverter;

public class DateBoxFormField extends FormField<Date, PDateBox> {

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

    public DateBoxFormField(final PDateBox widget, final DataConverter<String, Date> dataConverter) {
        super(widget, dataConverter);
    }

    @Override
    public void reset0() {
        widget.setValue(null);
    }

    @Override
    public Date getValue() {
        return widget.getValue();
    }

    @Override
    public void setValue(final Date value) {
        widget.setValue(value);
    }

    @Override
    protected String getStringValue() {
        return dataProvider.from(getValue());
    }

    @Override
    public void setEnabled(final boolean enabled) {
        widget.setEnabled(enabled);
    }

}
