package com.ponysdk.sample.client.page;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.sample.client.page.component.CounterComponent;
import com.ponysdk.sample.client.page.component.SvelteCounterComponent;
import com.ponysdk.sample.client.page.component.TemplateCounterComponent;
import com.ponysdk.sample.client.page.component.VueCounterComponent;
import com.ponysdk.sample.client.page.component.WebCounterComponent;

/**
 * Demo page showing all PComponent types: React, Vue, Svelte, WebComponent, and Template.
 */
public class PComponentPageActivity extends SamplePageActivity {

    private CounterComponent reactCounter;
    private VueCounterComponent vueCounter;
    private SvelteCounterComponent svelteCounter;
    private WebCounterComponent webCounter;
    private TemplateCounterComponent templateCounter;

    public PComponentPageActivity() {
        super("PComponent", "Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = Element.newPVerticalPanel();
        panel.setSpacing(20);

        // Description
        final PHTML description = Element.newPHTML(
            "<p>This page demonstrates all <b>PComponent</b> types. " +
            "Each counter uses the same props (label, count, color) but different rendering frameworks.</p>" +
            "<p>The server sends JSON props via WebSocket, and each framework renders the UI on the client.</p>"
        );
        panel.add(description);

        // Control buttons
        final PFlowPanel buttonPanel = Element.newPFlowPanel();
        buttonPanel.addStyleName("button-panel");

        final PButton incrementAll = Element.newPButton("Increment All");
        incrementAll.addClickHandler(event -> {
            if (reactCounter != null) reactCounter.increment();
            if (vueCounter != null) vueCounter.increment();
            if (svelteCounter != null) svelteCounter.increment();
            if (webCounter != null) webCounter.increment();
            if (templateCounter != null) templateCounter.increment();
        });

        final PButton decrementAll = Element.newPButton("Decrement All");
        decrementAll.addClickHandler(event -> {
            if (reactCounter != null) reactCounter.decrement();
            if (vueCounter != null) vueCounter.decrement();
            if (svelteCounter != null) svelteCounter.decrement();
            if (webCounter != null) webCounter.decrement();
            if (templateCounter != null) templateCounter.decrement();
        });

        final PButton randomColors = Element.newPButton("Random Colors");
        randomColors.addClickHandler(event -> {
            if (reactCounter != null) reactCounter.setColor(getRandomColor());
            if (vueCounter != null) vueCounter.setColor(getRandomColor());
            if (svelteCounter != null) svelteCounter.setColor(getRandomColor());
            if (webCounter != null) webCounter.setColor(getRandomColor());
            if (templateCounter != null) templateCounter.setColor(getRandomColor());
        });

        buttonPanel.add(incrementAll);
        buttonPanel.add(decrementAll);
        buttonPanel.add(randomColors);
        panel.add(buttonPanel);

        // Framework labels
        final PHTML frameworkInfo = Element.newPHTML(
            "<table style='width:100%; text-align:center; margin-top:20px;'>" +
            "<tr>" +
            "<td><b>React</b><br/><small>PReactComponent</small></td>" +
            "<td><b>Vue</b><br/><small>PVueComponent</small></td>" +
            "<td><b>Svelte</b><br/><small>PSvelteComponent</small></td>" +
            "<td><b>Web Component</b><br/><small>PWebComponent</small></td>" +
            "<td><b>Template</b><br/><small>PTemplateComponent</small></td>" +
            "</tr>" +
            "</table>"
        );
        panel.add(frameworkInfo);

        examplePanel.setWidget(panel);

        // Attach components to window
        final PWindow window = getView().asWidget().getWindow();

        reactCounter = new CounterComponent("React", 0, "#61dafb");
        reactCounter.attach(window);

        vueCounter = new VueCounterComponent("Vue", 0, "#42b883");
        vueCounter.attach(window);

        svelteCounter = new SvelteCounterComponent("Svelte", 0, "#ff3e00");
        svelteCounter.attach(window);

        webCounter = new WebCounterComponent("WebComponent", 0, "#f7df1e");
        webCounter.attach(window);

        templateCounter = new TemplateCounterComponent("Template", 0, "#9b59b6");
        templateCounter.attach(window);
    }

    private String getRandomColor() {
        final String[] colors = {
            "#61dafb", "#42b883", "#ff3e00", "#f7df1e", "#9b59b6",
            "#3498db", "#2ecc71", "#e74c3c", "#f39c12", "#1abc9c"
        };
        return colors[(int) (Math.random() * colors.length)];
    }
}
