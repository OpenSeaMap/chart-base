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

import org.apache.log4j.Logger;

import osmb.utilities.geo.GeoCoordinate;

/**
 * A map space pixel address. It is defined by x index, y index and zoom level.
 * - It is NOT the pixel. -
 * 
 * @author humbach
 */
public class PixelAddress
{
	private static final Logger log = Logger.getLogger(PixelAddress.class);

	private int mX = 0;
	private int mY = 0;
	private int mZoom = 0;

	/**
	 * Constructs a new map space pixel address with the specified data.
	 * 
	 * @param xIdx
	 *          The pixels x index
	 * @param yIdx
	 *          The pixels y index
	 * @param zoomLvl
	 *          The pixels zoom level
	 */
	public PixelAddress(int xIdx, int yIdx, int zoomLvl)
	{
		mX = xIdx;
		mY = yIdx;
		mZoom = zoomLvl;
	}

	/**
	 * ??? only used in TileStoreCoverageLayer
	 * 
	 * @param corner
	 */
	public PixelAddress(MP2Corner corner)
	{
		this.mZoom = corner.getZoom();
		int sizeInPixel = MP2MapSpace.getSizeInPixel_UC(mZoom);
		if (corner.getX() == sizeInPixel)
		{
			log.warn("called for easternmost border (180°E), x = " + corner.getX() + ", using (x - 1)");
			this.mX = corner.getX() - 1;
		}
		else
			this.mX = corner.getX();
		if (corner.getY() == sizeInPixel)
		{
			log.warn("called for southernmost border (-85.051..°S), y = " + corner.getY() + ", using (y - 1)");
			this.mY = corner.getY() - 1;
		}
		else
			this.mY = corner.getY();
	}

	// /**
	// * zoom = MP2MapSpace.MAX_TECH_ZOOM
	// *
	// * @param lat
	// * @param lon
	// */
	// public PixelAddress(double lat, double lon)
	// {
	// this(MP2MapSpace.cLonToX_Pixel(lon, MP2MapSpace.MAX_TECH_ZOOM), MP2MapSpace.cLatToY_Pixel(lat, MP2MapSpace.MAX_TECH_ZOOM), MP2MapSpace.MAX_TECH_ZOOM);
	// }

	/**
	 * @return The zoom level of this pixel.
	 */
	public int getZoom()
	{
		return mZoom;
	}

	/**
	 * @return The x index of the pixel.
	 */
	public int getX()
	{
		return mX;
	}

	/**
	 * @return The y index of the pixel.
	 */
	public int getY()
	{
		return mY;
	}

	/**
	 * @return The address of the tile which contains this pixel.
	 */
	public TileAddress getTileAddress()
	{
		return new TileAddress(this);
	}

	/**
	 * @return The geo coordinate of the upper left corner of this pixel.
	 */
	public GeoCoordinate toGeoUpperLeftCorner()
	{
		double lon = MP2MapSpace.cXToLonLeftBorder(mX, mZoom);
		double lat = MP2MapSpace.cYToLatUpperBorder(mY, mZoom);
		return new GeoCoordinate(lat, lon);
	}

	// public GeoCoordinate toGeoLowerRightCorner()
	// {
	// double lon = MP2MapSpace.cXToLonRightBorder(mX, mZoom);
	// double lat = MP2MapSpace.cYToLatLowerBorder(mY, mZoom);
	// return new GeoCoordinate(lat, lon);
	// }
	//
	// public GeoCoordinate toGeoPixelCenter()
	// {
	// double lon = MP2MapSpace.cXToLonPixelCenter(mX, mZoom);
	// double lat = MP2MapSpace.cYToLatPixelCenter(mY, mZoom);
	// return new GeoCoordinate(lat, lon);
	// }
	//
	// public PixelAddress zoomOut(int newSmallerZoomLevel)
	// {
	// if (newSmallerZoomLevel >= mZoom)
	// {
	// log.error("OUT OF RANGE: called with newSmallerZoomLevel = " + newSmallerZoomLevel + " >= " + mZoom + " -> return!");
	// }
	// newSmallerZoomLevel = Math.max(MP2MapSpace.MIN_TECH_ZOOM, Math.min(newSmallerZoomLevel, MP2MapSpace.MAX_TECH_ZOOM));
	// return new MP2Pixel(MP2MapSpace.xyChangeZoom_UC(mX, mZoom, newSmallerZoomLevel), MP2MapSpace.xyChangeZoom_UC(mY, mZoom, newSmallerZoomLevel),
	// newSmallerZoomLevel);
	// }
}
