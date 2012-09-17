
package com.ponysdk.ui.server.list2.header;

import com.ponysdk.core.query.SortingType;
import com.ponysdk.impl.theme.PonySDKTheme;

public class HeaderSortingHelper {

    public static SortingType getNextSortingType(final SortingType sortingType) {
        SortingType newSortingType;

        if (sortingType == SortingType.NONE) newSortingType = SortingType.ASCENDING;
        else if (sortingType == SortingType.DESCENDING) newSortingType = SortingType.ASCENDING;
        else if (sortingType == SortingType.ASCENDING) newSortingType = SortingType.DESCENDING;
        else newSortingType = SortingType.NONE;

        return newSortingType;
    }

    public static String getAssociatedStyleName(final SortingType sortingType) {
        switch (sortingType) {
            case ASCENDING:
                return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_ASCENDING;
            case DESCENDING:
                return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_DESCENDING;
            case NONE:
                return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_NONE;
        }
        return PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE_NONE;
    }

}
