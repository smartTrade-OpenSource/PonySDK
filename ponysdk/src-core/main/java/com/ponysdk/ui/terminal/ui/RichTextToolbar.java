
package com.ponysdk.ui.terminal.ui;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class RichTextToolbar extends Composite {

    /**
     * We use an inner EventHandler class to avoid exposing event methods on the RichTextToolbar itself.
     */
    private class EventHandler implements ClickHandler, ChangeHandler, KeyUpHandler {

        @Override
        public void onChange(ChangeEvent event) {
            Widget sender = (Widget) event.getSource();

            if (sender == backColors) {
                formatter.setBackColor(backColors.getValue(backColors.getSelectedIndex()));
                backColors.setSelectedIndex(0);
            } else if (sender == foreColors) {
                formatter.setForeColor(foreColors.getValue(foreColors.getSelectedIndex()));
                foreColors.setSelectedIndex(0);
            } else if (sender == fonts) {
                formatter.setFontName(fonts.getValue(fonts.getSelectedIndex()));
                fonts.setSelectedIndex(0);
            } else if (sender == fontSizes) {
                formatter.setFontSize(fontSizesConstants[fontSizes.getSelectedIndex() - 1]);
                fontSizes.setSelectedIndex(0);
            }
        }

        @Override
        public void onClick(ClickEvent event) {
            Widget sender = (Widget) event.getSource();

            if (sender == bold) {
                formatter.toggleBold();
            } else if (sender == italic) {
                formatter.toggleItalic();
            } else if (sender == underline) {
                formatter.toggleUnderline();
            } else if (sender == subscript) {
                formatter.toggleSubscript();
            } else if (sender == superscript) {
                formatter.toggleSuperscript();
            } else if (sender == strikethrough) {
                formatter.toggleStrikethrough();
            } else if (sender == indent) {
                formatter.rightIndent();
            } else if (sender == outdent) {
                formatter.leftIndent();
            } else if (sender == justifyLeft) {
                formatter.setJustification(RichTextArea.Justification.LEFT);
            } else if (sender == justifyCenter) {
                formatter.setJustification(RichTextArea.Justification.CENTER);
            } else if (sender == justifyRight) {
                formatter.setJustification(RichTextArea.Justification.RIGHT);
            } else if (sender == insertImage) {
                String url = Window.prompt("Enter an image URL:", "http://");
                if (url != null) {
                    formatter.insertImage(url);
                }
            } else if (sender == createLink) {
                String url = Window.prompt("Enter a link URL:", "http://");
                if (url != null) {
                    formatter.createLink(url);
                }
            } else if (sender == removeLink) {
                formatter.removeLink();
            } else if (sender == hr) {
                formatter.insertHorizontalRule();
            } else if (sender == ol) {
                formatter.insertOrderedList();
            } else if (sender == ul) {
                formatter.insertUnorderedList();
            } else if (sender == removeFormat) {
                formatter.removeFormat();
            } else if (sender == richText) {
                // We use the RichTextArea's onKeyUp event to update the toolbar status.
                // This will catch any cases where the user moves the cursur using the
                // keyboard, or uses one of the browser's built-in keyboard shortcuts.
                updateStatus();
            }
        }

        @Override
        public void onKeyUp(KeyUpEvent event) {
            Widget sender = (Widget) event.getSource();
            if (sender == richText) {
                // We use the RichTextArea's onKeyUp event to update the toolbar status.
                // This will catch any cases where the user moves the cursur using the
                // keyboard, or uses one of the browser's built-in keyboard shortcuts.
                updateStatus();
            }
        }
    }

    private static final RichTextArea.FontSize[] fontSizesConstants = new RichTextArea.FontSize[] { RichTextArea.FontSize.XX_SMALL, RichTextArea.FontSize.X_SMALL, RichTextArea.FontSize.SMALL, RichTextArea.FontSize.MEDIUM,
            RichTextArea.FontSize.LARGE, RichTextArea.FontSize.X_LARGE, RichTextArea.FontSize.XX_LARGE };

    private final EventHandler handler = new EventHandler();

    private final RichTextArea richText;

    private final RichTextArea.Formatter formatter;

    private final VerticalPanel outer = new VerticalPanel();

    private final HorizontalPanel topPanel = new HorizontalPanel();

    private final HorizontalPanel bottomPanel = new HorizontalPanel();

    private ToggleButton bold;

    private ToggleButton italic;

    private ToggleButton underline;

    private ToggleButton subscript;

    private ToggleButton superscript;

    private ToggleButton strikethrough;

    private PushButton indent;

    private PushButton outdent;

    private PushButton justifyLeft;

    private PushButton justifyCenter;

    private PushButton justifyRight;

    private PushButton hr;

    private PushButton ol;

    private PushButton ul;

    private PushButton insertImage;

    private PushButton createLink;

    private PushButton removeLink;

    private PushButton removeFormat;

    private ListBox backColors;

    private ListBox foreColors;

    private ListBox fonts;

    private ListBox fontSizes;

    /**
     * Creates a new toolbar that drives the given rich text area.
     * 
     * @param richText
     *            the rich text area to be controlled
     */
    public RichTextToolbar(RichTextArea richText) {
        this.richText = richText;
        this.formatter = richText.getFormatter();

        outer.add(topPanel);
        outer.add(bottomPanel);
        topPanel.setWidth("100%");
        bottomPanel.setWidth("100%");

        initWidget(outer);
        setStyleName("gwt-RichTextToolbar");
        richText.addStyleName("hasRichTextToolbar");

        if (formatter != null) {
            topPanel.add(bold = createToggleButton("/images/bold.png", "bold"));
            topPanel.add(italic = createToggleButton("/images/italic.png", "italic"));
            topPanel.add(underline = createToggleButton("/images/underline.png", "underline"));
            topPanel.add(subscript = createToggleButton("/images/subscript.png", "subscript"));
            topPanel.add(superscript = createToggleButton("/images/superscript.png", "superscript"));
            topPanel.add(justifyLeft = createPushButton("/images/justifyLeft.png", "justifyLeft"));
            topPanel.add(justifyCenter = createPushButton("/images/justifyCenter.png", "justifyCenter"));
            topPanel.add(justifyRight = createPushButton("/images/justifyRight.png", "justifyRight"));
        }

        if (formatter != null) {
            topPanel.add(strikethrough = createToggleButton("/images/strikeThrough.png", "strikeThrough"));
            topPanel.add(indent = createPushButton("/images/indent.png", "indent"));
            topPanel.add(outdent = createPushButton("/images/outdent.png", "outdent"));
            topPanel.add(hr = createPushButton("/images/hr.png", "hr"));
            topPanel.add(ol = createPushButton("/images/ol.png", "ol"));
            topPanel.add(ul = createPushButton("/images/ul.png", "ul"));
            topPanel.add(insertImage = createPushButton("/images/insertImage.png", "insertImage"));
            topPanel.add(createLink = createPushButton("/images/createLink.png", "createLink"));
            topPanel.add(removeLink = createPushButton("/images/removeLink.png", "removeLink"));
            topPanel.add(removeFormat = createPushButton("/images/removeFormat.png", "removeFormat"));
        }

        if (formatter != null) {
            bottomPanel.add(backColors = createColorList("Background"));
            bottomPanel.add(foreColors = createColorList("Foreground"));
            bottomPanel.add(fonts = createFontList());
            bottomPanel.add(fontSizes = createFontSizes());

            // We only use these handlers for updating status, so don't hook them up
            // unless at least formatter editing is supported.
            richText.addKeyUpHandler(handler);
            richText.addClickHandler(handler);
        }
    }

    private ListBox createColorList(String caption) {
        ListBox lb = new ListBox();
        lb.addChangeHandler(handler);
        lb.setVisibleItemCount(1);

        lb.addItem(caption);
        lb.addItem("white", "white");
        lb.addItem("black", "black");
        lb.addItem("red", "red");
        lb.addItem("green", "green");
        lb.addItem("yellow", "yellow");
        lb.addItem("blue", "blue");
        return lb;
    }

    private ListBox createFontList() {
        ListBox lb = new ListBox();
        lb.addChangeHandler(handler);
        lb.setVisibleItemCount(1);

        lb.addItem("font", "");
        lb.addItem("normal", "");
        lb.addItem("Times New Roman", "Times New Roman");
        lb.addItem("Arial", "Arial");
        lb.addItem("Courier New", "Courier New");
        lb.addItem("Georgia", "Georgia");
        lb.addItem("Trebuchet", "Trebuchet");
        lb.addItem("Verdana", "Verdana");
        return lb;
    }

    private ListBox createFontSizes() {
        ListBox lb = new ListBox();
        lb.addChangeHandler(handler);
        lb.setVisibleItemCount(1);

        lb.addItem("size");
        lb.addItem("xxsmall");
        lb.addItem("xsmall");
        lb.addItem("small");
        lb.addItem("medium");
        lb.addItem("large");
        lb.addItem("xlarge");
        lb.addItem("xxlarge");
        return lb;
    }

    private PushButton createPushButton(String path, String tip) {
        PushButton pb = new PushButton(new Image(path));
        pb.addClickHandler(handler);
        pb.setTitle(tip);
        return pb;
    }

    private ToggleButton createToggleButton(String path, String tip) {
        ToggleButton tb = new ToggleButton(new Image(path));
        tb.addClickHandler(handler);
        tb.setTitle(tip);
        return tb;
    }

    /**
     * Updates the status of all the stateful buttons.
     */
    private void updateStatus() {
        if (formatter != null) {
            bold.setDown(formatter.isBold());
            italic.setDown(formatter.isItalic());
            underline.setDown(formatter.isUnderlined());
            subscript.setDown(formatter.isSubscript());
            superscript.setDown(formatter.isSuperscript());
        }

        if (formatter != null) {
            strikethrough.setDown(formatter.isStrikethrough());
        }
    }
}
