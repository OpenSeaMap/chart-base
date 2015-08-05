/*******************************************************************************
 * Copyright (c) OSMCB developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package osmb.program.map;

import java.awt.Dimension;
import java.awt.Point;

<<<<<<< HEAD
import javax.xml.bind.annotation.XmlAttribute;

=======
>>>>>>> origin/master
import osmb.mapsources.IfMapSource;
import osmb.program.catalog.IfCapabilityDeletable;
import osmb.program.catalog.IfCatalogObject;
import osmb.program.tiles.IfTileFilter;
import osmb.program.tiles.TileImageParameters;

public interface IfMap extends IfCatalogObject, IfCapabilityDeletable
{
	public IfMapSource getMapSource();

	/**
	 * a map is of exactly on zoom level
	 * 
	 * @return zoom level of this map
	 */
	public int getZoom();

	/**
	 * A map belongs to one layer
	 * 
	 * @return The layer this map is a child of
	 */
	public IfLayer getLayer();

	public void setLayer(IfLayer layer);

	/**
	 * The lower left (south-west) edge of the map
	 * 
	 * @return
	 */
<<<<<<< HEAD
	@XmlAttribute
=======
>>>>>>> origin/master
	public Point getMinTileCoordinate();

	public void setMinTileCoordinate(Point MinC);

	/**
	 * The upper right (north-east) edge of the map
	 * 
	 * @return
	 */
<<<<<<< HEAD
	@XmlAttribute
=======
>>>>>>> origin/master
	public Point getMaxTileCoordinate();

	public void setMaxTileCoordinate(Point MaxC);

	/**
	 * The dimension of the tiles used in this map. This depends on the mapsource
	 * 
	 * @return The tile dimension in pixels
	 */
	public Dimension getTileSize();

	public TileImageParameters getParameters();

	public void setParameters(TileImageParameters p);

	/**
	 * A description of the map for use in the GUI
	 * 
	 * @return A descriptive String of the map
	 */
	public String getInfoText();

	public IfTileFilter getTileFilter();

	public IfMap deepClone(IfLayer newLayer);
}
