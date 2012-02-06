/*============================================================================
 *
 * Copyright (c) 2000-2008 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.ui.server.form;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.ui.server.addon.PDialogBox;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

/**
 * @author mjbenjamin
 */
public class WizardActivity extends AbstractActivity {

    protected List<FormActivity> formActivities = new ArrayList<FormActivity>();

    protected PSimplePanel currentFormActivityPanel;

    protected boolean lastActivity = false;

    private final PButton previous = new PButton("Previous");

    private final PButton finish = new PButton("Finish");

    private final PButton cancel = new PButton("Cancel");

    private final PButton next = new PButton("Next");

    protected FormActivity currentFormActivity;

    int index;

    protected final List<WizardActivityHandler> wizardActivityHandlers = new ArrayList<WizardActivityHandler>();

    private final PDialogBox container;

    public interface WizardActivityHandler {

        public void onNext(FormActivity currentFormActivity);

        public void onPrevious(FormActivity currentFormActivity);

        public void onFinish(FormActivity currentFormActivity);
    }

    public WizardActivity(String caption, List<FormActivity> formActivities, PDialogBox container) {
        this.formActivities = formActivities;
        this.container = container;
    }

    @Override
    public void start(PAcceptsOneWidget world) {
        final PVerticalPanel verticalPanel = new PVerticalPanel();
        currentFormActivityPanel = new PSimplePanel();
        verticalPanel.add(currentFormActivityPanel);

        final PHorizontalPanel horizontalPanel = new PHorizontalPanel();
        verticalPanel.add(horizontalPanel);
        next.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                if (currentFormActivity.isValid() && index != (formActivities.size() - 1)) {
                    currentFormActivityPanel.clear();
                    for (final WizardActivityHandler handler : wizardActivityHandlers) {
                        handler.onNext(currentFormActivity);
                    }
                    index++;
                    currentFormActivity = formActivities.get(index);
                    currentFormActivity.start(currentFormActivityPanel);
                    update();
                }

            }
        });
        horizontalPanel.add(previous);
        previous.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                if (index != 0) {
                    currentFormActivityPanel.clear();
                    for (final WizardActivityHandler handler : wizardActivityHandlers) {
                        handler.onPrevious(currentFormActivity);
                    }
                    index--;
                    currentFormActivity = formActivities.get(index);
                    currentFormActivity.start(currentFormActivityPanel);
                    update();
                }
            }
        });
        horizontalPanel.add(next);
        horizontalPanel.add(finish);
        horizontalPanel.add(cancel);
        finish.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                boolean valid = true;
                for (final FormActivity activity : formActivities) {
                    if (!activity.isValid()) valid = false;
                }
                if (valid) {
                    for (final WizardActivityHandler handler : wizardActivityHandlers) {
                        handler.onFinish(currentFormActivity);
                    }
                }
            }
        });
        cancel.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent arg0) {
                container.hide();
            }
        });

        index = 0;
        currentFormActivity = formActivities.get(index);
        update();
        currentFormActivity.start(currentFormActivityPanel);

        world.setWidget(verticalPanel);
    }

    public void update() {
        finish.setVisible(index == (formActivities.size() - 1));
        next.setVisible(index < (formActivities.size() - 1));
        previous.setVisible(index != 0);

    }

    public List<FormActivity> getFormActivities() {
        return formActivities;
    }

    public void addFormActivity(FormActivity formActivity) {
        this.formActivities.add(formActivity);
        update();
    }

    public void addWizardActivityHandler(WizardActivityHandler handler) {
        wizardActivityHandlers.add(handler);
    }

}
