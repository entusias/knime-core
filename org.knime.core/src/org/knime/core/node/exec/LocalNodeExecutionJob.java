/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 17, 2008 (wiswedel): created
 */
package org.knime.core.node.exec;

import java.util.concurrent.Future;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NodeExecutionJob;
import org.knime.core.node.workflow.SingleNodeContainer;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class LocalNodeExecutionJob extends NodeExecutionJob {

    private Future<?> m_future;

    public LocalNodeExecutionJob(final SingleNodeContainer snc,
            final PortObject[] data, final ExecutionContext ec) {
        super(snc, data, ec);
    }


    /** {@inheritDoc} */
    @Override
    public boolean cancel() {
        if (m_future == null) {
            throw new IllegalStateException(
                    "Future that represents the execution has not been set.");
        }
        return m_future.cancel(true);
    }

    /**
     * Set the future that represents the pending execution.
     * @param future the future to set
     */
    void setFuture(final Future<?> future) {
        m_future = future;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mainExecute() {
        return getSingleNodeContainer().performExecuteNode(
                getPortObjects(), getExecutionContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReConnecting() {
        return false;
    }

}
