
package com.ponysdk.ui.server.list2.paging;

import com.ponysdk.ui.server.basic.IsPWidget;

public interface PagerView extends IsPWidget {

    public void addPagerListener(PagerListener pagerListener);

    public void addPageIndex(int pageIndex);

    public void setStart(boolean enabled, int pageIndex);

    public void setEnd(boolean enabled, int pageIndex);

    public void setPrevious(boolean enabled, int pageIndex);

    public void setNext(boolean enabled, int pageIndex);

    public void setSelectedPage(final int pageIndex);

    void clear();

}
