
package com.ponysdk.ui.server.list.renderer.header;

import com.ponysdk.core.query.SortingType;

public class HeaderSortingHelper {

    public static SortingType getNextSortingType(final SortingType sortingType) {
        SortingType newSortingType;

        if (sortingType == SortingType.NONE)
            newSortingType = SortingType.ASCENDING;
        else if (sortingType == SortingType.DESCENDING)
            newSortingType = SortingType.ASCENDING;
        else if (sortingType == SortingType.ASCENDING)
            newSortingType = SortingType.DESCENDING;
        else
            newSortingType = SortingType.NONE;

        return newSortingType;
    }

    public static String getAssociatedStyleName(final SortingType sortingType) {
        switch (sortingType) {
        case ASCENDING:
            return "ascending";
        case DESCENDING:
            return "descending";
        case NONE:
            return "none";
        }
        return "none";
    }

}
