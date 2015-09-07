
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
import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.test.server.mock.EmptyTxnContext;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.list.DefaultSimpleListView;
import com.ponysdk.ui.server.list.refreshable.Cell;
import com.ponysdk.ui.server.list.refreshable.RefreshableCellRenderer;
import com.ponysdk.ui.server.list.refreshable.RefreshableDataGrid;
import com.ponysdk.ui.server.list.refreshable.RefreshableDataGridColumnDescriptor;
import com.ponysdk.ui.server.list.renderer.header.HeaderCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.IdentityValueProvider;

public class RefreshableDataGridTest {

    private static final Logger log = LoggerFactory.getLogger(RefreshableDataGridTest.class);

    @Rule
    public TestName name = new TestName();

    private RefreshableDataGrid<String, Data> datagrid;

    private Txn txn;

    @Before
    public void beforeTest() {
        log.info("Running #" + name.getMethodName());

        final Application application = new Application("test", "Test", new EmptyTxnContext(), new ApplicationManagerOption());
        final UIContext uiContext = new UIContext(application);
        UIContext.setCurrent(uiContext);
        txn = Txn.get();
        txn.begin(new EmptyTxnContext());
        PPusher.initialize();

        final RefreshableDataGridColumnDescriptor<Data, Data, PHTML> descriptor = new RefreshableDataGridColumnDescriptor<>();
        descriptor.setValueProvider(new IdentityValueProvider<Data>());
        descriptor.setHeaderCellRenderer(new HeaderCellRenderer() {

            @Override
            public IsPWidget render() {
                return new PLabel("The header");
            }
        });
        descriptor.setCellRenderer(new RefreshableCellRenderer<Data, PHTML>() {

            @Override
            public void update(final Data value, final Cell<Data, PHTML> previous) {
                previous.getW().setHTML(value.v1);
            }

            @Override
            public PHTML render(final int row, final Data value) {
                return new PHTML(value.v1);
            }
        });

        datagrid = new RefreshableDataGrid<>(new DefaultSimpleListView());
        datagrid.addDataGridColumnDescriptor(descriptor);
    }

    @After
    public void afterTest() {
        txn.commit();
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
        setData("01", "Data 01");

        checkRowCount(1);
        checkVisibleItemCount(1);
        checkIndex("01", 0);

        setData("02", "Data 02");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("02", 1);
    }

    @Test
    public void testSetData03() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

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
        // XX/2
        checkIndex("02", 3);
        checkIndex("03", 4);
    }

    @Test
    public void testSetData04() {
        setData("00", "Data 00");
        setData("01", "Data 01");

        checkRowCount(2);
        checkVisibleItemCount(2);

        checkIndex("00", 0);
        checkIndex("01", 1);

        insertColspan(2);

        checkRowCount(3);
        checkVisibleItemCount(2);

        setData("02", "Data 02");

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 3);
    }

    @Test
    public void testSetData05() {
        setData("00", "Data 00");
        setData("01", "Data 01");

        checkRowCount(2);
        checkVisibleItemCount(2);

        checkIndex("00", 0);
        checkIndex("01", 1);

        insertColspan(2);
        insertColspan(3);
        insertColspan(4);

        checkRowCount(5);
        checkVisibleItemCount(2);

        setData("02", "Data 02");

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 5);
    }

    @Test
    public void testSetData06() {
        setData("00", "Data 00");
        setData("01", "Data 01");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("00", 0);
        checkIndex("01", 1);

        insertColspan(2);
        insertColspan(3);
        insertColspan(4);

        checkRowCount(5);
        checkVisibleItemCount(2);

        setData("02", "Data 02");

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

        setData("03", "Data 03");

        checkRowCount(8);
        checkVisibleItemCount(4);
        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 5);
        checkIndex("03", 7);
    }

    @Test
    public void testRemoveData01() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        remove("02");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("00", 0);
        checkIndex("01", 1);
    }

    @Test
    public void testRemoveData02() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        remove("01");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("00", 0);
        checkIndex("02", 1);
    }

    @Test
    public void testRemoveData03() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        remove("00");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("01", 0);
        checkIndex("02", 1);
    }

    @Test
    public void testRemoveData04() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        remove(2);

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("00", 0);
        checkIndex("01", 1);
    }

    @Test
    public void testRemoveData05() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        remove(1);

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("00", 0);
        checkIndex("02", 1);
    }

    @Test
    public void testRemoveData06() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        remove(0);

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("01", 0);
        checkIndex("02", 1);
    }

    @Test
    public void testRemoveAndAdd01() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        remove("00");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("01", 0);
        checkIndex("02", 1);

        setData("00", "Data 00");

        checkRowCount(3);
        checkVisibleItemCount(3);
        checkIndex("01", 0);
        checkIndex("02", 1);
        checkIndex("00", 2);
    }

    @Test
    public void testRemoveAndAdd02() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");

        checkRowCount(3);
        checkVisibleItemCount(3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);

        // Change hashcode
        final Data data = datagrid.getData("00");
        data.v2 = 10;

        remove("00");

        checkRowCount(2);
        checkVisibleItemCount(2);
        checkIndex("01", 0);
        checkIndex("02", 1);

        setData("00", "Data 00");

        checkRowCount(3);
        checkVisibleItemCount(3);
        checkIndex("01", 0);
        checkIndex("02", 1);
        checkIndex("00", 2);
    }

    @Test
    public void testMove01() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        moveRow("02", 0);

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("02", 0);
        checkIndex("00", 1);
        checkIndex("01", 2);
        checkIndex("03", 3);
    }

    @Test
    public void testMove02() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        moveRow("02", 3);

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("03", 2);
        checkIndex("02", 3);
    }

    @Test
    public void testMove03() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        moveRow("01", 3);

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("02", 1);
        checkIndex("03", 2);
        checkIndex("01", 3);

        moveRow("01", 2);

        checkIndex("00", 0);
        checkIndex("02", 1);
        checkIndex("01", 2);
        checkIndex("03", 3);

        moveRow("02", 2);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);
    }

    @Test
    public void testMove04() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        moveRow("00", 2);

        checkIndex("01", 0);
        checkIndex("02", 1);
        checkIndex("00", 2);
        checkIndex("03", 3);

        moveRow("00", 2);

        checkIndex("01", 0);
        checkIndex("02", 1);
        checkIndex("00", 2);
        checkIndex("03", 3);

        moveRow("00", 2);

        checkIndex("01", 0);
        checkIndex("02", 1);
        checkIndex("00", 2);
        checkIndex("03", 3);
    }

    @Test
    public void testMoveAndAdd01() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        moveRow("02", 3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("03", 2);
        checkIndex("02", 3);

        setData("04", "Data 04");

        checkRowCount(5);
        checkVisibleItemCount(5);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("03", 2);
        checkIndex("02", 3);
        checkIndex("04", 4);

        moveRow("04", 3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("03", 2);
        checkIndex("04", 3);
        checkIndex("02", 4);
    }

    @Test
    public void testMoveAndAdd02() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        moveRow("02", 3);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("03", 2);
        checkIndex("02", 3);

        insertColspan(1);

        checkRowCount(5);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        // XX/1
        checkIndex("01", 2);
        checkIndex("03", 3);
        checkIndex("02", 4);

        moveRow("02", 0);

        checkIndex("02", 0);
        checkIndex("00", 1);
        // XX/2
        checkIndex("01", 3);
        checkIndex("03", 4);
    }

    @Test
    public void testMoveAndAdd03() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        insertColspan(3);

        checkRowCount(5);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 4);

        moveRow("01", 4);

        checkIndex("00", 0);
        checkIndex("02", 1);
        checkIndex("03", 3);
        checkIndex("01", 4);
    }

    @Test
    public void testMoveAndAdd04() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        insertColspan(1);

        checkRowCount(5);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 2);
        checkIndex("02", 3);
        checkIndex("03", 4);

        moveRow("03", 1);

        checkIndex("00", 0);
        checkIndex("03", 1);
        checkIndex("01", 3);
        checkIndex("02", 4);

        moveRow("03", 1);

        checkIndex("00", 0);
        checkIndex("03", 1);
        checkIndex("01", 3);
        checkIndex("02", 4);

        moveRow("03", 1);

        checkIndex("00", 0);
        checkIndex("03", 1);
        checkIndex("01", 3);
        checkIndex("02", 4);
    }

    @Test
    public void testMoveAndAdd05() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        insertColspan(1);

        checkRowCount(5);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 2);
        checkIndex("02", 3);
        checkIndex("03", 4);

        moveRow("03", 2);

        checkIndex("00", 0);
        checkIndex("03", 2);
        checkIndex("01", 3);
        checkIndex("02", 4);

        moveRow("03", 2);

        checkIndex("00", 0);
        checkIndex("03", 2);
        checkIndex("01", 3);
        checkIndex("02", 4);
    }

    @Test
    public void testRemoveColspan() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

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
        // XX / 2
        checkIndex("02", 3);
        checkIndex("03", 4);

        remove(2);

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);
    }

    @Test
    public void testIterateVisible01() {
        setData("00", "Data 00");
        setData("01", "Data 01");
        setData("02", "Data 02");
        setData("03", "Data 03");

        checkRowCount(4);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 2);
        checkIndex("03", 3);

        int i = 0;
        for (final Data d : datagrid.getVisibleItems()) {
            if (i == 0) checkKey(d.key, "00");
            else if (i == 1) checkKey(d.key, "01");
            else if (i == 2) checkKey(d.key, "02");
            else if (i == 3) checkKey(d.key, "03");
            i++;
        }

        i = 0;
        for (final Data d : datagrid.getVisibleItems()) {
            if (i == 0) checkKey(d.key, "00");
            else if (i == 1) checkKey(d.key, "01");
            else if (i == 2) checkKey(d.key, "02");
            else if (i == 3) checkKey(d.key, "03");
            i++;
        }

        insertColspan(2);

        checkRowCount(5);
        checkVisibleItemCount(4);

        checkIndex("00", 0);
        checkIndex("01", 1);
        checkIndex("02", 3);
        checkIndex("03", 4);

        i = 0;
        for (final Data d : datagrid.getVisibleItems()) {
            if (i == 0) checkKey(d.key, "00");
            else if (i == 1) checkKey(d.key, "01");
            else if (i == 2) checkKey(d.key, "02");
            else if (i == 3) checkKey(d.key, "03");
            i++;
        }
    }

    private void setData(final String key, final String value) {
        final Data d = new Data();
        d.key = key;
        d.v1 = value;
        d.v2 = 0;

        datagrid.setData(key, d);
    }

    private void remove(final String key) {
        datagrid.removeByKey(key);
    }

    private void remove(final int index) {
        datagrid.remove(index);
    }

    private void insertColspan(final int row) {
        datagrid.insertRow(row, 0, datagrid.getColumnDescriptors().size() + 1, new PLabel("colspan " + row));
    }

    private void moveRow(final String key, final int to) {
        datagrid.moveRow(key, to);
    }

    private void checkKey(final String key, final String expectedKey) {
        Assert.assertEquals(expectedKey, key);
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

    private static class Data {

        public String key;
        public String v1;
        public int v2;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
            result = prime * result + v2;
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Data other = (Data) obj;
            if (key == null) {
                if (other.key != null) return false;
            } else if (!key.equals(other.key)) return false;
            if (v1 == null) {
                if (other.v1 != null) return false;
            } else if (!v1.equals(other.v1)) return false;
            if (v2 != other.v2) return false;
            return true;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
