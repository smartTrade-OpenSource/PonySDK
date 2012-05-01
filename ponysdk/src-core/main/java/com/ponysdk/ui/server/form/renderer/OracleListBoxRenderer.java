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

package com.ponysdk.ui.server.form.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.event.PEventHandler;
import com.ponysdk.core.event.PHandlerRegistration;
import com.ponysdk.core.query.CriterionField;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.impl.query.memory.FilteringTools;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.Focusable;
import com.ponysdk.ui.server.basic.HasPValueChangeHandlers;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAttachedPopupPanel;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PTextBoxBase;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.HasPKeyPressHandlers;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.basic.event.PHasText;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class OracleListBoxRenderer implements FormFieldRenderer, PValueChangeHandler<String>, PHasText, HasPValueChangeHandlers<String>, HasPKeyPressHandlers, Focusable {

    private final Map<String, Object> hiddenValueByItems = new HashMap<String, Object>();

    private final Map<Object, String> itemsByHiddenValue = new HashMap<Object, String>();

    private final List<String> items = new ArrayList<String>();

    private final List<PValueChangeHandler<String>> valueChangeHandlers = new ArrayList<PValueChangeHandler<String>>();

    private final List<PKeyPressHandler> keypPressHandlers = new ArrayList<PKeyPressHandler>();

    private final List<PTextBoxBase> fields = new ArrayList<PTextBoxBase>();

    private final int pageSize;

    private String value;

    private final boolean enabled = true;

    private final String caption;

    public int maxCharacterLength;

    private final PTextBox textbox = new PTextBox();

    public OracleListBoxRenderer(final int pageSize, final String caption) {
        this.pageSize = pageSize;
        this.caption = caption;
    }

    private final class KeyUpHandler extends PVerticalPanel implements PKeyUpHandler {

        private final PLabel down;

        protected final PTextBox textBox;

        private final PPopupPanel popup;

        private final PLabel up;

        private final PLabel nextPaginationLabel;

        private final PVerticalPanel popupContent;

        private List<String> matchingElements;

        private final PLabel previousPaginationLabel;

        private final PButton deploy;

        private boolean initialized = false;

        int currentPage = 0;

        int currentSelected = -1;

        boolean deployed = false;

        List<PLabel> currentMatchingElements = new ArrayList<PLabel>();

        private KeyUpHandler(final PTextBox textBox, final PPopupPanel popup, final PButton deploy) {
            // this.addStyleName(PonySDKTheme.ORACLE_LIST_BOX);
            this.previousPaginationLabel = new PLabel();
            this.deploy = deploy;
            this.down = new PLabel();
            down.addStyleName(PonySDKTheme.ORACLE_ARROW);
            down.addStyleName(PonySDKTheme.ORACLE_ARROW_DOWN);
            down.setVisible(false);
            this.up = new PLabel();
            up.addStyleName(PonySDKTheme.ORACLE_ARROW);
            up.addStyleName(PonySDKTheme.ORACLE_ARROW_UP);
            up.setVisible(false);
            initDeploy();
            this.textBox = textBox;
            this.popup = popup;
            this.nextPaginationLabel = new PLabel();
            nextPaginationLabel.setStyleName(PonySDKTheme.ORACLE_PAGINATION);
            this.popupContent = new PVerticalPanel();
            popupContent.setSizeFull();
            setHorizontalAlignment(PHorizontalAlignment.ALIGN_LEFT);
            add(previousPaginationLabel);
            add(up);
            setCellHorizontalAlignment(up, PHorizontalAlignment.ALIGN_CENTER);
            previousPaginationLabel.setStyleName(PonySDKTheme.ORACLE_PAGINATION);
            add(popupContent);
            popup.setWidget(this);
            add(down);
            setCellHorizontalAlignment(down, PHorizontalAlignment.ALIGN_CENTER);
            add(nextPaginationLabel);
        }

        void initDeploy() {
            deploy.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    deployed = !deployed;
                    if (!deployed) {
                        deploy.setText("+");
                        popup.hide();
                        refresh(null);
                    } else {
                        deploy.setText("-");
                        if (textbox.getText() != null) refresh(textbox.getText());
                        else refresh("%");
                    }
                    textbox.setFocus(true);
                }

            });
        }

        @Override
        public void onKeyUp(final int keyCode) {
            final PKeyCodes code = PKeyCodes.fromInt(keyCode);

            if (code == null) {
                currentSelected = -1;
                deploy.setText("+");
                popupContent.clear();
                if (textBox.getText() == null || textBox.getText().equals("")) {
                    popup.hide();
                    return;
                }
                value = textBox.getText();
                refresh(textBox.getText());
            } else {
                switch (code) {
                    case UP:
                        process(-1);
                        break;
                    case DOWN:
                        process(1);
                        break;
                    case ENTER:
                        onValueChange(new PValueChangeEvent<String>(this, textBox.getText()));
                        popup.hide();
                        deploy.setText("+");
                        break;
                    default:
                        break;
                }
            }
        }

        private void process(final int i) {
            if (currentMatchingElements.size() != 0) {
                currentSelected += i;
                switch (i) {
                    case -1:
                        if (currentSelected >= 0) {
                            // case we are selecting an element between first and last of current page
                            refreshLabels();
                        } else if (currentPage > 0) {
                            // case last selected element is the first, there are remaining previous page and
                            // we want to move backward ==> go to previous page
                            goToPreviousPage();
                            refreshLabels();
                        } else {
                            // case we reached the first element of the first page ==> do nothing and stay on
                            // the first element
                            currentSelected = 0;
                        }
                        break;
                    case 1:
                        if (currentSelected != currentMatchingElements.size()) {
                            // case we are selecting an element between first and last of current page
                            refreshLabels();
                        } else {
                            // case we reached the last element of current page and we want to move forward
                            final boolean hasRemainingPage = currentPage < totalPage();
                            if (hasRemainingPage) {
                                // case there are remaining next page => go to next page
                                goToNextPage();
                                refreshLabels();
                            } else {
                                // we stay at the last element of current page
                                currentSelected = currentSelected - 1;
                                return;
                            }
                        }
                        break;
                }

            }

            textbox.setFocus(true);
        }

        int totalPage() {

            if ((matchingElements.size() < pageSize)) return 0;
            else if ((matchingElements.size() % pageSize) == 0) return ((matchingElements.size() / pageSize) - 1);
            else return (matchingElements.size() / pageSize);
        }

        void refreshLabels() {
            for (final PLabel l : currentMatchingElements) {
                l.removeStyleName(PonySDKTheme.ORACLE_LIST_BOX_SELECTED);
            }
            final PLabel selectedLabel = currentMatchingElements.get(currentSelected);
            selectedLabel.addStyleName(PonySDKTheme.ORACLE_LIST_BOX_SELECTED);
            textBox.setText(selectedLabel.getText());
        }

        protected void refresh(final String pattern) {
            if (pattern != null)
            // get String list corresponding to filter patter
            matchingElements = filter(pattern);
            else matchingElements = Collections.emptyList();

            if (!matchingElements.isEmpty()) {
                // there are matching pattern ==> show popup
                popup.hide();
                popup.show();
                if (!initialized) {
                    initHandlers();
                    initialized = true;
                }
                fillContentWithMatchingElements(new ArrayList<String>(matchingElements));
                deployed = true;
                deploy.setText("-");
            } else {
                popup.hide();
                deployed = false;
                deploy.setText("+");
            }
            textbox.setFocus(true);
        }

        private void goToNextPage() {
            currentSelected = 0;
            currentPage++;
            init();
            textBox.setText(textBox.getText());
            textbox.setFocus(true);
        }

        private void initHandlers() {
            down.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    goToNextPage();
                    refreshLabels();
                }

            });
            this.up.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    goToPreviousPage();
                    currentSelected = 0;
                    refreshLabels();
                }

            });
        }

        private void goToPreviousPage() {
            currentPage--;
            init();
            currentSelected = currentMatchingElements.size() - 1;
            textBox.setText(textBox.getText());
            textbox.setFocus(true);
        }

        private void init() {
            popupContent.clear();
            currentMatchingElements.clear();
            int currentPageBeginIndex;
            int currentPageEndIndex;

            final int matchingElementsSize = matchingElements.size();
            // first element index in page
            currentPageBeginIndex = currentPage * pageSize;

            // last page not reached
            if (currentPageBeginIndex + pageSize < matchingElements.size()) {
                currentPageEndIndex = currentPageBeginIndex + pageSize;
                down.setVisible(true);
                nextPaginationLabel.setVisible(true);
            } else {
                // reach the last page, last index is the size of matchinElements
                currentPageEndIndex = matchingElements.size();
                down.setVisible(false);
                nextPaginationLabel.setVisible(false);
            }

            // get element for current page
            final List<String> elementsForCurrentPage = matchingElements.subList(currentPageBeginIndex, currentPageEndIndex);
            // add element to popup
            for (final String s : elementsForCurrentPage) {

                final PLabel label = new PLabel(s);
                currentMatchingElements.add(label);
                label.addStyleName(PonySDKTheme.ORACLE_ITEM);
                label.addClickHandler(new PClickHandler() {

                    @Override
                    public void onClick(final PClickEvent event) {
                        OracleListBoxRenderer.this.onValueChange(new PValueChangeEvent<String>(this, s));
                        popup.hide();
                        deployed = false;
                        deploy.setText("+");
                    }
                });
                popupContent.add(label);
                popupContent.setCellWidth(label, "100%");

            }
            popupContent.setHorizontalAlignment(PHorizontalAlignment.ALIGN_LEFT);
            if (currentPage == 0) {
                // if first page then hide previous pagination label and disable up button
                previousPaginationLabel.setVisible(false);
                up.setVisible(false);
            } else {
                // else there is a page before ==> enable previous pagination label and enable up button
                previousPaginationLabel.setText((currentPageBeginIndex - pageSize + 1) + " - " + (currentPageBeginIndex) + " / " + matchingElementsSize);
                previousPaginationLabel.setVisible(true);
                up.setVisible(true);
            }
            if (currentPageEndIndex == matchingElementsSize) {
                nextPaginationLabel.setVisible(false);
                down.setVisible(false);
            } else {
                final int nextEnd = (currentPageEndIndex + pageSize) > matchingElementsSize ? matchingElementsSize : currentPageEndIndex + pageSize;
                nextPaginationLabel.setText((currentPageEndIndex + 1) + " - " + nextEnd + " / " + matchingElementsSize);
                nextPaginationLabel.setVisible(true);
                down.setVisible(true);
            }
        }

        protected void fillContentWithMatchingElements(final List<String> filt) {
            currentPage = 0;
            this.matchingElements = filt;
            init();
        }
    }

    protected <T extends PTextBox> FormFieldComponent<T> buildTextField(final T t) {
        final FormFieldComponent<T> formFieldComponent = new FormFieldComponent<T>(t);
        formFieldComponent.getInput().setText(value);
        formFieldComponent.getInput().setEnabled(enabled);
        formFieldComponent.setCaption(caption);
        return formFieldComponent;
    }

    @Override
    public IsPWidget render(final FormField formField) {
        final PLabel captionLabel = new PLabel(caption);

        fields.add(textbox);
        final PAttachedPopupPanel popup = new PAttachedPopupPanel(true, textbox);
        popup.asWidget().setStyleName(PonySDKTheme.ORACLE_POPUP_PANEL);

        final PButton deploy = new PButton("+");
        final KeyUpHandler keyUphandler = new KeyUpHandler(textbox, popup, deploy);
        textbox.setStyleName(PonySDKTheme.ORACLE_TEXT_BOX);
        textbox.addKeyUpHandler(keyUphandler);
        textbox.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                keyUphandler.refresh(textbox.getText());
            }
        });

        final PVerticalPanel mainPanel = new PVerticalPanel();
        final PHorizontalPanel textBoxAndDeployButtonPanel = new PHorizontalPanel();
        textBoxAndDeployButtonPanel.add(textbox);
        textBoxAndDeployButtonPanel.add(deploy);
        textBoxAndDeployButtonPanel.setCellVerticalAlignment(textbox, PVerticalAlignment.ALIGN_MIDDLE);
        textBoxAndDeployButtonPanel.setCellVerticalAlignment(deploy, PVerticalAlignment.ALIGN_MIDDLE);
        mainPanel.add(captionLabel);
        mainPanel.add(textBoxAndDeployButtonPanel);

        return mainPanel;
    }

    public void addItem(final String item, final Object hiddenValue) {
        if (item.length() > maxCharacterLength) {
            maxCharacterLength = item.length();
            textbox.setSize(maxCharacterLength);
        }
        items.add(item);
        hiddenValueByItems.put(item, hiddenValue);
        itemsByHiddenValue.put(hiddenValue, item);
    }

    protected List<String> filter(final String filter) {
        final CriterionField criterionField = new CriterionField("");
        criterionField.setValue("%" + filter + "%");
        criterionField.setSortingType(SortingType.ASCENDING);
        List<String> filtered = FilteringTools.filterStringCollection(new ArrayList<String>(hiddenValueByItems.keySet()), Arrays.asList(criterionField));
        filtered = FilteringTools.sortStringCollection(filtered, Arrays.asList(criterionField));
        return filtered;
    }

    @Override
    public void reset() {
        textbox.setText("");
    }

    @Override
    public void addErrorMessage(final String errorMessage) {

    }

    @Override
    public void clearErrorMessage() {

    }

    @Override
    public void setEnabled(final boolean enabled) {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setValue(final Object value) {

    }

    @Override
    public Object getValue() {
        return hiddenValueByItems.get(value);
    }

    @Override
    public void ensureDebugID(final String id) {
        textbox.ensureDebugId(id);
    }

    @Override
    public PHandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        for (final PTextBoxBase field : fields) {
            field.addKeyPressHandler(handler);
        }
        keypPressHandlers.add(handler);

        return new PHandlerRegistration() {

            @Override
            public void removeHandler() {
                keypPressHandlers.remove(handler);
            }
        };
    }

    @Override
    public List<PKeyPressHandler> getKeyPressHandlers() {
        return keypPressHandlers;
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        valueChangeHandlers.add(handler);
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        valueChangeHandlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(valueChangeHandlers);
    }

    @Override
    public String getText() {
        return value;
    }

    @Override
    public void setText(final String text) {
        this.value = text;
        for (final PTextBoxBase field : fields) {
            field.setText(text);
        }
    }

    @Override
    public void onValueChange(final PValueChangeEvent<String> event) {
        setText(event.getValue());
        for (final PValueChangeHandler<String> handler : valueChangeHandlers) {
            handler.onValueChange(event);
        }
    }

    @Override
    public <H extends PEventHandler> void addDomHandler(final H handler, final Type<H> type) {
        for (final PTextBoxBase field : fields) {
            field.addDomHandler(handler, type);
        }
    }

    @Override
    public void setFocus(final boolean focused) {
        textbox.setFocus(true);
    }

}
