/*
 * ------------------------------------------------------------------------
 *
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
 */
package org.knime.core.jaxrs.workflow.entity;

import org.knime.core.gateway.v0.workflow.entity.WorkflowUIInfoEnt;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link WorkflowUIInfoEnt} interface that can be deserialized from a json object (json-annotated constructor).
 *
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "EntityType")
@JsonSubTypes({ 
  @Type(value = WorkflowUIInfoEntFromJson.class, name = "WorkflowUIInfoEnt")
})
public class WorkflowUIInfoEntFromJson  implements WorkflowUIInfoEnt {

	private int m_GridX;
	private int m_GridY;
	private boolean m_SnapToGrid;
	private boolean m_ShowGrid;
	private double m_ZoomLevel;
	private boolean m_HasCurvedConnection;
	private int m_ConnectionLineWidtdh;

	@JsonCreator
	private WorkflowUIInfoEntFromJson(
	@JsonProperty("GridX") int GridX,	@JsonProperty("GridY") int GridY,	@JsonProperty("SnapToGrid") boolean SnapToGrid,	@JsonProperty("ShowGrid") boolean ShowGrid,	@JsonProperty("ZoomLevel") double ZoomLevel,	@JsonProperty("HasCurvedConnection") boolean HasCurvedConnection,	@JsonProperty("ConnectionLineWidtdh") int ConnectionLineWidtdh	) {
		m_GridX = GridX;
		m_GridY = GridY;
		m_SnapToGrid = SnapToGrid;
		m_ShowGrid = ShowGrid;
		m_ZoomLevel = ZoomLevel;
		m_HasCurvedConnection = HasCurvedConnection;
		m_ConnectionLineWidtdh = ConnectionLineWidtdh;
	}
	
	protected WorkflowUIInfoEntFromJson() {
		//just a dummy constructor for subclasses
	}


	@Override
    public int getGridX() {
        	return m_GridX;
            
    }
    
	@Override
    public int getGridY() {
        	return m_GridY;
            
    }
    
	@Override
    public boolean getSnapToGrid() {
        	return m_SnapToGrid;
            
    }
    
	@Override
    public boolean getShowGrid() {
        	return m_ShowGrid;
            
    }
    
	@Override
    public double getZoomLevel() {
        	return m_ZoomLevel;
            
    }
    
	@Override
    public boolean getHasCurvedConnection() {
        	return m_HasCurvedConnection;
            
    }
    
	@Override
    public int getConnectionLineWidtdh() {
        	return m_ConnectionLineWidtdh;
            
    }
    

}