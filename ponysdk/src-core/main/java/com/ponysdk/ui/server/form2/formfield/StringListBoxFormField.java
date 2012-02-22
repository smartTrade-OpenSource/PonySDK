
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.form2.dataconverter.DataConverter;
import com.ponysdk.ui.server.form2.dataconverter.IdentityConverter;

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
