
package com.ponysdk.test.server.list2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.test.server.mock.EmptySession;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.list2.DefaultSimpleListView;
import com.ponysdk.ui.server.list2.refreshable.Cell;
import com.ponysdk.ui.server.list2.refreshable.RefreshableCellRenderer;
import com.ponysdk.ui.server.list2.refreshable.RefreshableDataGrid;
import com.ponysdk.ui.server.list2.refreshable.RefreshableDataGridColumnDescriptor;
import com.ponysdk.ui.server.list2.renderer.header.HeaderCellRenderer;
import com.ponysdk.ui.server.list2.valueprovider.IdentityValueProvider;

public class RefreshableDataGridTest {

    private static final Logger log = LoggerFactory.getLogger(RefreshableDataGridTest.class);

    @Rule
    public TestName name = new TestName();

    private RefreshableDataGrid<String, String> datagrid;

    @Before
    public void beforeTest() {
        log.info("Running #" + name.getMethodName());

        final Application application = new Application(new EmptySession());
        final UIContext uiContext = new UIContext(application);
        UIContext.setCurrent(uiContext);
        PPusher.initialize();

        final RefreshableDataGridColumnDescriptor<String, String, PHTML> descriptor = new RefreshableDataGridColumnDescriptor<String, String, PHTML>();
        descriptor.setValueProvider(new IdentityValueProvider<String>());
        descriptor.setHeaderCellRenderer(new HeaderCellRenderer() {

            @Override
            public IsPWidget render() {
                return new PLabel("The header");
            }
        });
        descriptor.setCellRenderer(new RefreshableCellRenderer<String, PHTML>() {

            @Override
            public void update(final String value, final Cell<String, PHTML> previous) {
                previous.getW().setHTML(value);
            }

            @Override
            public PHTML render(final int row, final String value) {
                return new PHTML(value);
            }
        });

        datagrid = new RefreshableDataGrid<String, String>(new DefaultSimpleListView());
        datagrid.addDataGridColumnDescriptor(descriptor);
    }

    @After
    public void afterTest() {
        UIContext.remove();
    }

    @Test
    public void testSetData01() {
        checkRowCount(0);
        checkVisibleItemCount(0);
        checkIndex("01", -1);
    }

    @Test
    public void testSetData02() {
        datagrid.setData("01", "Data 01");

        checkRowCount(1);
        checkVisibleItemCount(1);
        checkIndex("01", 0);

        datagrid.setData("02", "Data 02");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("02", 1);
    }

    @Test
    public void testSetData03() {
        datagrid.setData("00", "Data 00");
        datagrid.setData("01", "Data 01");
        datagrid.setData("02", "Data 02");
        datagrid.setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        insertColspan(2);

        checkRowCount(5);
        checkVisibleItemCount(4);
        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 3);
        checkIndex("03", 4);
    }

    @Test
    public void testSetData04() {
        datagrid.setData("00", "Data 00");
        datagrid.setData("01", "Data 01");

        checkRowCount(2);
        checkVisibleItemCount(2);

        checkIndex("00", 0);
        checkIndex("01", 1);

        insertColspan(2);

        checkRowCount(3);
        checkVisibleItemCount(2);

        datagrid.setData("02", "Data 02");

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 3);
    }

    @Test
    public void testSetData05() {
        datagrid.setData("00", "Data 00");
        datagrid.setData("01", "Data 01");

        checkRowCount(2);
        checkVisibleItemCount(2);

        checkIndex("00", 0);
        checkIndex("01", 1);

        insertColspan(2);
        insertColspan(3);
        insertColspan(4);

        checkRowCount(5);
        checkVisibleItemCount(2);

        datagrid.setData("02", "Data 02");

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 5);
    }

    @Test
    public void testSetData06() {
        datagrid.setData("00", "Data 00");
        datagrid.setData("01", "Data 01");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("00", 0);
        checkIndex("01", 1);

        insertColspan(2);
        insertColspan(3);
        insertColspan(4);

        checkRowCount(5);
        checkVisibleItemCount(2);

        datagrid.setData("02", "Data 02");

        checkRowCount(6);
        checkVisibleItemCount(3);
        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 5);

        insertColspan(6);

        checkRowCount(7);
        checkVisibleItemCount(3);
        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 5);

        datagrid.setData("03", "Data 03");

        checkRowCount(8);
        checkVisibleItemCount(4);
        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 5);
        checkIndex("03", 7);
    }

    private void insertColspan(final int row) {
        datagrid.insertRow(row, 0, datagrid.getColumnDescriptors().size() + 1, new PLabel("colspan " + row));
    }

    private void checkIndex(final String key, final int expectedIndex) {
        Assert.assertEquals(expectedIndex, datagrid.getRow(key));
    }

    private void checkVisibleItemCount(final int expectedSize) {
        Assert.assertEquals(expectedSize, datagrid.getVisibleItemCount());
    }

    private void checkRowCount(final int expectedSize) {
        Assert.assertEquals(expectedSize, datagrid.getRowCount());
    }

}
