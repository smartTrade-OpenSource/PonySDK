
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;
import com.ponysdk.core.ui.form.dataconverter.IdentityConverter;

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
