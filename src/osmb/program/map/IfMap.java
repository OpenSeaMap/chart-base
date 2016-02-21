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

import osmb.mapsources.IfMapSource;
import osmb.program.catalog.IfCapabilityDeletable;
import osmb.program.catalog.IfCatalogObject;
import osmb.program.tiles.IfTileFilter;
import osmb.program.tiles.TileImageParameters;

/**
 * This interface describes the necessary elements for a map. Its standard implementation is {@link Map}
 * 
 * @author humbach
 */
public interface IfMap extends IfCatalogObject, IfCapabilityDeletable
{
	/**
	 * Each map is of exactly one map source. We currently don't support the 'on the fly' combination of multi-map-maps from different map sources.
	 * We need to define a multi-map source for these, which then is handled as one single map source.
	 * 
	 * @return The map source where this map is taken from.
	 */
	public IfMapSource getMapSource();

	/**
	 * A map is of exactly one zoom level. This is the zoom level of the layer to which this map belongs.
	 * 
	 * @return zoom level of this map
	 */
	public int getZoom();

	/**
	 * A map has an internal name stating the maps zoom, position and size, we call it the maps number to be conforming to most agencies deploying maps, which
	 * have proprietary systems to number their charts. These numbers are easier to handle and communicate than descriptive names for the charts.
	 * The numbers are given in map units, which are 8-tiles for lat and lon and tiles for width and height.
	 * 
	 * This number is generated during construction of the map, so there is no setter available.
	 * We need a setter if we want to be able to resize maps later while consolidating the catalog.
	 * 
	 * @return The (internal) number of the map in the format 'Zoom-Lat-Lon-Height-Width' in map units.
	 */
	public String getNumber();

	/**
	 * A map belongs to one layer.
	 * 
	 * @return The layer this map is a child of.
	 */
	public IfLayer getLayer();

	public void setLayer(IfLayer layer);

	/**
	 * The tile coordinates of the upper left (north-west) tile of the map.
	 * 
	 * @return Tile coordinate (N-W).
	 */
	public Point getMinTileCoordinate();

	/**
	 * Sets the tile coordinate of the upper left (north-west) tile of the map.
	 */
	public void setMinTileCoordinate(Point MinC);

	/**
	 * The tile coordinate of the bottom right (south-east) tile of the map.
	 * 
	 * @return Tile coordinate (S-E).
	 */
	public Point getMaxTileCoordinate();

	/**
	 * Sets the tile coordinates of the bottom right (south-east) tile of the map.
	 */
	public void setMaxTileCoordinate(Point MaxC);

	/**
	 * The pixel coordinates of the upper left (north-west) pixel of the map.
	 * 
	 * @return Pixel coordinate (N-W).
	 */
	public Point getMinPixelCoordinate();

	/**
	 * Sets the pixel coordinates of the upper left (north-west) pixel of the map.
	 */
	public void setMinPixelCoordinate(Point MinC);

	/**
	 * The pixel coordinates of the bottom right (south-east) pixel of the map.
	 * 
	 * @return Pixel coordinate (S-E).
	 */
	public Point getMaxPixelCoordinate();

	/**
	 * Sets the pixel coordinates of the bottom right (south-east) pixel of the map.
	 */
	public void setMaxPixelCoordinate(Point MaxC);

	/**
	 * The dimension of the tiles used in this map. This depends on the map source. Currently (2015) we support exclusively tiles of 256 by 256 pixels.
	 * 
	 * @return The tile dimension in pixels.
	 */
	public Dimension getTileSize();

	public TileImageParameters getParameters();

	public void setParameters(TileImageParameters p);

	/**
	 * A description of the map for use in the GUI.
	 * 
	 * @return A descriptive String of the map.
	 */
	public String getInfoText();

	public IfTileFilter getTileFilter();

	public IfMap deepClone(IfLayer newLayer);

	/**
	 * @return The number of tiles in this map.
	 */
	public long getTileCount();

	/**
	 * @return XMin in tile indices
	 */
	public int getXMin();

	/**
	 * @return XMax in tile indices
	 */
	public int getXMax();

	/**
	 * @return YMin in tile indices
	 */
	public int getYMin();

	/**
	 * @return YMax in tile indices
	 */
	public int getYMax();
}
