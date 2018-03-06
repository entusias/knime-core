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
 *   Mar 4, 2018 (loki): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This provides the 'action' for connecting two or more nodes.
 */
public class LinkNodesAction extends AbstractNodeAction {

    /** org.eclipse.ui.commands command ID for this action. **/
    static public final String ID = "knime.commands.linknodes";

    static private final NodeLogger LOGGER = NodeLogger.getLogger(LinkNodesAction.class);


    /**
     * @param editor The workflow editor which this action will work within
     */
    public LinkNodesAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/link_nodes.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        // TODO Auto-generated method stub

    }

    /**
     * @return <code>true</code> if we have:
     *                  • at least two nodes which do not have the same left spatial location
     *                  • that the left most node has at least 1 outport
     *                  • that the right most node has at least 1 inport
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] selected = this.getSelectedParts(NodeContainerEditPart.class);

        if (selected.length < 2) {
            return false;
        }

        ScreenedSelectionSet sss = this.screenNodeSelection(selected);

        return (sss.setIsConnectable());
    }

    /**
     *
     * @param nodes
     * @return This returns a collection of PlannedConnection instances which can then be "executed" to
     *                  form the resulting connected subset of nodes. The ordering of the connection
     *                  events is unimportant and so is returned as a Collection to emphasize that.
     */
    protected Collection<PlannedConnection> generateConnections(final NodeContainerEditPart[] nodes) {
        ArrayList<PlannedConnection> rhett = new ArrayList<>();
        ScreenedSelectionSet sss = this.screenNodeSelection(nodes);

        if (!sss.setIsConnectable()) {
            return rhett;
        }


        /*
         * Take first node from sss, get spatially next that:
         *
         *      . has an inport
         *      . is not spatially overlapping
         *      . is not connected to the outport of another node in this set already (not considering the
         *              plan of connections to be made due to this action)
         *
         *
         * This doesn't embody the "on the same y-line" connection logic. (TODO)
         */
        List<NodeContainerUI> orderedNodes = sss.getConnectableNodes();
        NodeContainerUI sourceNode;
        NodeContainerUI destinationNode;
        int currentIndex;
        for (int i = 0; i < (orderedNodes.size() - 1); i++) {
            currentIndex = i + 1;

            sourceNode = orderedNodes.get(i);
            if (sourceNode.getNrOutPorts() > 0) {
                int sourceOutPort = this.getUseablePort(sourceNode, false, rhett);

                if (sourceOutPort == -1) {
                    LOGGER.error("Node " + sourceNode.getNameWithID() + " has no available outports.");

                    rhett.clear();

                    return rhett;
                }

                while (currentIndex < orderedNodes.size()) {
                    destinationNode = orderedNodes.get(currentIndex);

                    if ((destinationNode.getNrInPorts() > 0)
                                        && (!this.nodesOverlapInXDomain(sourceNode, destinationNode)
                                        && (!this.nodeHasConnectionWithinSet(destinationNode,
                                                                             orderedNodes)))) {
                        int destinationInPort = this.getUseablePort(destinationNode, true, rhett);
                        PlannedConnection pc;

                        if (destinationInPort == -1) {
                            LOGGER.error("Node " + destinationNode.getNameWithID()
                                                 + " has no available inports.");

                            rhett.clear();

                            return rhett;
                        }

                        pc = new PlannedConnection(sourceNode, sourceOutPort,
                                                   destinationNode, destinationInPort);

                        rhett.add(pc);

                        break;
                    }

                    currentIndex++;
                }
            }
        }

        /*
         * Now, find any with inports that are not connected in the plan and that are not the first node;
         *  connect to the first previous node which has an outport.
         */

        return rhett;
    }

    /**
     * @param node
     * @param inport if true, then inports will be referenced and outports if false
     * @param existingPlan the already existing planned connections
     * @return an int representing the port to use; the priority is the first (in natural ordering) unused
     *              and unplanned port; if no such port exists, then the first unplanned port; if still no
     *              such port exists, -1.
     */
    protected int getUseablePort(final NodeContainerUI node, final boolean inport,
                                 final List<PlannedConnection> existingPlan) {
        WorkflowManager wm = this.getManager();
        Set<ConnectionContainer> existingConnections = inport ? wm.getIncomingConnectionsFor(node.getID())
                                                              : wm.getOutgoingConnectionsFor(node.getID());
        int portCount = inport ? node.getNrInPorts() : node.getNrOutPorts();
        int port = 0;
        boolean exists;

        while (port < portCount) {
            exists = false;
            for (ConnectionContainer cc : existingConnections) {
                if ((inport && (cc.getDestPort() == port))
                            || ((!inport) && (cc.getSourcePort() == port))) {
                    exists = true;

                    break;
                }
            }

            if (!exists) {
                for (PlannedConnection pc : existingPlan) {
                    if ((inport && (pc.getDestinationInportIndex() == port))
                                || ((!inport) && (pc.getSourceOutportIndex() == port))) {
                        exists = true;

                        break;
                    }
                }

                if (!exists) {
                    return port;
                }
            }

            port++;
        }

        // Ok - if we're still here, all free ports in the existing (pre-new-link-command-being-executed)
        //      are already spoken for in the plan, so now just grab the first one that is not spoken
        //      for in the new plan.
        //
        // TODO in this case we need to also include a detach event in the plan for a pre-existing
        //          connection
        port = 0;
        while (port < portCount) {
            exists = false;
            for (PlannedConnection pc : existingPlan) {
                if ((inport && (pc.getDestinationInportIndex() == port))
                            || ((!inport) && (pc.getSourceOutportIndex() == port))) {
                    exists = true;

                    break;
                }
            }

            if (!exists) {
                return port;
            }

            port++;
        }

        return -1;
    }

    /**
     * @param node1
     * @param node2
     * @return true is the two nodes overlap in the x-domain
     */
    protected boolean nodesOverlapInXDomain(final NodeContainerUI node1, final NodeContainerUI node2) {
        NodeUIInformation ui1 = node1.getUIInformation();
        NodeUIInformation ui2 = node2.getUIInformation();
        int[] bounds1 = ui1.getBounds();
        int[] bounds2 = ui2.getBounds();
        int node1x2 = bounds1[0] + bounds1[2];
        int node2x2 = bounds2[0] + bounds2[2];

        return (((bounds1[0] >= bounds2[0]) && (bounds2[0] <= node1x2))
                    || ((bounds2[0] >= bounds1[0]) && (bounds1[0] <= node2x2)));
    }

    /**
     * @param node
     * @param set
     * @return true if the specified node already has an inport connected to the outport of a node in the set
     */
    protected boolean nodeHasConnectionWithinSet(final NodeContainerUI node,
                                                 final Collection<NodeContainerUI> set) {
        WorkflowManager wm = this.getManager();
        Set<ConnectionContainer> incoming = wm.getIncomingConnectionsFor(node.getID());

        if (incoming.size() > 0) {
            NodeID nid;

            for (ConnectionContainer cc : incoming) {
                nid = cc.getSource();

                for (NodeContainerUI setItem : set) {
                    if (nid.equals(setItem.getID())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * This performs the first pass screening which consists of determining the spatially leftmost and
     *  rightmost nodes in the selection, that are valid, and discards any invalid nodes (nodes which
     *  are spatially leftmost but have only inports or are spatially rightmost but have only outports.)
     *
     * @param nodes the current selection set of nodes in the workflow editor
     * @return an instance of ScreenedSelectionSet
     */
    protected ScreenedSelectionSet screenNodeSelection(final NodeContainerEditPart[] nodes) {
        NodeContainerUI[] spatialBounds = new NodeContainerUI[2];
        NodeContainerUI node;
        NodeUIInformation uiInfo;
        NodeUIInformation uiInfoToo;
        boolean canBeLeftMost;
        boolean canBeRightMost;

        for (int i = 0; i < nodes.length; i++) {
            node = nodes[i].getNodeContainer();

            canBeLeftMost = (node.getNrOutPorts() > 0);
            canBeRightMost = (node.getNrInPorts() > 0);

            uiInfo = node.getUIInformation();
            if ((uiInfo == null) || (uiInfo.getBounds() == null)) {
                continue;
            }

            if ((spatialBounds[0] == null) && canBeLeftMost) {
                if (spatialBounds[1] != null) {
                    uiInfoToo = spatialBounds[1].getUIInformation();
                    if (uiInfoToo.getBounds()[0] < uiInfo.getBounds()[0]) {
                        spatialBounds[0] = spatialBounds[1];
                        spatialBounds[1] = node;
                    }
                    else {
                        spatialBounds[0] = node;
                    }
                }
                else {
                    spatialBounds[0] = node;
                }
            }
            else if ((spatialBounds[1] == null) && canBeRightMost) {
                if (spatialBounds[0] != null) {
                    uiInfoToo = spatialBounds[0].getUIInformation();
                    if (uiInfoToo.getBounds()[0] > uiInfo.getBounds()[0]) {
                        spatialBounds[1] = spatialBounds[0];
                        spatialBounds[0] = node;
                    }
                    else {
                        spatialBounds[1] = node;
                    }
                }
                else {
                    spatialBounds[1] = node;
                }
            }
            else if ((spatialBounds[0] != null) && (spatialBounds[1] != null)) {
                if (canBeLeftMost) {
                    uiInfoToo = spatialBounds[0].getUIInformation();
                    if (uiInfoToo.getBounds()[0] > uiInfo.getBounds()[0]) {
                        spatialBounds[0] = node;
                    }
                }

                if ((spatialBounds[0] != node) && canBeRightMost) {
                    uiInfoToo = spatialBounds[1].getUIInformation();
                    if (uiInfoToo.getBounds()[0] < uiInfo.getBounds()[0]) {
                        spatialBounds[1] = node;
                    }
                }
            }
        }

        ArrayList<NodeContainerUI> connectableNodes = new ArrayList<>();
        if ((spatialBounds[0] == null) || (spatialBounds[1] == null)) {
            return new ScreenedSelectionSet(connectableNodes, spatialBounds[0], spatialBounds[1]);
        }

        int leftBound = spatialBounds[0].getUIInformation().getBounds()[0];
        int rightBound = spatialBounds[1].getUIInformation().getBounds()[0];
        boolean discard;
        for (int i = 0; i < nodes.length; i++) {
            node = nodes[i].getNodeContainer();
            uiInfo = node.getUIInformation();
            if ((uiInfo == null) || (uiInfo.getBounds() == null)) {
                continue;
            }

            if ((uiInfo.getBounds()[0] >= leftBound) && (uiInfo.getBounds()[0] <= rightBound)) {
                discard = false;

                if (uiInfo.getBounds()[0] == leftBound) {
                   if (node.getNrOutPorts() == 0) {
                       discard = true;
                   }
                }
                else if (uiInfo.getBounds()[0] == rightBound) {
                    if (node.getNrInPorts() == 0) {
                        discard = true;
                    }
                }

                if (!discard) {
                    connectableNodes.add(node);
                }
            }
        }

        return new ScreenedSelectionSet(connectableNodes, spatialBounds[0], spatialBounds[1]);
    }


    static private class ScreenedSelectionSet {

        private List<NodeContainerUI> connectableNodes;
        private NodeContainerUI spatiallyLeftMostNode;
        private NodeContainerUI spatiallyRightMostNode;

        private ScreenedSelectionSet(final Collection<NodeContainerUI> connectables,
                                     final NodeContainerUI left, final NodeContainerUI right) {
            this.connectableNodes = new ArrayList<>(connectables);
            this.spatiallyLeftMostNode = left;
            this.spatiallyRightMostNode = right;

            Collections.sort(this.connectableNodes, new NodeSpatialComparator());
        }

        private boolean setIsConnectable() {
            return ((this.spatiallyLeftMostNode != null)
                        && (this.spatiallyRightMostNode != null)
                        && (this.connectableNodes.size() > 0));
        }

        /**
         * @return a spatially in-order list of connectable nodes
         * @see NodeSpatialComparator
         */
        private List<NodeContainerUI> getConnectableNodes() {
            return this.connectableNodes;
        }

        private NodeContainerUI getSpatiallyLeftMostNode() {
            return this.spatiallyLeftMostNode;
        }

        private NodeContainerUI getSpatiallyRightMostNode() {
            return this.spatiallyRightMostNode;
        }

    }


    static private class PlannedConnection {

        private NodeContainerUI sourceNode;
        private int sourceOutportIndex;

        private NodeContainerUI destinationNode;
        private int destinationInportIndex;

        private PlannedConnection(final NodeContainerUI source, final int sourcePort,
                                  final NodeContainerUI destination, final int destinationPort) {
            this.sourceNode = source;
            this.sourceOutportIndex = sourcePort;

            this.destinationNode = destination;
            this.destinationInportIndex = destinationPort;
        }

        /**
         * @return the sourceNode
         */
        private NodeContainerUI getSourceNode() {
            return sourceNode;
        }
        /**
         * @return the sourceOutportIndex
         */
        private int getSourceOutportIndex() {
            return sourceOutportIndex;
        }
        /**
         * @return the destinationNode
         */
        private NodeContainerUI getDestinationNode() {
            return destinationNode;
        }
        /**
         * @return the destinationInportIndex
         */
        private int getDestinationInportIndex() {
            return destinationInportIndex;
        }



    }


    /**
     * This orders ascending first by x-coordinate and second by y-coordinate.
     */
    static private class NodeSpatialComparator
            implements Comparator<NodeContainerUI> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final NodeContainerUI node1, final NodeContainerUI node2) {
            NodeUIInformation ui1 = node1.getUIInformation();
            NodeUIInformation ui2 = node2.getUIInformation();
            int[] bounds1 = ui1.getBounds();
            int[] bounds2 = ui2.getBounds();

            if (bounds1[0] != bounds2[0]) {
                return bounds1[0] - bounds2[0];
            }

            return bounds1[1] - bounds2[1];
        }

    }

}
