package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.PTextBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateInputFormField extends AbstractInputFormField<Date> {
    private static final Logger log = LoggerFactory.getLogger(DateInputFormField.class);
    private final DateFormat dateFormat;

    public DateInputFormField(String caption) {
        this(caption, new SimpleDateFormat("yyyy-MM-dd"));
    }

    public DateInputFormField(String caption, DateFormat format) {
        super(caption);
        dateFormat = format;
    }

    @Override
    protected PTextBox createInnerWidget() {
        PTextBox input = super.createInnerWidget();
        input.setAttribute("type", "date");
        return input;
    }

    @Override
    public Date getValue() {
        try {
            return dateFormat.parse(input.getText());
        } catch (ParseException e) {
            log.error("Cannot parse input : {}", input.getText(), e);
        }
        return null;
    }

    @Override
    public void setValue(Date date) {
        input.setText(dateFormat.format(date));
    }

}
