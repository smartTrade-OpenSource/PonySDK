/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PWidget;

/**
 * Manages and displays a Pagination Component
 * 
 * @see PagerView
 * @param <T>
 */
public class Pager<T> implements IsPWidget {

    protected final int pageSize;
    protected final int pagingWindow;

    protected int currentPage = 0;

    protected final PagerView view;

    public Pager(final PagerView view) {
        this(view, 20);
    }

    public Pager(final PagerView view, final int pageSize) {
        this(view, pageSize, 5);
    }

    public Pager(final PagerView view, final int pageSize, final int pagingWindow) {
        this.view = view;
        this.pageSize = pageSize;
        this.pagingWindow = pagingWindow;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(final int newPageNum) {
        currentPage = newPageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void addListener(final PagerListener listener) {
        view.addPagerListener(listener);
    }

    public void process(final int itemCount) {
        view.clear();
        int pageCount = 0;
        final int rest = itemCount % pageSize;
        if (rest != 0) {
            pageCount = (itemCount - rest) / pageSize + 1;
        } else pageCount = (itemCount - rest) / pageSize;
        if (itemCount == 0 || pageCount == 1) {
            view.asWidget().setVisible(false);
            return;
        } else {
            view.asWidget().setVisible(true);
        }

        currentPage = Math.min(pageCount - 1, currentPage);
        view.setStart(currentPage != 0 ? true : false, 0);
        view.setEnd(currentPage != pageCount - 1 ? true : false, pageCount - 1);

        view.setPrevious(currentPage != 0 ? true : false, currentPage - 1);
        view.setNext(currentPage != pageCount - 1 ? true : false, currentPage + 1);

        int page = currentPage - pagingWindow / 2;
        if (page + pagingWindow > pageCount) {
            page -= (page + pagingWindow) - pageCount;
        }

        for (int tempPageCount = 0; tempPageCount < pagingWindow && page < pageCount; page++) {
            if (page >= 0) {
                view.addPageIndex(page);
                ++tempPageCount;
            }
        }
        view.setSelectedPage(currentPage);
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    public PagerView getView() {
        return view;
    }
}
