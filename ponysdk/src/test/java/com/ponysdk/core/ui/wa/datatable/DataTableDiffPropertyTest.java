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

package com.ponysdk.core.ui.wa.datatable;

import com.ponysdk.core.ui.component.PropsDiffer;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for DataTable diff proportionality.
 * <p>
 * Feature: ui-library-wrapper, Property 8: DataTable Diff Proportional to Changes
 * </p>
 * <p>
 * For any DataTable with N rows where K rows are modified (K &lt; N), the JSON Patch
 * produced by PropsDiffer SHALL contain operations only for the K modified rows,
 * and the patch size SHALL be smaller than the full N-row JSON representation.
 * </p>
 * <p>
 * <b>Validates: Requirements 8.2</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 8: DataTable Diff Proportional to Changes")
public class DataTableDiffPropertyTest {

    private static final Pattern DATA_INDEX_PATTERN = Pattern.compile("^/data/(\\d+)/");

    private final PropsDiffer<DataTableProps> differ = new PropsDiffer<>();

    /**
     * For any DataTable with N rows where K rows are modified (K &lt; N),
     * the JSON Patch SHALL contain operations only for the K modified rows —
     * no operations should reference unmodified row indices.
     * Additionally, when K is small relative to N, the patch SHALL be smaller
     * than the full N-row JSON.
     * <p>
     * <b>Validates: Requirements 8.2</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 8: DataTable diff is proportional to modified rows")
    void diffProportionalToChanges(
            @ForAll("dataTableWithModifiedRows") DataTableModification modification) {

        final DataTableProps original = modification.original;
        final DataTableProps modified = modification.modified;
        final int totalRows = modification.totalRows;
        final int modifiedCount = modification.modifiedCount;
        final Set<Integer> modifiedIndices = modification.modifiedIndices;

        // Compute the diff
        final Optional<JsonArray> patchOpt = differ.computeDiff(original, modified);

        // There must be a patch since K > 0
        assertTrue(patchOpt.isPresent(),
                "Patch should be present when " + modifiedCount + " of " + totalRows + " rows are modified");

        final JsonArray patch = patchOpt.get();

        // Extract all row indices referenced by patch operations
        final Set<Integer> patchedIndices = new HashSet<>();
        for (int i = 0; i < patch.size(); i++) {
            final JsonObject op = patch.getJsonObject(i);
            final String path = op.getString("path");
            final Matcher matcher = DATA_INDEX_PATTERN.matcher(path);
            if (matcher.find()) {
                patchedIndices.add(Integer.parseInt(matcher.group(1)));
            }
        }

        // All patched indices must be within the set of actually modified rows
        for (final int idx : patchedIndices) {
            assertTrue(modifiedIndices.contains(idx),
                    "Patch references row index " + idx + " which was not modified. "
                            + "Modified indices: " + modifiedIndices);
        }

        // The patch should reference exactly the modified rows (no more, no fewer)
        assertEquals(modifiedIndices, patchedIndices,
                "Patch should reference exactly the " + modifiedCount + " modified row indices");

        // The number of patch operations should be proportional to K, not N.
        // Each modified row has 2 changed fields (name, email), so expect ~2*K operations.
        assertTrue(patch.size() <= modifiedCount * 3,
                "Patch operations (" + patch.size() + ") should be proportional to modified rows ("
                        + modifiedCount + "), not total rows (" + totalRows + ")");

        // When K is at most half of N, the patch should be smaller than full JSON
        if (modifiedCount <= totalRows / 2) {
            final JsonObject fullJson = differ.toJson(modified);
            final String patchString = patch.toString();
            final String fullString = fullJson.toString();

            assertTrue(patchString.length() < fullString.length(),
                    "When K (" + modifiedCount + ") <= N/2 (" + totalRows / 2
                            + "), patch size (" + patchString.length()
                            + " chars) should be smaller than full JSON (" + fullString.length() + " chars)");
        }
    }

    // ========== Data Model ==========

    record DataTableModification(
            DataTableProps original,
            DataTableProps modified,
            int totalRows,
            int modifiedCount,
            Set<Integer> modifiedIndices
    ) {}

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<DataTableModification> dataTableWithModifiedRows() {
        // N rows between 10 and 40, K modified rows between 1 and N/2
        // (keeping K <= N/2 ensures the patch-smaller-than-full-JSON property holds)
        return Arbitraries.integers().between(10, 40).flatMap(totalRows ->
                Arbitraries.integers().between(1, totalRows / 2).flatMap(modifiedCount ->
                        Arbitraries.randoms().map(random -> buildModification(totalRows, modifiedCount, random))
                )
        );
    }

    private DataTableModification buildModification(final int totalRows, final int modifiedCount,
                                                     final Random random) {
        final List<ColumnDef> columns = List.of(
                ColumnDef.of("id", "ID"),
                ColumnDef.sortable("name", "Name"),
                ColumnDef.of("email", "Email")
        );

        // Build original rows
        final List<Map<String, Object>> originalRows = new ArrayList<>();
        for (int i = 0; i < totalRows; i++) {
            final Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "row-" + i);
            row.put("name", "User " + i);
            row.put("email", "user" + i + "@test.com");
            originalRows.add(row);
        }

        // Pick K random distinct indices to modify
        final List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < totalRows; i++) indices.add(i);
        Collections.shuffle(indices, random);
        final Set<Integer> modifiedIndices = new TreeSet<>(indices.subList(0, modifiedCount));

        // Build modified rows — only change the selected K rows
        final List<Map<String, Object>> modifiedRows = new ArrayList<>();
        for (int i = 0; i < totalRows; i++) {
            if (modifiedIndices.contains(i)) {
                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", "row-" + i);
                row.put("name", "Changed " + i);
                row.put("email", "changed" + i + "@test.com");
                modifiedRows.add(row);
            } else {
                modifiedRows.add(originalRows.get(i));
            }
        }

        final DataTableProps original = DataTableProps.defaults()
                .withColumns(columns)
                .withData(originalRows)
                .withTotalRows(totalRows);

        final DataTableProps modified = DataTableProps.defaults()
                .withColumns(columns)
                .withData(modifiedRows)
                .withTotalRows(totalRows);

        return new DataTableModification(original, modified, totalRows, modifiedCount, modifiedIndices);
    }
}
