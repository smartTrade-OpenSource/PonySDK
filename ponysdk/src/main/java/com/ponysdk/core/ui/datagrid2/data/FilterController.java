package com.ponysdk.core.ui.datagrid2.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;

public class FilterController<V> {
	private Map<String, Set<String>> filterIDsByGroupName = new HashMap<>();
	private Map<String, String> groupNameByFilterID = new HashMap<>();
	private final Map<String, AbstractFilter<V>> filters = new HashMap<>();
	private final Map<AbstractFilter<V>, String> keys = new HashMap<>();

	public void registerFilterInGroup(String groupName, String filterID) {
		filterIDsByGroupName.computeIfAbsent(groupName, k -> new HashSet<>()).add(filterID);
		groupNameByFilterID.put(filterID, groupName);
	}

	public AbstractFilter<V> addFilter(String filterID, AbstractFilter<V> filter) {
		keys.put(filter, filterID);
		return filters.put(filterID, filter);
	}

	public String getGroupName(final String filterID) {
		return groupNameByFilterID.getOrDefault(filterID, filterID);
	}


	public Collection<Collection<AbstractFilter<V>>> getFilterGroups() {
		// The filterGroupProvider not null mean that group(s) of filters
		// exist so we need to construct a map that contain all filters by group(s)
		// to evaluate the acceptance state of the row.
		final Map<String, Collection<AbstractFilter<V>>> filtersByGroup = new HashMap<>();
		for (final AbstractFilter<V> filter : filters.values()) {
			final String groupName = getGroupName(keys.get(filter));
			// Add the filter to the given groupName. If the groupName is not already
			// present in the map,
			// we add it and init its value to a new HashSet that contain the filter.
			filtersByGroup.compute(groupName, (k, v) -> {
				Collection<AbstractFilter<V>> filters;
				if (v == null) {
					filters = new HashSet<>();
				} else {
					filters = v;
				}
				filters.add(filter);
				return filters;
			});
		}
		return filtersByGroup.values();
	}

	public boolean clearFilter(String key) {
		AbstractFilter<V> removed = filters.remove(key);
		keys.remove(removed);
		return removed != null;
	}

	public void clearFilters(ColumnDefinition<V> column) {
		final Iterator<AbstractFilter<V>> iterator = filters.values().iterator();
		while (iterator.hasNext()) {
			final AbstractFilter<V> filter = iterator.next();
			final ColumnDefinition<V> filterColumn = filter.getColumnDefinition();
			if (filterColumn != null && filterColumn == column) {
				iterator.remove();
				keys.remove(filter);
			}
		}
	}

	public void clearFilters() {
		filters.clear();
		keys.clear();
	}

}
