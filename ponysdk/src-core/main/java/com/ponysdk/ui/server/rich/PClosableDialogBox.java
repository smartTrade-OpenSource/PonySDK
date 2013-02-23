
package com.ponysdk.ui.server.rich;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class PClosableDialogBox extends PPopupPanel {

    private final PSimplePanel captionContainer;

    private final PSimplePanel closeContainer;

    private final PSimplePanel contentContainer;

    public PClosableDialogBox(final String caption) {
        this(false, new PLabel(caption), new PImage("images/close_16.png"));
    }

    public PClosableDialogBox(final boolean modal, final IsPWidget captionWidget, final IsPWidget closeWidget) {
        super(false);
        setModal(modal);

        setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX);

        captionContainer = new PSimplePanel();
        closeContainer = new PSimplePanel();
        contentContainer = new PSimplePanel();
        captionContainer.setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX_CAPTION);
        closeContainer.setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX_CLOSE);
        contentContainer.setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX_CONTENT);

        final PFlexTable layout = new PFlexTable();
        layout.addStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX_LAYOUT);

        layout.setWidget(0, 0, captionContainer);
        layout.setWidget(0, 1, closeContainer);
        layout.setWidget(1, 0, contentContainer);

        layout.getFlexCellFormatter().setColSpan(1, 0, 2);
        layout.getFlexCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
        layout.getFlexCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
        layout.getFlexCellFormatter().addStyleName(0, 0, PonySDKTheme.CLOSABLE_DIALOGBOX_HEADER);
        layout.getFlexCellFormatter().addStyleName(0, 1, PonySDKTheme.CLOSABLE_DIALOGBOX_HEADER);

        closeContainer.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                hide();
            }
        }, PClickEvent.TYPE);

        super.setWidget(layout);

        captionContainer.setWidget(captionWidget);
        closeContainer.setWidget(closeWidget);
    }

    public void setCaption(final IsPWidget widget) {
        captionContainer.setWidget(widget);
    }

    public void setClose(final IsPWidget widget) {
        closeContainer.setWidget(widget);
    }

    public void setContent(final IsPWidget widget) {
        contentContainer.setWidget(widget);
    }

    @Override
    public void setWidget(final PWidget w) {
        throw new IllegalArgumentException("Use PClosableDialogBox.setContent() to set the content of the popup");
    }

    public void displayAtCenter() {
        setPopupPositionAndShow(new PPositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                setPopupPosition((windowWidth - offsetWidth) / 2, (windowHeight - offsetHeight) / 2);
            }
        });
    }
}
