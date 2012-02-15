
package com.ponysdk.ui.server.form2.dataconverter;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateConverter implements DataConverter<String, Date> {

    private static final Logger log = LoggerFactory.getLogger(DateConverter.class);

    private final DateFormat dateFormat;

    public DateConverter() {
        this(DateFormat.getDateInstance());
    }

    public DateConverter(final DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public String from(final Date t) {
        if (t == null) return null;
        return dateFormat.format(t);
    }

    @Override
    public Date to(final String t) {
        try {
            return dateFormat.parse(t);
        } catch (final ParseException e) {
            log.error("cannot parse date", e);
        }
        return null;
    }

}
