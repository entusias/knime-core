/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   21.04.2005 (cebron): created
 */
package org.knime.base.data.normalize;

import java.util.Arrays;

import org.knime.base.data.statistics.StatisticsTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * A wrapper table to normalize all DataRows. Three methods of normalization are
 * available:
 * <ul>
 * <li>Min-Max Normalization</li>
 * <li>Z-Score Normalization</li>
 * <li>Normalization by decimal scaling</li>
 * </ul>
 * <b>Important !</b> Be sure to pull a new
 * {@link org.knime.core.data.DataTableSpec} with
 * {@link #generateNewSpec(DataTableSpec, String[])}, because
 * {@link org.knime.core.data.def.IntCell} columns are converted to
 * {@link org.knime.core.data.def.DoubleCell} columns.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public final class Normalizer {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Normalizer.class);

    /**
     * Table to be wrapped.
     */
    private final DataTable m_table;

    /**
     * Column indices to work on.
     */
    private int[] m_colindices;

    /**
     * Create new wrapper Table from an existing one.
     * 
     * @param table table to be wrapped
     * @param columns to work on
     * @see DataTable#getDataTableSpec()
     */
    public Normalizer(final BufferedDataTable table, final String[] columns) {
        m_table = table;
        DataTableSpec spec = table.getDataTableSpec();
        m_colindices = findNumericalColumns(spec, columns);
    }

    /**
     * Creates a new DataTableSpec. IntCell-columns are converted to
     * DoubleCell-columns.
     * 
     * @param inspec the DataTableSpec of the input table
     * @param columns the columns that are normalized
     * @return DataTableSpec for the output table
     */
    public static final DataTableSpec generateNewSpec(
            final DataTableSpec inspec, final String[] columns) {
        // filters out all non-numerical columns in argument
        int[] colindices = findNumericalColumns(inspec, columns);
        Arrays.sort(colindices); // will make a binary search on it.
        int nrCols = inspec.getNumColumns();
        DataColumnSpec[] colspecs = new DataColumnSpec[nrCols];
        for (int i = 0; i < nrCols; i++) {
            DataColumnSpec colspec = inspec.getColumnSpec(i);
            if (Arrays.binarySearch(colindices, i) >= 0) {
                DataType coltype = colspec.getType();
                // findNumericalColumns makes sure that only double compatible
                // types are included.
                assert coltype.isCompatible(DoubleValue.class);
                DataColumnSpecCreator c = new DataColumnSpecCreator(colspec);
                // must not set domain - that will change anyway.
                c.setDomain(null);
                // int type must be overwritten
                c.setType(DoubleCell.TYPE);
                colspecs[i] = c.createSpec();
            } else {
                colspecs[i] = colspec;
            }
        }
        DataTableSpec out = new DataTableSpec(colspecs);
        return out;
    }

    /**
     * Method that looks into spec and filters all columns given by the second
     * argument AND which are also double compatible.
     * 
     * @param spec the spec to look into
     * @param columns the columns to include
     * @return the valid indices
     */
    private static final int[] findNumericalColumns(final DataTableSpec spec,
            final String[] columns) {
        int[] colindices = new int[columns.length];
        int validCount = 0;
        for (int i = 0; i < columns.length; i++) {
            int index = spec.findColumnIndex(columns[i]);
            if (index < 0) {
                throw new IllegalArgumentException("Column \"" + columns[i]
                        + "\" not contained in data.");
            }
            DataType type = spec.getColumnSpec(index).getType();
            if (!type.isCompatible(DoubleValue.class)) {
                LOGGER.debug("Non-numerical column: \"" + columns[i]
                        + "\", skipping.");
            } else {
                colindices[validCount] = index;
                validCount++;
            }
        }
        int[] result = new int[validCount];
        System.arraycopy(colindices, 0, result, 0, validCount);
        return result;
    }

    /**
     * Does the Min-Max Normalization.
     * 
     * @param newmax the new maximum
     * @param newmin the new minimum
     * @param exec an object to check for user cancelations. Can be
     *            <code>null</code>.
     * @throws CanceledExecutionException if user canceled
     * @return normalized DataTable
     */
    public AffineTransTable doMinMaxNorm(
            final double newmax, final double newmin,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        ExecutionMonitor statisticsExec = exec.createSubProgress(.5);
        StatisticsTable st = new StatisticsTable(m_table, statisticsExec);
        DataTableSpec spec = st.getDataTableSpec();
        DataCell[] max = st.getMax();
        DataCell[] min = st.getMin();
        final double[] scales = new double[m_colindices.length];
        final double[] transforms = new double[m_colindices.length];

        for (int i = 0; i < transforms.length; i++) {
            DataColumnSpec cSpec = spec.getColumnSpec(m_colindices[i]);
            boolean isDouble = cSpec.getType().isCompatible(DoubleValue.class);
            if (!isDouble || max[m_colindices[i]].isMissing()) {
                assert (!isDouble || min[m_colindices[i]].isMissing());
                scales[i] = Double.NaN;
                transforms[i] = Double.NaN;
            } else {
                // scales and translation to [0,1]
                double maxI = ((DoubleValue)max[m_colindices[i]])
                        .getDoubleValue();
                double minI = ((DoubleValue)min[m_colindices[i]])
                        .getDoubleValue();
                scales[i] = (maxI == minI ? 1 : 1.0 / (maxI - minI));
                transforms[i] = -minI * scales[i];
                // scale and translation to [newmin, newmax]
                scales[i] *= (newmax - newmin);
                transforms[i] *= (newmax - newmin);
                transforms[i] += newmin;
            }
        }
        String[] includes = getNames();
        return new AffineTransTable(m_table, includes, scales, transforms);
    }

    /**
     * Does the Z-Score Normalization.
     * 
     * @param exec an object to check for user cancelations. Can be
     *            <code>null</code>.
     * @throws CanceledExecutionException if user canceled
     * @return the normalized DataTable
     */
    public AffineTransTable doZScoreNorm(final ExecutionMonitor exec)
            throws CanceledExecutionException {
        ExecutionMonitor statisticsExec = exec.createSubProgress(.5);
        StatisticsTable st = new StatisticsTable(m_table, statisticsExec);
        double[] mean = st.getMean();
        double[] stddev = st.getStandardDeviation();

        final double[] scales = new double[m_colindices.length];
        final double[] transforms = new double[m_colindices.length];

        for (int i = 0; i < m_colindices.length; i++) {
            if (Double.isNaN(mean[m_colindices[i]])) {
                scales[i] = Double.NaN;
                transforms[i] = Double.NaN;
            } else {
                scales[i] = (stddev[m_colindices[i]] == 0.0 ? 1.0
                        : 1.0 / stddev[m_colindices[i]]);
                transforms[i] = -mean[m_colindices[i]] * scales[i];
            }
        }
        String[] includes = getNames();
        return new AffineTransTable(m_table, includes, scales, transforms);
    }

    /**
     * Does the decimal scaling.
     * 
     * @param exec an object to check for user cancelations. Can be
     *            <code>null</code>.
     * @throws CanceledExecutionException if user canceled
     * @return the normalized DataTable
     */
    public AffineTransTable doDecimalScaling(final ExecutionMonitor exec)
            throws CanceledExecutionException {
        StatisticsTable st = new StatisticsTable(m_table, exec);
        String[] includes = getNames();
        double[] max = st.getdoubleMax();
        double[] min = st.getdoubleMin();
        double[] scales = new double[m_colindices.length];
        double[] transforms = new double[m_colindices.length];
        for (int i = 0; i < m_colindices.length; i++) {
            int trueIndex = m_colindices[i];
            double absMax = Math.abs(max[trueIndex]);
            double absMin = Math.abs(min[trueIndex]);
            double maxvalue = absMax > absMin ? absMax : absMin; 
            int exp = 0;
            while (Math.abs(maxvalue) > 1) {
                maxvalue = maxvalue / 10;
                exp++;
            }
            scales[i] = 1.0 / Math.pow(10, exp);
            transforms[i] = 0.0;
        }
        return new AffineTransTable(m_table, includes, scales, transforms);
    }

    /* Get the names for all included columns. */
    private String[] getNames() {
        int[] cols = m_colindices;
        String[] result = new String[cols.length];
        DataTableSpec spec = m_table.getDataTableSpec();
        for (int i = 0; i < cols.length; i++) {
            result[i] = spec.getColumnSpec(m_colindices[i]).getName();
        }
        return result;
    }
}
