
package com.ponysdk.impl.webapplication.page;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ponysdk.core.place.Place;
import com.ponysdk.core.security.Permission;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.impl.webapplication.page.place.PagePlace;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTabPanel;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public abstract class TabGroupPageActivity<T> extends PageActivity {

    protected final Map<T, Integer> indexesByTab = new HashMap<T, Integer>();
    protected final Map<Integer, T> tabByIndexes = new HashMap<Integer, T>();
    protected final Map<Integer, Boolean> states = new HashMap<Integer, Boolean>();

    protected Map<String, T> tabsByName;
    protected T currentTab;

    private final PTabPanel tablPanel;

    public TabGroupPageActivity(final String pageName, final String pageCategory, final Map<String, T> tabsByName) {
        this(pageName, pageCategory, tabsByName, Permission.ALLOWED);
    }

    public TabGroupPageActivity(final String pageName, final String pageCategory, final Map<String, T> tabsByName, final Permission permission) {
        super(pageName, pageCategory, permission);

        setPageView(new DefaultPageView());

        this.tabsByName = tabsByName;
        this.currentTab = tabsByName.values().iterator().next();
        this.tablPanel = buildBody();
    }

    protected PTabPanel buildBody() {
        final PTabPanel panel = new PTabPanel();
        panel.addStyleName(PonySDKTheme.PAGE_BODY);
        return panel;
    }

    public void selectTab(final int index) {
        tablPanel.selectTab(index);
    }

    @Override
    protected void onInitialization() {
        pageView.getBody().setWidget(tablPanel);

        int index = 0;

        for (final Entry<String, T> partition : tabsByName.entrySet()) {
            final PScrollPanel content = new PScrollPanel();
            content.setSizeFull();
            content.addStyleName(PonySDKTheme.PAGE_BODY);

            tablPanel.add(content, partition.getKey());

            indexesByTab.put(partition.getValue(), index);
            tabByIndexes.put(index, partition.getValue());

            index++;
        }
    }

    @Override
    protected void onFirstShowPage() {
        tablPanel.addSelectionHandler(new PSelectionHandler<Integer>() {

            @Override
            public void onSelection(final PSelectionEvent<Integer> event) {
                fireOnShowPage(event.getSelectedItem());
            }

        });
        fireOnShowPage(0);
    }

    protected void fireOnShowPage(final Integer selectedItem) {
        final T tab = tabByIndexes.get(selectedItem);

        if (!tab.equals(currentTab)) {
            onLeavingPage(tab);
        }

        currentTab = tab;

        final Boolean state = states.get(selectedItem);

        if (!Boolean.TRUE.equals(state)) {
            states.put(selectedItem, true);
            onFirstShowPage(currentTab);
        }
        onShowPage(currentTab, null);
    }

    @Override
    protected void onLeavingPage() {
        onLeavingPage(currentTab);
    }

    @Override
    protected void onShowPage(final Place place) {
        onShowPage(currentTab, place);
    }

    protected abstract void onFirstShowPage(T partition);

    protected abstract void onShowPage(T partition, Place place);

    protected abstract void onInitialization(T partition);

    protected abstract void onLeavingPage(T partition);

    protected abstract void addHandlers();

    @Override
    public void goToPage(final PagePlace place) {
        place.getPageActivity().goToPage(place);
    }

    protected PSimplePanel getBody(final T partition) {
        return (PSimplePanel) tablPanel.getWidget(indexesByTab.get(partition)).asWidget();
    }
}
