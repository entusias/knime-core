/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   May 19, 2006 (wiswedel): created
 */
package de.unikn.knime.base.node.io.table.read;

import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeView;

/**
 * Factory for table that reads the file as written from the
 * Write table node.
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeFactory extends NodeFactory {

    /**
     * @see NodeFactory#createNodeModel()
     */
    public @Override NodeModel createNodeModel() {
        return new ReadTableNodeModel();
    }

    /**
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * @see NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(
            final int viewIndex, final NodeModel nodeModel) {
        throw new IndexOutOfBoundsException("No view.");
    }

    /**
     * @see NodeFactory#hasDialog()
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    /**
     * @see NodeFactory#createNodeDialogPane()
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ReadTableNodeDialogPane();
    }

}
