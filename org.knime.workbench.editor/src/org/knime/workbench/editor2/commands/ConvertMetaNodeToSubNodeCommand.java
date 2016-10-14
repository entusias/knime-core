/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created on Oct 4, 2013 by Berthold
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CastUtil;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.action.MetaNodeToSubNodeResult;

/**
 * Command to wrap a metanode into a subnode/wrappednode.
 * @author M. Berthold
 */
public class ConvertMetaNodeToSubNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ConvertMetaNodeToSubNodeCommand.class);

    private final NodeID m_id;
    private MetaNodeToSubNodeResult m_metaNodeToSubNodeResult;

    /**
     * @param wfm the workflow manager holding the metanode
     * @param id of node to be converted.
     */
    public ConvertMetaNodeToSubNodeCommand(final IWorkflowManager wfm, final NodeID id) {
        super(wfm);
        m_id = id;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        return getHostWFM().canRemoveNode(m_id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            m_metaNodeToSubNodeResult = CastUtil.castWFM(getHostWFM()).convertMetaNodeToSubNode(m_id);
        } catch (Exception e) {
            String error = "Converting Metanode failed: " + e.getMessage();
            LOGGER.error(error, e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(), "Convert failed", error);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_metaNodeToSubNodeResult != null && m_metaNodeToSubNodeResult.canUndo();
    }

    /** {@inheritDoc} */
    @Override
    public void undo() {
        m_metaNodeToSubNodeResult.undo();
        m_metaNodeToSubNodeResult = null;
    }

}
