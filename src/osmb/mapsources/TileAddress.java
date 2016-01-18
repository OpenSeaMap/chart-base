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

//import org.apache.log4j.Logger;

/**
 * A map tile address. It is defined by x index, y index and zoom level.
 * - It is NOT the tile. -
 * 
 * @author humbach
 */
public class TileAddress
{
	// private static final Logger log = Logger.getLogger(TileAddr.class);

	private int mX = 0;
	private int mY = 0;
	private int mZoom = 0;

	/**
	 * This creates the address of the tile which the specified pixel contains.
	 * 
	 * @param pixel
	 */
	public TileAddress(PixelAddress pixel)
	{
		mX = pixel.getX() / MP2MapSpace.TECH_TILESIZE;
		mY = pixel.getY() / MP2MapSpace.TECH_TILESIZE;
		mZoom = pixel.getZoom();
	}

	// /**
	// * ???
	// * zoom = MP2MapSpace.MAX_TECH_ZOOM
	// *
	// * @param lat
	// * @param lon
	// */
	// public TileAddr(double lat, double lon)
	// {
	// this(MP2MapSpace.cLonToX_Pixel(lon, MP2MapSpace.MAX_TECH_ZOOM) / MP2MapSpace.TECH_TILESIZE,
	// MP2MapSpace.cLatToY_Pixel(lat, MP2MapSpace.MAX_TECH_ZOOM) / MP2MapSpace.TECH_TILESIZE, MP2MapSpace.MAX_TECH_ZOOM);
	// }

	/**
	 * @return The x index of this tile.
	 */
	public int getX()
	{
		return mX;
	}

	/**
	 * @return The y index of this tile.
	 */
	public int getY()
	{
		return mY;
	}

	/**
	 * @return The zoom level of this tile.
	 */
	public int getZoom()
	{
		return mZoom;
	}

	// public TileAddress zoomOut(int newSmallerZoomLevel)
	// {
	// if (newSmallerZoomLevel >= mZoom)
	// {
	// log.error("OUT OF RANGE: called with newSmallerZoomLevel = " + newSmallerZoomLevel + " >= " + mZoom + " -> return!");
	// }
	// newSmallerZoomLevel = Math.max(MP2MapSpace.MIN_TECH_ZOOM, Math.min(newSmallerZoomLevel, MP2MapSpace.MAX_TECH_ZOOM));
	// return new MP2Tile(MP2MapSpace.xyChangeZoom_UC(mX, mZoom, newSmallerZoomLevel), MP2MapSpace.xyChangeZoom_UC(mY, mZoom, newSmallerZoomLevel),
	// newSmallerZoomLevel);
	// }

	// public MP2Pixel getMinPixelCoordinate()
	// {
	// return new MP2Pixel(mX * MP2MapSpace.TECH_TILESIZE, mY * MP2MapSpace.TECH_TILESIZE, mZoom);
	// }

	/**
	 * @return The address of the pixel at the upper left corner of this tile.
	 */
	public MP2Corner getUpperLeftCorner()
	{
		return new MP2Corner(mX * MP2MapSpace.TECH_TILESIZE, mY * MP2MapSpace.TECH_TILESIZE, mZoom);
	}

	// public MP2Pixel getMaxPixelCoordinate()
	// {
	// return new MP2Pixel((mX + 1) * MP2MapSpace.TECH_TILESIZE - 1, (mY + 1) * MP2MapSpace.TECH_TILESIZE - 1, mZoom);
	// }
	//
	// public MP2Corner getBottomRightCorner()
	// {
	// return new MP2Corner((mX + 1) * MP2MapSpace.TECH_TILESIZE, (mY + 1) * MP2MapSpace.TECH_TILESIZE, mZoom);
	// }
}
