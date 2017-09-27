
package com.ponysdk.core.ui.datagrid.dynamic;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ponysdk.core.ui.datagrid.ColumnDescriptor;
import com.ponysdk.core.ui.datagrid.DataGrid;
import com.ponysdk.core.ui.datagrid.View;
import com.ponysdk.core.ui.datagrid.impl.DefaultView;

public class DynamicDataGrid<DataType> extends DataGrid<DataType> {

    private final Configuration configuration;

    public DynamicDataGrid(final Configuration configuration) {
        this(configuration, new DefaultView(), Function.identity());
    }

    public DynamicDataGrid(final Configuration configuration, final Comparator<DataType> comparator) {
        this(configuration, new DefaultView(), Function.identity(), comparator);
    }

    public DynamicDataGrid(final Configuration configuration, final Function<DataType, ?> keyProvider) {
        this(configuration, new DefaultView(), keyProvider);
    }

    public DynamicDataGrid(final Configuration configuration, final Function<DataType, ?> keyProvider,
            final Comparator<DataType> comparator) {
        this(configuration, new DefaultView(), keyProvider, comparator);
    }

    public DynamicDataGrid(final Configuration configuration, final View view, final Function<DataType, ?> keyProvider) {
        this(configuration, view, keyProvider, (Comparator<DataType>) Comparator.naturalOrder());
    }

    public DynamicDataGrid(final Configuration configuration, final View view, final Function<DataType, ?> keyProvider,
            final Comparator<DataType> comparator) {
        super(view, keyProvider, comparator);
        this.configuration = configuration;
        initColumnDescriptor();
    }

    private void initColumnDescriptor() {
        final Predicate<Method> filter = configuration.getFilter();
        Arrays.stream(configuration.getType().getDeclaredMethods()).filter(filter).forEach(this::addColumnDescriptor);
    }

    private void addColumnDescriptor(final Method method) {
        method.setAccessible(true);
        final Function<String, String> captionTransform = configuration.getCaptionTransform();
        final ColumnDescriptor<DataType> columnDescriptor = ColumnDescriptor.newDefault(captionTransform.apply(method.getName()),
            data -> this.invoke(method, data));
        addColumnDescriptor(columnDescriptor);
    }

    private String invoke(final Method method, final DataType data) {
        try {
            return method.invoke(data).toString();
        } catch (final Exception e) {
            return "x";
        }
    }
}
