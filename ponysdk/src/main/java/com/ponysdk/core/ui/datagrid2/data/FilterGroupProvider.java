package com.ponysdk.core.ui.datagrid2.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FilterGroupProvider {
	private Map<String, Set<String>> filtersIDByGroupName = new HashMap<>();
	private Map<String, String> groupNameByFilterID = new HashMap<>();

	/**
	 * Add a filter to a group.
	 * @param groupName Name of the group.
	 * @param filterID Filter ID to add.
	 */
	public void addFilter(String groupName, String filterID) {
		Set<String> filtersID = filtersIDByGroupName.computeIfAbsent(groupName, k -> new HashSet<>());
		filtersID.add(filterID);
		groupNameByFilterID.put(filterID, groupName);
	}

	/**
	 * Retrieve an unmodifiable {@link Set} of filterID associated to the given groupName
	 * @param groupName Name of the group
	 * @return filters ID associated to the given groupName
	 */
	public Set<String> getFiltersID(final String groupName) {
		return Collections.unmodifiableSet(filtersIDByGroupName.get(groupName));
	}
	
	/**
	 * Retrieve group name from filterID. If no group, return filterID as group name.
	 * @param filterID Filter ID
	 * @return Group name
	 */
	public String getGroupName(final String filterID) {
		return groupNameByFilterID.getOrDefault(filterID, filterID);
	}
}
