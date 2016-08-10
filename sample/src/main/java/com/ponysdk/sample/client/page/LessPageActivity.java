/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.sample.client.page;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PScript.ExecutionCallback;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.core.ui.basic.event.PChangeHandler;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form.Form;
import com.ponysdk.core.ui.form.FormFieldComponent;
import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.form.validator.FieldValidator;
import com.ponysdk.core.ui.form.validator.ValidationResult;

public class LessPageActivity extends SamplePageActivity {

    private static final Logger log = LoggerFactory.getLogger(LessPageActivity.class);

    private static final String STANDARD = "Standard";
    private static final String DARK = "Dark";

    private final ColorValidator colorValidator = new ColorValidator();

    private ColorFormField header;
    private ColorFormField headerTextColor;
    private ColorFormField footer;
    private ColorFormField footerTextColor;
    private ColorFormField highlight;

    private ColorFormField black;
    private ColorFormField grayDarker;
    private ColorFormField grayDark;
    private ColorFormField gray;
    private ColorFormField grayLight;
    private ColorFormField grayLighter;
    private ColorFormField white;

    public LessPageActivity() {
        super("Less", "Extra");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        PScript.execute(PWindow.getMain(), "window.colors = {};");

        final PFlowPanel layout = new PFlowPanel();
        layout.add(new PLabel(
                "Pony SDK styling use Less CSS. It's really easy to customize your styling with the use of a few variable."));
        layout.add(new PLabel("Customize the sample by changing this variables :"));

        final Form form = new Form();

        final PSimplePanel headerPreview = new PSimplePanel();
        final PSimplePanel headerTextColorPreview = new PSimplePanel();
        final PSimplePanel footerPreview = new PSimplePanel();
        final PSimplePanel footerTextColorPreview = new PSimplePanel();
        final PSimplePanel highlightPreview = new PSimplePanel();

        final PSimplePanel blackPreview = new PSimplePanel();
        final PSimplePanel grayDarkerPreview = new PSimplePanel();
        final PSimplePanel grayDarkPreview = new PSimplePanel();
        final PSimplePanel grayPreview = new PSimplePanel();
        final PSimplePanel grayLightPreview = new PSimplePanel();
        final PSimplePanel grayLighterPreview = new PSimplePanel();
        final PSimplePanel whitePreview = new PSimplePanel();

        header = buildColorFormField("498BF4", headerPreview);
        headerTextColor = buildColorFormField("EDEDED", headerTextColorPreview);
        footer = buildColorFormField("498BF4", footerPreview);
        footerTextColor = buildColorFormField("EDEDED", footerTextColorPreview);
        highlight = buildColorFormField("498BF4", highlightPreview);

        black = buildColorFormField("000000", blackPreview);
        grayDarker = buildColorFormField("222222", grayDarkerPreview);
        grayDark = buildColorFormField("333333", grayDarkPreview);
        gray = buildColorFormField("555555", grayPreview);
        grayLight = buildColorFormField("999999", grayLightPreview);
        grayLighter = buildColorFormField("eeeeee", grayLighterPreview);
        white = buildColorFormField("ffffff", whitePreview);

        form.addFormField(header.formField);
        form.addFormField(headerTextColor.formField);
        form.addFormField(footer.formField);
        form.addFormField(footerTextColor.formField);
        form.addFormField(highlight.formField);

        form.addFormField(black.formField);
        form.addFormField(grayDarker.formField);
        form.addFormField(grayDark.formField);
        form.addFormField(gray.formField);
        form.addFormField(grayLight.formField);
        form.addFormField(grayLighter.formField);
        form.addFormField(white.formField);

        final PFlexTable formLayout = new PFlexTable();
        formLayout.setWidget(0, 0, new FormFieldComponent("header", header.formField));
        formLayout.setWidget(1, 0, new FormFieldComponent("headerTextColor", headerTextColor.formField));
        formLayout.setWidget(2, 0, new FormFieldComponent("footer", footer.formField));
        formLayout.setWidget(3, 0, new FormFieldComponent("footerTextColor", footerTextColor.formField));
        formLayout.setWidget(5, 0, new FormFieldComponent("highlight", highlight.formField));

        formLayout.setWidget(0, 1, headerPreview);
        formLayout.setWidget(1, 1, headerTextColorPreview);
        formLayout.setWidget(2, 1, footerPreview);
        formLayout.setWidget(3, 1, footerTextColorPreview);
        formLayout.setWidget(5, 1, highlightPreview);

        formLayout.setWidget(0, 2, new FormFieldComponent("black", black.formField));
        formLayout.setWidget(1, 2, new FormFieldComponent("grayDarker", grayDarker.formField));
        formLayout.setWidget(2, 2, new FormFieldComponent("grayDark", grayDark.formField));
        formLayout.setWidget(3, 2, new FormFieldComponent("gray", gray.formField));
        formLayout.setWidget(4, 2, new FormFieldComponent("grayLight", grayLight.formField));
        formLayout.setWidget(5, 2, new FormFieldComponent("grayLighter", grayLighter.formField));
        formLayout.setWidget(6, 2, new FormFieldComponent("white", white.formField));

        formLayout.setWidget(0, 3, blackPreview);
        formLayout.setWidget(1, 3, grayDarkerPreview);
        formLayout.setWidget(2, 3, grayDarkPreview);
        formLayout.setWidget(3, 3, grayPreview);
        formLayout.setWidget(4, 3, grayLightPreview);
        formLayout.setWidget(5, 3, grayLighterPreview);
        formLayout.setWidget(6, 3, whitePreview);

        final PButton validateButton = new PButton("Validate");
        validateButton.setStyleName("pony-PButton accent");
        validateButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final boolean isValid = form.isValid();
                if (isValid) {
                    updateClientColorAndRefreshLess();
                }
            }

        });

        layout.add(formLayout);
        layout.add(validateButton);

        final PListBox themesSelector = new PListBox();
        themesSelector.addItem(STANDARD);
        themesSelector.addItem(DARK);
        themesSelector.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final PChangeEvent event) {
                final String selectedItem = themesSelector.getSelectedItem();
                setTheme(selectedItem);
            }
        });

        layout.add(new PHTML("<br><br><br>"));
        layout.add(new PLabel("You can easily compile many themes"));
        layout.add(new PLabel("Try some of it: "));
        layout.add(themesSelector);

        examplePanel.setWidget(layout);
    }

    protected void updateClientColorAndRefreshLess() {
        final StringBuilder js = new StringBuilder();
        js.append("window.colors.header = \"#" + header.getValue() + "\";");
        js.append("window.colors.headerTextColor = \"#" + headerTextColor.getValue() + "\";");
        js.append("window.colors.footer = \"#" + footer.getValue() + "\";");
        js.append("window.colors.footerTextColor = \"#" + footerTextColor.getValue() + "\";");
        js.append("window.colors.highlight = \"#" + highlight.getValue() + "\";");
        js.append("window.colors.black = \"#" + black.getValue() + "\";");
        js.append("window.colors.grayDarker = \"#" + grayDarker.getValue() + "\";");
        js.append("window.colors.grayDark = \"#" + grayDark.getValue() + "\";");
        js.append("window.colors.gray = \"#" + gray.getValue() + "\";");
        js.append("window.colors.grayLight = \"#" + grayLight.getValue() + "\";");
        js.append("window.colors.grayLighter = \"#" + grayLighter.getValue() + "\";");
        js.append("window.colors.white = \"#" + white.getValue() + "\";");
        js.append("less.refresh();");

        PScript.execute(PWindow.getMain(), js.toString(), new ExecutionCallback() {

            @Override
            public void onSuccess(final String msg) {
            }

            @Override
            public void onFailure(final String msg) {
                log.error(msg);
            }
        });
    }

    protected void setTheme(final String selectedItem) {
        if (STANDARD.equals(selectedItem)) {
            header.setValue("498BF4");
            headerTextColor.setValue("EDEDED");
            footer.setValue("498BF4");
            footerTextColor.setValue("EDEDED");
            highlight.setValue("498BF4");
            black.setValue("000000");
            grayDarker.setValue("222222");
            grayDark.setValue("333333");
            gray.setValue("555555");
            grayLight.setValue("999999");
            grayLighter.setValue("eeeeee");
            white.setValue("ffffff");
        } else if (DARK.equals(selectedItem)) {
            header.setValue("FF8800");
            headerTextColor.setValue("333333");
            footer.setValue("FF8800");
            footerTextColor.setValue("333333");
            highlight.setValue("FF8800");
            black.setValue("FFFFFF");
            grayDarker.setValue("EEEEEE");
            grayDark.setValue("CCCCCC");
            gray.setValue("999999");
            grayLight.setValue("666666");
            grayLighter.setValue("444444");
            white.setValue("111111");
        }

    }

    private ColorFormField buildColorFormField(final String defaultValue, final PSimplePanel preview) {
        final StringTextBoxFormField ff = new StringTextBoxFormField();
        ff.setValidator(colorValidator);
        ff.setValue(defaultValue);
        ff.getWidget().addValueChangeHandler(new PValueChangeHandler<String>() {

            @Override
            public void onValueChange(final PValueChangeEvent<String> event) {
                if (colorValidator.isValid(event.getValue()).isValid()) {
                    preview.setStyleProperty("backgroundColor", "#" + event.getValue());
                }
            }
        });

        preview.setWidth("28px");
        preview.setHeight("28px");
        preview.setStyleProperty("border", "1px solid black");
        preview.setStyleProperty("position", "relative");
        preview.setStyleProperty("bottom", "-10px");
        preview.setStyleProperty("backgroundColor", "#" + defaultValue);

        return new ColorFormField(ff, preview);
    }

    private class ColorValidator implements FieldValidator {

        @Override
        public ValidationResult isValid(final String value) {
            if (value == null || value.isEmpty()) return ValidationResult.newFailedValidationResult("Empty field");
            if (!Pattern.matches("[A-Fa-f0-9]{6}", value)) return ValidationResult.newFailedValidationResult("Not a color (#123456)");
            return ValidationResult.newOKValidationResult();
        }

    }

    private class ColorFormField {

        protected final StringTextBoxFormField formField;
        protected final PSimplePanel preview;

        public ColorFormField(final StringTextBoxFormField formField, final PSimplePanel preview) {
            this.formField = formField;
            this.preview = preview;
        }

        public String getValue() {
            return formField.getValue();
        }

        public void setValue(final String n) {
            formField.setValue(n);
            preview.setStyleProperty("backgroundColor", "#" + n);
        }

    }
}
