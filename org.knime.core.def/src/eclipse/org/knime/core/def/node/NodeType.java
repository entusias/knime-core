package org.knime.core.def.node;

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
 * -------------------------------------------------------------------
 *
 */

/**
* Enum for all node types.
*
* @author Michael Berthold, University of Konstanz
* @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
*/
public enum NodeType {

    /** A data producing node. */
    Source,
    /** A data consuming node. */
    Sink,
    /** A learning node. */
    Learner,
    /** A predicting node. */
    Predictor,
    /** A data manipulating node. */
    Manipulator,
    /** A visualizing node. */
    Visualizer,
    /** A metanode. */
    Meta,
    /** Start node of a loop. */
    LoopStart,
    /** End node of a loop. */
    LoopEnd,
    /** Start node of a scope.
     * @since 2.8*/
    ScopeStart,
    /** End node of a scope.
     * @since 2.8*/
    ScopeEnd,
    /** A node contributing to quick/web form. */
    QuickForm,
    /** All other nodes. */
    Other,
    /** A missing node (framework use only).
     * @since 2.7 */
    Missing,
    /** If not specified. */
    Unknown,
    /** @since 2.10 */
    Subnode,
    /** @since 2.10 */
    VirtualIn,
    /** @since 2.10 */
    VirtualOut
}