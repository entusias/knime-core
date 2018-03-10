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
 *   Mar 9, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.figures.WorkflowFigure;

/**
 * This class exists to provide a render hint to the workflow editor user when they are dragging so that
 *      they are made aware that the canvas will auto-scroll.
 */
class DragScrollingHintRenderer implements MouseListener, MouseMoveListener {

    static private final NodeLogger LOGGER = NodeLogger.getLogger(DragScrollingHintRenderer.class);

    static private final float SPATIAL_ZONE_PERCENTAGE = 0.1f;

    static private final Dimension CORNER_DIMENSION = new Dimension(5, 3);

    // Makes for better code readability
    static private final int NORTH = 0;
    static private final int WEST = 1;
    static private final int SOUTH = 2;
    static private final int EAST = 3;


    final private FigureCanvas backingCanvas;
    final private WorkflowFigure rootFigure;

    final private Color fillColor;

    // This will only be consulted from the SWT thread, so i'm content making it neither volatile nor
    //  AtomicBoolean
    private boolean weAreInADrag;

    // Similarly, this will only be interacted with on the SWT thread; the indices represent N,W,S,E.
    private int[] renderSpatialThresholds;
    // Similarly ...; this is used to store the reduced width, 0, or height, 1, of a highlight region.
    private int[] renderDimensions;
    private RoundedRectangle[] currentlyDisplayedRegions;

    /**
     * @param viewer we assume this instance is being created by WorkflowGraphicalViewerCreator
     */
    DragScrollingHintRenderer(final GraphicalViewer viewer, final WorkflowFigure figure) {
        this.backingCanvas = (FigureCanvas)viewer.getControl();

        this.backingCanvas.addMouseListener(this);
        this.backingCanvas.addMouseMoveListener(this);

        this.rootFigure = figure;

        this.fillColor = new Color(this.backingCanvas.getDisplay(), 0, 134, 197);
    }

    // This will always be called on the SWT thread
    private RoundedRectangle makeRoundedRectangle(final Rectangle viewportBounds, final int side) {
        final RoundedRectangle rhett = new RoundedRectangle();

        rhett.setFill(true);
        rhett.setBackgroundColor(this.fillColor);
        rhett.setCornerDimensions(CORNER_DIMENSION);

        switch (side) {
            case NORTH:
            case SOUTH:
                rhett.setPreferredSize(viewportBounds.width, this.renderDimensions[1]);
                break;
            default:
                rhett.setPreferredSize(this.renderDimensions[0], viewportBounds.height);
                break;
        }


        return rhett;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMove(final MouseEvent e) {
        if (this.weAreInADrag) {
            final Rectangle bounds = this.backingCanvas.getViewport().getBounds();
            final boolean renderNorth = (e.y <= (this.renderSpatialThresholds[NORTH] + bounds.y));
            final boolean renderWest = (e.x <= (this.renderSpatialThresholds[WEST] + bounds.x));
            final boolean renderSouth = (e.y >= (this.renderSpatialThresholds[SOUTH] + bounds.y));
            final boolean renderEast = (e.x >= (this.renderSpatialThresholds[EAST] + bounds.x));

            // TODO we need to be a scroll listener as potentially the drag could not change but
            //          a scrolling may go on (hard to tell since the scroll doesn't work for me)


            // TODO there's some abstraction which can be done here probably

            if (renderNorth) {
                if (this.currentlyDisplayedRegions[NORTH] == null) {
                    this.currentlyDisplayedRegions[NORTH] = this.makeRoundedRectangle(bounds, NORTH);

                    LOGGER.info("adding north");

                    this.rootFigure.add(this.currentlyDisplayedRegions[NORTH]);
                }

                this.currentlyDisplayedRegions[NORTH].setLocation(bounds.getLocation());
            }
            else if (this.currentlyDisplayedRegions[NORTH] != null) {
                this.rootFigure.remove(this.currentlyDisplayedRegions[NORTH]);

                this.currentlyDisplayedRegions[NORTH] = null;
            }

            if (renderSouth) {
                int y = bounds.y + bounds.height;

                if (this.currentlyDisplayedRegions[SOUTH] == null) {
                    this.currentlyDisplayedRegions[SOUTH] = this.makeRoundedRectangle(bounds, SOUTH);

                    this.rootFigure.add(this.currentlyDisplayedRegions[SOUTH]);
                }

                y -= this.currentlyDisplayedRegions[SOUTH].getSize().height;

                this.currentlyDisplayedRegions[SOUTH].setLocation(new Point(bounds.x, y));
            }
            else if (this.currentlyDisplayedRegions[SOUTH] != null) {
                this.rootFigure.remove(this.currentlyDisplayedRegions[SOUTH]);

                this.currentlyDisplayedRegions[SOUTH] = null;
            }

            if (renderWest) {
                if (this.currentlyDisplayedRegions[WEST] == null) {
                    this.currentlyDisplayedRegions[WEST] = this.makeRoundedRectangle(bounds, WEST);

                    LOGGER.info("adding west");

                    this.rootFigure.add(this.currentlyDisplayedRegions[WEST]);
                }

                this.currentlyDisplayedRegions[WEST].setLocation(bounds.getLocation());
            }
            else if (this.currentlyDisplayedRegions[WEST] != null) {
                this.rootFigure.remove(this.currentlyDisplayedRegions[WEST]);

                this.currentlyDisplayedRegions[WEST] = null;
            }

            if (renderEast) {
                int x = bounds.x + bounds.width;

                if (this.currentlyDisplayedRegions[EAST] == null) {
                    this.currentlyDisplayedRegions[EAST] = this.makeRoundedRectangle(bounds, EAST);

                    this.rootFigure.add(this.currentlyDisplayedRegions[EAST]);
                }

                x -= this.currentlyDisplayedRegions[EAST].getSize().width;

                this.currentlyDisplayedRegions[EAST].setLocation(new Point(x, bounds.y));
            }
            else if (this.currentlyDisplayedRegions[EAST] != null) {
                this.rootFigure.remove(this.currentlyDisplayedRegions[EAST]);

                this.currentlyDisplayedRegions[EAST] = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDoubleClick(final MouseEvent e) { } // NOPMD

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDown(final MouseEvent e) {
        final Rectangle viewportBounds;

        this.weAreInADrag = true;

        viewportBounds = this.backingCanvas.getViewport().getBounds();

        this.renderSpatialThresholds = new int[4];
        this.renderSpatialThresholds[NORTH] = (int)(SPATIAL_ZONE_PERCENTAGE * viewportBounds.height);
        this.renderSpatialThresholds[WEST] = (int)(SPATIAL_ZONE_PERCENTAGE * viewportBounds.width);
        this.renderSpatialThresholds[SOUTH] = viewportBounds.height - this.renderSpatialThresholds[0];
        this.renderSpatialThresholds[EAST] = viewportBounds.width - this.renderSpatialThresholds[1];

        this.renderDimensions = new int[2];
        this.renderDimensions[0] = this.renderSpatialThresholds[WEST];
        this.renderDimensions[1] = this.renderSpatialThresholds[NORTH];

        this.currentlyDisplayedRegions = new RoundedRectangle[4];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseUp(final MouseEvent e) {
        this.weAreInADrag = false;

        this.renderSpatialThresholds = null;
        this.renderDimensions = null;

        for (int i = 0; i < 4; i++) {
            if (this.currentlyDisplayedRegions[i] != null) {
                this.rootFigure.remove(this.currentlyDisplayedRegions[i]);
            }
        }
        this.renderSpatialThresholds = null;
    }

}
