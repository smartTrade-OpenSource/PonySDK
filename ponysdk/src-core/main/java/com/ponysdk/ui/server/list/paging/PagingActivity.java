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

package com.ponysdk.ui.server.list.paging;

import com.ponysdk.core.deprecated.AbstractActivity;
import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.event.PEventBusAware;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.list.paging.event.PagingSelectionChangeEvent;

public class PagingActivity extends AbstractActivity implements PEventBusAware {

    private int pageSize = 20;

    private static final int PAGING_WINDOW = 5;

    protected final PagingView pagingView;

    protected int activePageIndex;

    private PEventBus eventBus;

    public PagingActivity(final PagingView pagingView) {
        this.pagingView = pagingView;
    }

    public void clear() {
        pagingView.clear();
        pagingView.showPagingBar(false);
        resetPosition();
    }

    public void process(final int fullSize) {
        pagingView.clear();
        int pageCount = 0;
        final int rest = fullSize % pageSize;
        if (rest != 0) {
            pageCount = (fullSize - rest) / pageSize + 1;
        } else pageCount = (fullSize - rest) / pageSize;
        if (fullSize == 0 || pageCount == 1) {
            pagingView.showPagingBar(false);
            return;
        }

        pagingView.showPagingBar(true);
        activePageIndex = Math.min(pageCount - 1, activePageIndex);
        pagingView.setStart(activePageIndex != 0 ? true : false, newCommand(0));
        pagingView.setEnd(activePageIndex != pageCount - 1 ? true : false, newCommand(pageCount - 1));

        pagingView.setPrevious(activePageIndex != 0 ? true : false, newCommand(activePageIndex - 1));
        pagingView.setNext(activePageIndex != pageCount - 1 ? true : false, newCommand(activePageIndex + 1));

        int page = activePageIndex - PAGING_WINDOW / 2;
        if (page + PAGING_WINDOW > pageCount) {
            page -= (page + PAGING_WINDOW) - pageCount;
        }

        for (int tempPageCount = 0; tempPageCount < PAGING_WINDOW && page < pageCount; page++) {
            if (page >= 0) {
                pagingView.addPageIndex(page, newCommand(page));
                ++tempPageCount;
            }
        }
        pagingView.setSelectedPage(activePageIndex);
    }

    public PCommand newCommand(final int pageIndex) {
        return new PCommand() {

            @Override
            public void execute() {
                activePageIndex = pageIndex;
                eventBus.fireEvent(new PagingSelectionChangeEvent(PagingActivity.this, activePageIndex));
            }
        };
    }

    public int getActivePageIndex() {
        return activePageIndex;
    }

    public void resetPosition() {
        this.activePageIndex = 0;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public void start(final PAcceptsOneWidget world) {
        world.setWidget(pagingView);
    }

    @Override
    public void setEventBus(final PEventBus eventBus) {
        this.eventBus = eventBus;
    }

}
