
package com.ponysdk.core.ui.list.paging;

import com.ponysdk.core.ui.basic.IsPWidget;

/**
 * View of a {@link Pager}
 */
public interface PagerView extends IsPWidget {

    void addPagerListener(PagerListener pagerListener);

    void addPageIndex(int pageIndex);

    void setStart(boolean enabled, int pageIndex);

    void setEnd(boolean enabled, int pageIndex);

    void setPrevious(boolean enabled, int pageIndex);

    void setNext(boolean enabled, int pageIndex);

    void setSelectedPage(final int pageIndex);

    void clear();

}
