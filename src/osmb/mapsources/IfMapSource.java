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
package osmb.mapsources;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import osmb.program.jaxb.MapSourceAdapter;
// W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;

/**
 * These are the methods common for all types of map sources.
 */
@XmlJavaTypeAdapter(MapSourceAdapter.class)
public interface IfMapSource
{
	/**
	 * This specifies where to look for a tile.
	 */
	public enum LoadMethod
	{
		DEFAULT, CACHE, SOURCE, STORE, ALL
	};

	/**
	 * A map source name has to be unique. This retrieves a 'user friendly' name.
	 * 
	 * @return Name of the map source as used in the UI.
	 */
	public String getName();

	/**
	 * The map source has an internally used ID to avoid string compares with the map source name.
	 * 
	 * @return ID of this map source as used internally, esp. by the tile store.
	 */
	// public int getID();

	// W ? what is Bundle#getMaxZoomLevel?
	/**
	 * Specifies the maximum zoom value. The number of zoom levels supported by this map source is [{@link #getMinZoom}.. {@link #getMaxZoom}].
	 * To use the zoom level it has to checked against {@link Bundle#getMaxZoomLevel}() and {@link MP2MapSpace#MAX_TECH_ZOOM}. <br>
	 * The first one is the maximum reasonable to be used in the bundles output format and device, the latter is the maximum zoom level supported by osmbs
	 * internal data structures.
	 * 
	 * @return maximum zoom value - currently (2015) <= {@link MP2MapSpace#MAX_TECH_ZOOM 22}
	 */
	public int getMaxZoom();

	// W ? what is Bundle.getMinZoomLevel()?
	/**
	 * Specifies the minimum zoom value. The number of zoom levels supported by this map source is [{@link #getMinZoom}.. {@link #getMaxZoom}].
	 * To use the zoom level it has to checked against Bundle.getMinZoomLevel() and IfMapSpace.MIN_TECH_ZOOM. The first one is the minimum reasonable to be used
	 * in the bundles output format and device, the latter is the minimum zoom level supported by osmbs internal data structures.
	 * 
	 * @return minimum zoom value - usually 0
	 */
	public int getMinZoom();

	/**
	 * Specifies the tile image type. For tiles rendered by Mapnik or Osmarenderer this is usually {@link TileImageType#PNG}.
	 * 
	 * @return Tile image file type.
	 */
	public TileImageType getTileImageType();

	public Color getBackgroundColor();

	public MapSourceLoaderInfo getLoaderInfo();

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo);

	/**
	 * Retrieves the data for the specified tile according to the specified load method as a byte array. If no data are available at the specified location, null
	 * is returned.<br>
	 * The loadMethod specifies where to look for the tile.<br>
	 * - STORE: looks if the tile is available in the tile store.<br>
	 * - SOURCE: loads the tile data from the (online) source.<br>
	 * - CACHE: looks in the mtc for the tile data.<br>
	 * - DEFAULT:<br>
	 * - ALL:<br>
	 * 
	 * @param zoom
	 * @param x
	 * @param y
	 * @param loadMethod
	 * @return the tile data as a byte array
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException;

	/**
	 * Retrieves the tile data as an image.
	 * The loadMethod specifies where to look for the tile.<br>
	 * - STORE: looks if the tile is available in the tile store.<br>
	 * - SOURCE: loads the tile data from the (online) source.<br>
	 * - CACHE: looks in the mtc for the tile data.<br>
	 * - DEFAULT:<br>
	 * - ALL:<br>
	 * 
	 * @param zoom
	 * @param x
	 * @param y
	 * @param loadMethod
	 * @return The tile data as an image or null if no image available
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException;

	/**
	 * This retrieves the tile image directly from the online map source.
	 * <p>
	 * 
	 * @param zoom
	 * @param x
	 * @param y
	 * @return The tile data as an image or null if no image available.
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	public BufferedImage downloadTileImage(int zoom, int x, int y) throws IOException, TileException, InterruptedException;

	public void initialize();
}
