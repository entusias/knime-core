package org.knime.core.data.sort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 * Test class fot the ColumnDataTableSorter.
 *
 * @author Marcel Hanser
 */
public class AbstractColumnTableSorterTest {
    private static final String CLASS = "Class";

    private static final String STRING_FEATURE = "StringFeature";

    private static final String FEATURE2 = "Feature2";

    private static final String FEATURE1 = "Feature1";

    private ExecutionContext m_exec;

    private BufferedDataTable testTable;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("rawtypes")
    @Before
    public void setUp() throws Exception {
        @SuppressWarnings("unchecked")
        NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        m_exec =
            new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
                SingleNodeContainer.MemoryPolicy.CacheOnDisc, new HashMap<Integer, ContainerTable>());

        DataColumnSpec[] colSpecs =
            new DataColumnSpec[]{new DataColumnSpecCreator(FEATURE1, DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator(FEATURE2, DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator(STRING_FEATURE, StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator(CLASS, StringCell.TYPE).createSpec(),};

        DataTableSpec spec = new DataTableSpec(colSpecs);
        final BufferedDataContainer container = m_exec.createDataContainer(spec);

        int i = 0;
        container.addRowToTable(creatRow(i++, 1, 8, "A", "AClass8"));
        container.addRowToTable(creatRow(i++, 2, 2, "Z", "ZClass2"));
        container.addRowToTable(creatRow(i++, 3, 5, "B", "BClass5"));
        container.addRowToTable(creatRow(i++, 4, 0, "E", "EClass0"));
        container.addRowToTable(creatRow(i++, 5, 1, "F", "FClass1"));
        container.addRowToTable(creatRow(i++, 6, 7, "G", "GClass7"));
        container.addRowToTable(creatRow(i++, 7, 9, "H", "HClass9"));
        container.addRowToTable(creatRow(i++, 8, 8, null, "Class8"));
        container.close();
        testTable = container.getTable();
    }

    /**
     * Tests the default sorting.
     *
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Test
    public void testSorting() throws CanceledExecutionException, InvalidSettingsException {
        assertSorting(FEATURE1);
        assertSorting(STRING_FEATURE);
        assertSorting(CLASS);

        assertSorting(FEATURE1, CLASS);
        assertSorting(FEATURE1, STRING_FEATURE);
    }

    /**
     * Tests the sorting with limited file handles.
     *
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Test
    public void testSortingWithLimitedFileHandler() throws CanceledExecutionException, InvalidSettingsException {

        BufferedDataTable bt = createRandomTable(50, 5000);

        ColumnBufferedDataTableSorter dataTableSorter =
            new ColumnBufferedDataTableSorter(bt.getDataTableSpec(), bt.getRowCount(), bt.getDataTableSpec()
                .getColumnNames());

        dataTableSorter.setMemService(new MemoryService(0.02));
        dataTableSorter.setMaxOpenContainers(60);

        final Comparator<DataRow> ascendingOrderAssertion =
            createAscendingOrderAssertingComparator(bt, bt.getDataTableSpec().getColumnNames());

        dataTableSorter.sort(bt, m_exec, new SortingConsumer() {
            final AtomicReference<DataRow> lastRow = new AtomicReference<>();

            @Override
            public void consume(final DataRow defaultRow) {
                if (lastRow.get() != null) {
                    ascendingOrderAssertion.compare(defaultRow, lastRow.get());
                }
                lastRow.set(defaultRow);
            }
        });
    }

    private BufferedDataTable createRandomTable(final int cols, final int rows) {
        long currentTimeMillis = System.currentTimeMillis();
        System.out.println("Using seed: " + currentTimeMillis);
        Random random = new Random(currentTimeMillis);

        DataTableSpecCreator creator = new DataTableSpecCreator();

        for (int i = 0; i < cols; i++) {
            creator.addColumns(new DataColumnSpecCreator("" + i, DoubleCell.TYPE).createSpec());
        }

        final BufferedDataContainer container = m_exec.createDataContainer(creator.createSpec());
        for (int i = 0; i < rows; i++) {
            DataCell[] rowVals = new DataCell[cols];
            for (int j = 0; j < cols; j++) {
                rowVals[j] = new DoubleCell(random.nextDouble());
            }
            container.addRowToTable(new DefaultRow(Integer.toString(i), rowVals));
            if (i % 1000 == 0) {
                System.out.println("Added row: " + i);
            }
        }
        container.close();
        return container.getTable();
    }

    /**
     * @param toSort
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    private void assertSorting(final String... toSort) throws CanceledExecutionException, InvalidSettingsException {
        ColumnBufferedDataTableSorter dataTableSorter =
            new ColumnBufferedDataTableSorter(testTable.getDataTableSpec(), testTable.getRowCount(), toSort);

        final Comparator<DataRow> ascendingOrderAssertion = createAscendingOrderAssertingComparator(testTable, toSort);

        dataTableSorter.sort(testTable, m_exec, new SortingConsumer() {
            final AtomicReference<DataRow> lastRow = new AtomicReference<>();

            @Override
            public void consume(final DataRow defaultRow) {
                assertEquals(toSort.length, defaultRow.getNumCells());
                if (lastRow.get() != null) {
                    ascendingOrderAssertion.compare(defaultRow, lastRow.get());
                }
                lastRow.set(defaultRow);
            }
        });
    }

    private Comparator<DataRow> createAscendingOrderAssertingComparator(final BufferedDataTable testTable2,
        String... cols) {
        final List<Comparator<DataCell>> innerComp = new ArrayList<>();
        final int[] indexes = new int[cols.length];
        cols = ArrayUtils.isEmpty(cols) ? testTable2.getDataTableSpec().getColumnNames() : cols;
        for (int i = 0; i < cols.length; i++) {
            DataColumnSpec columnSpec = testTable2.getDataTableSpec().getColumnSpec(cols[i]);
            indexes[i] = testTable2.getDataTableSpec().findColumnIndex(cols[i]);
            innerComp.add(columnSpec.getType().getComparator());
        }
        return new Comparator<DataRow>() {

            @Override
            public int compare(final DataRow o1, final DataRow o2) {
                for (int i = 0; i < indexes.length; i++) {
                    Comparator<DataCell> comp = innerComp.get(i);
                    assertTrue(comp.compare(o1.getCell(i), o2.getCell(i)) >= 0);
                }
                return 0;
            }
        };
    }

    private static DataRow creatRow(final int i, final double d, final double e, final String string,
        final String string2) {
        return new DefaultRow(RowKey.createRowKey(i), new DoubleCell(d), new DoubleCell(e), string == null
            ? DataType.getMissingCell() : new StringCell(string), new StringCell(string2));
    }
}