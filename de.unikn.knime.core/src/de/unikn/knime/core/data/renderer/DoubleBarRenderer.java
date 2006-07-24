/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   01.02.2006 (cebron): created
 */
package de.unikn.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;

import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.def.DoubleCell;

/**
 * Renderer for double cells that paints the whole range in a bar according
 * to the cell's value. It uses the domain information from the column spec to
 * determine min and max value and to find the appropriate gray value. If no
 * domain information is available, 0.0 and 1.0 are assumed to define the range.
 * @author Bernd Wiswedel, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 */
public class DoubleBarRenderer extends DefaultDataValueRenderer {
    
    /** Creates new instance given a column spec. This object will get the
     * information about min/max from the spec and do the normalization 
     * accordingly.
     * @param spec The spec from which to get min/max. May be null in which 
     *         case 0.0 and 1.0 are assumed.
     */
    public DoubleBarRenderer(final DataColumnSpec spec) {
        super(spec);
        setIcon(new BarIcon());
        setIconTextGap(0);
    }
    
    /** Overridden to ignore any invocation.
     * @see javax.swing.JLabel#setText(java.lang.String)
     */
    @Override
    public void setText(final String text) {
    }
    
    /** Sets the value for the icon.
     * @param d the value to be used.
     */
    public void setIconValue(final double d) {
        Icon icon = getIcon();
        if (icon instanceof BarIcon) {
            ((BarIcon)icon).setValue(d);
        }
    }
    
    /**
    /** Sets the value according to the column domain's min/max. If the 
     * object is not instance of DoubleValue, the cell is painted red.
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        double d = 0;
        if (value instanceof DoubleValue) {
            DoubleValue cell = (DoubleValue)value;
            double val = cell.getDoubleValue();
            DataColumnSpec spec = getColSpec();
            boolean takeValuesFrom = spec != null;
            takeValuesFrom &= spec.getDomain().hasLowerBound();
            takeValuesFrom &= spec.getDomain().hasUpperBound();
            takeValuesFrom &= DoubleCell.TYPE.isASuperTypeOf(spec
                    .getType());
            double min;
            double max;
            if (takeValuesFrom) {
                DataColumnDomain domain = spec.getDomain();
                min = ((DoubleValue)domain.getLowerBound()).getDoubleValue();
                max = ((DoubleValue)domain.getUpperBound()).getDoubleValue();
            } else {
                min = Double.POSITIVE_INFINITY;
                max = Double.NEGATIVE_INFINITY;
            }
            if (min >= max) {
                min = 0.0;
                max = 1.0;
            }
            d = (float)((val - min) / (max - min));
            setIconValue(d);
        } else {
            setIconValue(Double.NaN);
        }
    }
    
    /** Returns "Gray Scale".
     * @see de.unikn.knime.core.data.renderer.DataValueRenderer#getDescription()
     */
    @Override
    public String getDescription() {
        return "Bars";
    }

    /** Private icon that is shown instead of any string. 
     * The code is mainly copied from javax.swing.plaf.basic.BasicIconFactory 
     * and altered accordingly.
     */
    private class BarIcon implements Icon {

        private double m_value = 0;

        /**
         * @see javax.swing.Icon#getIconHeight()
         */
        public int getIconHeight() {
            return getHeight();
        }
        
        /**
         * @see javax.swing.Icon#getIconWidth()
         */
        public int getIconWidth() {
            return getWidth();
        }
        
        /**
         * Sets the current vale.
         * @param d double value.
         */
        public void setValue(final double d) {
            m_value = d;
        }
        
        /**
         * @see javax.swing.Icon#paintIcon( java.awt.Component,
         *      java.awt.Graphics, int, int)
         */
        public void paintIcon(final Component c, final Graphics g, final int x,
                final int y) {
            int iconWidth = getIconWidth();
            if (Double.isNaN(m_value)) {
                g.setColor(Color.BLACK);
                ((Graphics2D)g).fill(new Rectangle2D.Double(x, y
                        + (getIconHeight() / 4), iconWidth - 4,
                        getIconHeight() / 2));
            } else {
                int width = (int)(m_value * iconWidth);
                GradientPaint redtogreen = new GradientPaint(x, y, Color.red,
                        iconWidth, y, Color.green);
                ((Graphics2D)g).setPaint(redtogreen);
                g.draw3DRect(x, y + (getIconHeight() / 4), width,
                        getIconHeight() / 2, true);
                ((Graphics2D)g).fill(new Rectangle2D.Double(x, y
                        + (getIconHeight() / 4), width, getIconHeight() / 2));
            }
        }
    } // end class BarIcon
}
