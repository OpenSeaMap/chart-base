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
import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;

/**
 * 
 */
@XmlJavaTypeAdapter(MapSourceAdapter.class)
public interface IfMapSource
{
	public enum LoadMethod
	{
		DEFAULT, CACHE, SOURCE
	};

	/**
	 * Specifies the maximum zoom value. The number of zoom levels supported by this map source is [{@link #getMinZoom()}.. {@link #getMaxZoom()}].
	 * To use the zoom level it has to checked against Bundle.getMaxZoomLevel() and IfMapSpace.MAX_TECH_ZOOM. The first one is the maximum reasonable to be used
	 * in the bundles output format and device, the latter is the maximum zoom level supported by osmbs internal data structures.
	 * 
	 * @return maximum zoom value - currently (2015) <= 22
	 */
	public int getMaxZoom();

	/**
	 * Specifies the minimum zoom value. The number of zoom levels supported by this map source is [{@link #getMinZoom()}.. {@link #getMaxZoom()}].
	 * To use the zoom level it has to checked against Bundle.getMinZoomLevel() and IfMapSpace.MIN_TECH_ZOOM. The first one is the minimum reasonable to be used
	 * in the bundles output format and device, the latter is the minimum zoom level supported by osmbs internal data structures.
	 * 
	 * @return minimum zoom value - usually 0
	 */
	public int getMinZoom();

	/**
	 * A tile layer name has to be unique.
	 * 
	 * @return Name of the tile layer
	 */
	public String getName();

	/**
	 * Retrieves the tile data as a byte array.
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
	 * 
	 * @param zoom
	 * @param x
	 * @param y
	 * @param loadMethod
	 * @return the tile data as an image
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException;

	/**
	 * Specifies the tile image type. For tiles rendered by Mapnik or Osmarenderer this is usually {@link TileImageType#PNG}.
	 * 
	 * @return Tile image file type
	 */
	public TileImageType getTileImageType();

	public IfMapSpace getMapSpace();

	public Color getBackgroundColor();

	public MapSourceLoaderInfo getLoaderInfo();

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo);
}
