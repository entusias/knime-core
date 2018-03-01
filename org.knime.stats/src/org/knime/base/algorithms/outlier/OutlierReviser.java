/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 21, 2018 (ortmann): created
 */
package org.knime.base.algorithms.outlier;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knime.base.algorithms.outlier.listeners.Warning;
import org.knime.base.algorithms.outlier.listeners.WarningListener;
import org.knime.base.algorithms.outlier.options.OutlierReplacementStrategy;
import org.knime.base.algorithms.outlier.options.OutlierTreatmentOption;
import org.knime.base.node.preproc.groupby.GroupKey;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * The algorithm to treat outliers based on the permitted intervals provided.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class OutlierReviser {

    /** Outlier treatment and output generation routine message. */
    private static final String TREATMENT_MSG = "Treating outliers and generating output";

    /** Empty table warning text. */
    private static final String EMPTY_TABLE_WARNING = "Node created an empty data table";

    /** The outlier treatment option. */
    private final OutlierTreatmentOption m_treatment;

    /** The outlier replacement strategy. */
    private final OutlierReplacementStrategy m_repStrategy;

    /** List of listeners receiving warning messages. */
    private final List<WarningListener> m_listeners;

    /** The outlier column names. */
    private String[] m_outlierColNames;

    /** The outlier free table. */
    private BufferedDataTable m_outTable;

    /** The table storing the summary. */
    private BufferedDataTable m_summaryTable;

    /** Tells whether the domains of the outlier columns have to be updated after the computation or not. */
    private final boolean m_updateDomain;

    /** Counter storing the number of outliers per column and group. */
    private GroupsMemberCounterPerColumn m_outlierRepCounter;

    /** Counter storing the number of members (non-missings) per column and group. */
    private GroupsMemberCounterPerColumn m_memberCounter;

    /** Counter storing for each missing group the number of members per column. */
    private GroupsMemberCounterPerColumn m_missingGroupsCounter;

    /**
     * Builder of the OutlierReviser.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    public static final class Builder {

        // Optional parameters
        /** The outlier treatment option. */
        private OutlierTreatmentOption m_treatment = OutlierTreatmentOption.REPLACE;

        /** The outlier replacement strategy. */
        private OutlierReplacementStrategy m_repStrategy = OutlierReplacementStrategy.INTERVAL_BOUNDARY;

        /** Tells whether the domains of the outlier columns have to be update after the computation or not. */
        private boolean m_updateDomain = false;

        /**
         * Constructort.
         *
         */
        public Builder() {
        }

        /**
         * Defines how outlier have to be treated, see {@link OutlierTreatmentOption}.
         *
         * @param treatment the treatment option to be used
         * @return the builder itself
         */
        public Builder setTreatmentOption(final OutlierTreatmentOption treatment) {
            m_treatment = treatment;
            return this;
        }

        /**
         * Defines the outlier replacement strategy, see {@link OutlierReplacementStrategy}.
         *
         * @param repStrategy the replacement strategy
         * @return the builder itself
         */
        public Builder setReplacementStrategy(final OutlierReplacementStrategy repStrategy) {
            m_repStrategy = repStrategy;
            return this;
        }

        /**
         * Sets the domain policy flag.
         *
         * @param resetDomain the domain policy
         * @return the builder itself
         */
        public Builder updateDomain(final boolean resetDomain) {
            m_updateDomain = resetDomain;
            return this;
        }

        /**
         * Constructs the outlier reviser using the settings provided by the builder.
         *
         * @return the outlier reviser using the settings provided by the builder
         */
        public OutlierReviser build() {
            return new OutlierReviser(this);
        }
    }

    /**
     * Constructor.
     *
     * @param b the builder
     */
    private OutlierReviser(final Builder b) {
        m_treatment = b.m_treatment;
        m_repStrategy = b.m_repStrategy;
        m_updateDomain = b.m_updateDomain;
        m_listeners = new LinkedList<WarningListener>();
    }

    /**
     * Adds the given listener.
     *
     * @param listener the listener to add
     */
    public void addListener(final WarningListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * Returns the outlier free data table.
     *
     * @return the outlier free data table
     */
    public BufferedDataTable getOutTable() {
        return m_outTable;
    }

    /**
     * Returns the spec of the outlier free data table.
     *
     * @param inSpec the spec of the input data table
     * @return the spec of the outlier free data table
     */
    public DataTableSpec getOutTableSpec(final DataTableSpec inSpec) {
        return inSpec;
    }

    /**
     * Returns the data table storing the permitted intervals and additional information about member counts.
     *
     * @return the data table storing the summary
     */
    public BufferedDataTable getSummaryTable() {
        return m_summaryTable;
    }

    /**
     * Returns the spec of the table storing the permitted intervals and additional information about member counts.
     *
     * @param inSpec the in spec
     * @param groupColNames the group column names
     *
     * @return the spec of the data table storing the summary
     */
    public static DataTableSpec getSummaryTableSpec(final DataTableSpec inSpec, final String[] groupColNames) {
        return OutlierSummaryTable.getExtendedModelSpec(inSpec, groupColNames);
    }

    /**
     * Returns the outlier treatment option.
     *
     * @return the outlier treatment option
     */
    OutlierTreatmentOption getTreatmentOption() {
        return m_treatment;
    }

    /**
     * Returns the outlier replacement strategy.
     *
     * @return the outlier replacement strategy
     */
    OutlierReplacementStrategy getReplacementStrategy() {
        return m_repStrategy;
    }

    /**
     * Returns the update domain flag.
     *
     * @return the update domain flag
     */
    boolean updateDomain() {
        return m_updateDomain;
    }

    /**
     * Clears the input data table of its outliers according to the defined outlier treatment settings.
     *
     * <p>
     * Given that outliers have to be replaced, each of the cells containing an outlier is either replaced by an missing
     * value or set to value of the closest value within the permitted interval. Otherwise all rows containing an
     * outlier are removed from the input data table.
     * </p>
     *
     * @param exec the execution context
     * @param in the data table whose outliers have to be treated
     * @param outlierModel the model storing the permitted intervals
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    public void treatOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final OutlierModel outlierModel) throws CanceledExecutionException {
        // start the treatment step
        exec.setMessage(TREATMENT_MSG);

        // set the outlier column names
        m_outlierColNames = outlierModel.getOutlierColNames();

        // counters for the number of non-missing values and outliers contained in each outlier column respective
        // the different groups
        m_outlierRepCounter = new GroupsMemberCounterPerColumn();
        m_memberCounter = new GroupsMemberCounterPerColumn();
        m_missingGroupsCounter = new GroupsMemberCounterPerColumn();

        // the domains updater
        final OutlierDomainsUpdater domainsUpdater = new OutlierDomainsUpdater();

        // treat the outliers with respect to the selected treatment option
        if (m_treatment == OutlierTreatmentOption.REPLACE) {
            // replaces outliers according to the set replacement strategy
            replaceOutliers(exec.createSubExecutionContext(.9), in, outlierModel, domainsUpdater);
        } else {
            // we remove all columns containing at least one outlier
            removeRows(exec.createSubExecutionContext(.9), in, outlierModel, domainsUpdater);
        }

        // reset the domain
        if (m_updateDomain) {
            m_outTable = domainsUpdater.updateDomain(exec, m_outTable);
        }

        // set the summary table
        m_summaryTable = OutlierSummaryTable.getExtendedModel(exec.createSubExecutionContext(0.1), outlierModel,
            m_memberCounter, m_outlierRepCounter, m_missingGroupsCounter);

        // reset the counters
        m_memberCounter = null;
        m_outlierRepCounter = null;
        m_missingGroupsCounter = null;
        m_outlierColNames = null;
    }

    /**
     * Replaces outliers found in the input table according to the selected replacement option. Additionally, the
     * outlier replacement counts and new domains are calculated.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param outlierModel the model storing the permitted intervals
     * @param domainsUpdater the domains updater
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void replaceOutliers(final ExecutionContext exec, final BufferedDataTable in,
        final OutlierModel outlierModel, final OutlierDomainsUpdater domainsUpdater)
        throws CanceledExecutionException {
        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // the in table spec
        final DataTableSpec inSpec = in.getDataTableSpec();

        // create column re-arranger to overwrite cells corresponding to outliers
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);

        // store the positions where the outlier columns names can be found in the input table
        final int[] outlierIndices = calculateOutlierIndicies(inSpec);

        final DataColumnSpec[] outlierSpecs = new DataColumnSpec[noOutliers];
        for (int i = 0; i < noOutliers; i++) {
            outlierSpecs[i] = inSpec.getColumnSpec(outlierIndices[i]);
        }
        // values are copied anyways by the re-arranger so there is no need to
        // create new instances for each row
        final DataCell[] treatedVals = new DataCell[noOutliers];

        final long rowCount = in.size();
        final double divisor = rowCount;

        final AbstractCellFactory fac = new AbstractCellFactory(outlierSpecs) {

            int m_curIter = 0;

            @Override
            public DataCell[] getCells(final DataRow row) {
                final int iter = ++m_curIter;
                exec.setProgress(m_curIter / divisor, () -> "treating outliers in row " + iter + " of " + rowCount);
                final GroupKey key = outlierModel.getKey(row, inSpec);
                final Map<String, double[]> colsMap = outlierModel.getGroupIntervals(key);
                for (int i = 0; i < noOutliers; i++) {
                    final DataCell curCell = row.getCell(outlierIndices[i]);
                    final DataCell treatedCell;
                    final String outlierColName = m_outlierColNames[i];
                    if (!curCell.isMissing()) {
                        // if the key exists treat the value otherwise we process an unkown group
                        if (colsMap != null) {
                            // increment the member counter
                            m_memberCounter.incrementMemberCount(outlierColName, key);
                            // treat the value of the cell if its a outlier
                            treatedCell = treatCellValue(colsMap.get(outlierColName), curCell);
                        } else {
                            m_missingGroupsCounter.incrementMemberCount(outlierColName, key);
                            treatedCell = curCell;
                        }
                    } else {
                        treatedCell = curCell;
                    }
                    // if we changed the value this is an outlier
                    if (!treatedCell.equals(curCell)) {
                        m_outlierRepCounter.incrementMemberCount(outlierColName, key);
                    }
                    // update the domain if necessary
                    if (m_updateDomain && !treatedCell.isMissing()) {
                        domainsUpdater.updateDomain(outlierColName, ((DoubleValue)treatedCell).getDoubleValue());
                    }
                    treatedVals[i] = treatedCell;
                }
                return treatedVals;
            }

        };
        // replace the outlier columns by their updated versions
        colRearranger.replace(fac, outlierIndices);
        m_outTable = exec.createColumnRearrangeTable(in, colRearranger, exec);
        exec.setProgress(1);
    }

    /**
     * Calculates the positions where the outlier columns can be found in the input table.
     *
     * @param inSpec the input table spec
     * @return the positions of the outlier columns w.r.t. the input spec
     */
    private int[] calculateOutlierIndicies(final DataTableSpec inSpec) {
        final int[] outlierIndices = new int[m_outlierColNames.length];
        for (int i = 0; i < m_outlierColNames.length; i++) {
            outlierIndices[i] = inSpec.findColumnIndex(m_outlierColNames[i]);
        }
        return outlierIndices;
    }

    /**
     * If necessary the value/type of the data cell is modified in accordance with the selected outlier replacement
     * strategy.
     *
     * @param interval the permitted interval
     * @param cell the the current data cell
     * @return the new data cell after replacing its value if necessary
     */
    private DataCell treatCellValue(final double[] interval, final DataCell cell) {
        // while the model hasn't saw only missing values for this group, the applier might contain non-missing entries
        // for this group, hence we need to check for this
        //TODO add warning?
        if (interval == null) {
            return cell;
        }
        double val = ((DoubleValue)cell).getDoubleValue();
        // checks if the value is an outlier
        if (m_repStrategy == OutlierReplacementStrategy.MISSING && (val < interval[0] || val > interval[1])) {
            return DataType.getMissingCell();
        }
        if (cell.getType() == DoubleCell.TYPE) {
            // sets to the lower interval bound if necessary
            val = Math.max(val, interval[0]);
            // sets to the higher interval bound if necessary
            val = Math.min(val, interval[1]);
            return DoubleCellFactory.create(val);
        } else {
            // sets to the lower interval bound if necessary
            // to the smallest integer inside the permitted interval
            val = Math.max(val, Math.ceil(interval[0]));
            // sets to the higher interval bound if necessary
            // to the largest integer inside the permitted interval
            val = Math.min(val, Math.floor(interval[1]));
            // return the proper DataCell
            if (cell.getType() == LongCell.TYPE) {
                return LongCellFactory.create((long)val);
            }
            return IntCellFactory.create((int)val);
        }
    }

    /**
     * Removes rows from the input table that contain outliers. Additionally, the outlier and group related counts, and
     * the new domains are calculated.
     *
     * @param exec the execution context
     * @param in the input data table
     * @param permIntervalsModel the model storing the permitted intervals
     * @param domainsUpdater the domains updater
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    private void removeRows(final ExecutionContext exec, final BufferedDataTable in,
        final OutlierModel permIntervalsModel, final OutlierDomainsUpdater domainsUpdater)
        throws CanceledExecutionException {
        // the in spec
        final DataTableSpec inSpec = in.getDataTableSpec();

        // store the positions where the outlier columns names can be found in the input table
        final int[] outlierIndices = calculateOutlierIndicies(inSpec);

        // total number of outlier columns
        final int noOutliers = m_outlierColNames.length;

        // the outlier free data table container
        BufferedDataContainer container = exec.createDataContainer(inSpec);

        final long rowCount = in.size();
        final double divisor = rowCount;
        long rowCounter = 1;

        // for each row test if it contains an outlier
        for (final DataRow row : in) {
            exec.checkCanceled();
            final long rowCounterLong = rowCounter++; // 'final' due to access in lambda expression
            exec.setProgress(rowCounterLong / divisor,
                () -> "Testing row " + rowCounterLong + " of " + rowCount + " for outliers");

            // get the group key of the currently processed row
            final GroupKey key = permIntervalsModel.getKey(row, inSpec);
            //get the map holding the permitted intervals for the given groups key
            Map<String, double[]> colsMap = permIntervalsModel.getGroupIntervals(key);
            boolean toInsert = true;
            for (int i = 0; i < noOutliers; i++) {
                final DataCell cell = row.getCell(outlierIndices[i]);
                final String outlierColName = m_outlierColNames[i];
                // if the key is existent check the rows, otherwise increment the missing group counters
                if (colsMap != null) {
                    final double[] interval = colsMap.get(outlierColName);
                    if (!cell.isMissing()) {
                        // increment the member counter
                        m_memberCounter.incrementMemberCount(outlierColName, key);
                        final double val = ((DoubleValue)cell).getDoubleValue();
                        // the model might not have learned anything about this key - outlier column combination
                        //TODO add warning?
                        if (interval != null && (val < interval[0] || val > interval[1])) {
                            toInsert = false;
                            // increment the outlier counter
                            m_outlierRepCounter.incrementMemberCount(outlierColName, key);
                        }
                    }
                } else {
                    if (!cell.isMissing()) {
                        m_missingGroupsCounter.incrementMemberCount(outlierColName, key);
                    }
                }
            }
            if (toInsert) {
                container.addRowToTable(row);
                // update the domain if necessary
                if (m_updateDomain) {
                    DataCell cell;
                    for (int i = 0; i < noOutliers; i++) {
                        if (!(cell = row.getCell(outlierIndices[i])).isMissing()) {
                            domainsUpdater.updateDomain(m_outlierColNames[i], ((DoubleValue)cell).getDoubleValue());
                        }
                    }
                }
            }
        }
        container.close();
        m_outTable = container.getTable();
        if (m_outTable.size() == 0) {
            // NodeModel#executeModel only sets the empty table warning if no other warnings were set before
            warnListeners(EMPTY_TABLE_WARNING);
        }
    }

    /**
     * Informs the listeners that a problem occured.
     *
     * @param msg the warning message
     */
    private void warnListeners(final String msg) {
        final Warning warning = new Warning(msg);
        // warn all listeners
        m_listeners.forEach(l -> l.warning(warning));
    }

    /**
     * Class wrapping the functionality to update domain bounds
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private final class OutlierDomainsUpdater {

        /** Map containing the min and max values for the column domains to update. */
        private final Map<String, double[]> m_domainsMap;

        private OutlierDomainsUpdater() {
            m_domainsMap = new HashMap<String, double[]>();
        }

        /**
         * Update the domain for the given columns.
         *
         * @param exec the execution context
         * @param data the data table whose domains have to be reseted
         * @return the data table after reseting the domains
         */
        private BufferedDataTable updateDomain(final ExecutionContext exec, final BufferedDataTable data) {
            DataTableSpec spec = data.getSpec();
            final DataColumnSpec[] domainSpecs = new DataColumnSpec[spec.getNumColumns()];
            for (int i = 0; i < spec.getNumColumns(); i++) {
                final DataColumnSpec columnSpec = spec.getColumnSpec(i);
                if (m_domainsMap.containsKey(columnSpec.getName())) {
                    domainSpecs[i] = updateDomainSpec(columnSpec, m_domainsMap.get(columnSpec.getName()));
                } else {
                    domainSpecs[i] = columnSpec;
                }
            }
            return exec.createSpecReplacerTable(data, new DataTableSpec(spec.getName(), domainSpecs));
        }

        /**
         * Updates the domain of the input spec.
         *
         * @param inSpec the spec to be updated
         * @param domainVals the min and max value of the input spec column
         * @return the updated spec
         */
        private DataColumnSpec updateDomainSpec(final DataColumnSpec inSpec, final double[] domainVals) {
            DataColumnSpecCreator specCreator = new DataColumnSpecCreator(inSpec);
            DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(inSpec.getDomain());
            DataCell[] domainBounds = createBoundCells(inSpec.getType(), domainVals[0], domainVals[1]);
            domainCreator.setLowerBound(domainBounds[0]);
            domainCreator.setUpperBound(domainBounds[1]);
            specCreator.setDomain(domainCreator.createDomain());
            return specCreator.createSpec();
        }

        /**
         * Creates two data cells of the proper type holding storing the given domain.
         *
         * @param type the type of the cell to create
         * @param lowerBound the lower bound of the domain
         * @param upperBound the upper bound of the domain
         * @return cells of the proper storing the given value
         */
        private DataCell[] createBoundCells(final DataType type, final double lowerBound, final double upperBound) {
            if (type == DoubleCell.TYPE) {
                return new DataCell[]{DoubleCellFactory.create(lowerBound), DoubleCellFactory.create(upperBound)};
            }
            // for int and long type use floor of the lower bound and ceil of the upper bound
            if (type == LongCell.TYPE) {
                return new DataCell[]{LongCellFactory.create((long)Math.floor(lowerBound)),
                    LongCellFactory.create((long)Math.ceil(upperBound))};
            }
            // it must be a int cell
            return new DataCell[]{IntCellFactory.create((int)Math.floor(lowerBound)),
                IntCellFactory.create((int)Math.ceil(upperBound))};
        }

        /**
         * Updates the domain for the respective column.
         *
         * @param colName the outlier column name
         * @param val the value
         */
        private void updateDomain(final String colName, final double val) {
            if (!m_domainsMap.containsKey(colName)) {
                m_domainsMap.put(colName, new double[]{val, val});
            }
            final double[] domainVals = m_domainsMap.get(colName);
            domainVals[0] = Math.min(domainVals[0], val);
            domainVals[1] = Math.max(domainVals[1], val);
        }
    }
}