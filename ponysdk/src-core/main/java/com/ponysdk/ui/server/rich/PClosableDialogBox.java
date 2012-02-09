
package com.ponysdk.ui.server.rich;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PPositionCallback;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class PClosableDialogBox extends PPopupPanel {

    private final PSimplePanel captionContainer;

    private final PSimplePanel closeContainer;

    private final PSimplePanel contentContainer;

    public PClosableDialogBox() {
        this(false);
    }

    public PClosableDialogBox(final boolean modal) {
        super(false);
        setModal(modal);

        setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX);

        captionContainer = new PSimplePanel();
        closeContainer = new PSimplePanel();
        contentContainer = new PSimplePanel();
        captionContainer.setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX_CAPTION);
        closeContainer.setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX_CLOSE);
        contentContainer.setStyleName(PonySDKTheme.CLOSABLE_DIALOGBOX_CONTENT);

        PFlexTable layout = new PFlexTable();
        layout.setCellPadding(0);
        layout.setCellSpacing(0);
        layout.setStyleProperty("border", "none");
        layout.setWidget(0, 0, captionContainer);
        layout.setWidget(0, 1, closeContainer);
        layout.setWidget(1, 0, contentContainer);

        layout.getFlexCellFormatter().setColSpan(1, 0, 2);
        layout.getFlexCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
        layout.getFlexCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
        layout.getFlexCellFormatter().addStyleName(0, 0, PonySDKTheme.CLOSABLE_DIALOGBOX_HEADER);
        layout.getFlexCellFormatter().addStyleName(0, 1, PonySDKTheme.CLOSABLE_DIALOGBOX_HEADER);
        layout.getColumnFormatter().setWidth(1, "30px");

        closeContainer.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                hide();
            }
        }, PClickEvent.TYPE);

        setWidget(layout);
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

    public void displayAtCenter() {
        setPopupPositionAndShow(new PPositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                setPopupPosition((windowWidth - offsetWidth) / 2, (windowHeight - offsetHeight) / 2);
            }
        });
    }
}
