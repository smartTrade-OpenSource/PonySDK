
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.form.dataconverter.DataConverter;
import com.ponysdk.ui.server.form.dataconverter.IdentityConverter;

public class StringListBoxFormField extends ListBoxFormField<String> {

    public StringListBoxFormField() {
        this(new PListBox(), IdentityConverter.STRING);
    }

    public StringListBoxFormField(final PListBox listBox) {
        this(listBox, IdentityConverter.STRING);
    }

    public StringListBoxFormField(final PListBox listBox, final DataConverter<String, String> dataProvider) {
        super(listBox, dataProvider);
    }

}
