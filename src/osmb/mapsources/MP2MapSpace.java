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

import java.awt.Point;

import org.apache.log4j.Logger;

/**
 * Mercator projection (globe <-> map space) with a map space width and height of 256 * 2<sup>zoom</sup> pixel.<br>
 * 
 * This is the common projection used by OpenStreetMap and Google.<br>
 * It provides methods to translate coordinates from 'map space' into latitude and longitude (on the WGS84 ellipsoid) and vice versa.<br>
 * Map space is measured in pixel. The origin of the map space is the top left pixel ( 0 | 0 ).
 * <ul>	Differentiations: (x/y integers, lat/lon doubles)
 * <li> Border coordinates: Every pixel ( x | y ) has four borders ( x: western, x + 1: eastern, y: northern, y + 1: southern )<br>
 *      The mapping x <-> lon/y <-> lat is 	within the limits of rounding
 * <li> Pixel coordinate:
 * <li> Pixel center:
 *  The map space origin  has latitude ~85 and longitude -180.
 * this makes: xMin - west; yMin - north; xMax - east; yMax - south.
 * While the geo coordinates are independent of zoom level the map space is NOT. Map space coordinates, pixel and tiles, are depending on the zoom level for
 * the same geographic point.
 * 
 * <p>
 * This is the only implementation that is currently supported by OpenSeaMap ChartBundler.
 * </p>
 * 
 * Currently it supports a world up to zoom level 22 because of the use of 32bit integer for pixel coordinates.
 * 
 * <ul> Determinations:
 * <li> {@link #TECH_TILESIZE} = 256 = 2<sup>8</sup></li>
 * <li> {@link #MAX_TECH_ZOOM} = 22</li>
 * <li> {@link #MIN_TECH_ZOOM} = 0</li>
 * <li> {@link #MAX_LAT} = 85.0511...° North</li>
 * <li> {@link #MIN_LAT} = -85.0511...° South</li>
 * <li> -180° (West) == 180° (East) <-> longitudinal cut</li>
 * </ul>
 * 
 * This contains code originally from Jan Peter Stotz.
 * @author wilbert
 *
 */
public class MP2MapSpace
{
	private static final Logger log = Logger.getLogger(MP2MapSpace.class);
	
	/**
	 * These are the software technical limits because we use an int for the pixel coordinates. In zoom level 23 the coordinate would exceed the int...
	 * If we will want to provide higher zoom levels, we have to change the data type used to 64bit int... This is a lot of work, so we do it not (now).<p>
	 * see {@link #TECH_TILESIZE} and {@link #MAX_TECH_ZOOM}
	 */
	public static final int MIN_TECH_ZOOM = 0;
	/**
	 * <code>MAX_TECH_ZOOM = 22</code>
	 */
	public static final int MAX_TECH_ZOOM = 22;
	/**
	 * <code>TECH_TILESIZE = 256</code> = 2<sup>8</sup>
	 */
	public static final int TECH_TILESIZE = 256;
	
	/**
	 * The northernmost border of map space (MAX_LAT = cYToLat_UR(0, zoom) = 85.05112877980659...).
	 */
	public static final double MAX_LAT = cYToLat_Borders(0, 0); // (0, 0): (northernmost border, any zoom level)
	/**
	 * The southernmost border of map space (MIN_LAT = cYToLat_UR(getMaxPixels(zoom), zoom) = -85.05112877980659...).
	 */
	public static final double MIN_LAT = cYToLat_Borders(TECH_TILESIZE, MIN_TECH_ZOOM); // (TECH_TILESIZE, MIN_TECH_ZOOM): (southernmost border in zoom 0, zoom level 0)
	
	/**
	 * The pixel difference between latitude = {@link #MAX_LAT} and latitude = 0° depending on (UnChecked) zoom level.
	 * 
	 * @param aZoomlevel
	 *                   The parameter zoom has to be checked before using this method!
	 * @return
	 *         -1/2 * 2<sup>zoom + 8</sup>
	 */
	protected static int falseNorthing_UC(int zoom)
	{
		return (-1 * getSizeInPixel_UC(zoom) / 2);
	}
	
	/**
	 * The size (width or height) of the map space in pixel depending on (UnChecked) zoom level.
	 * 
	 * @param zoom
	 *             The parameter zoom has to be checked before using this method!
	 * @return
	 *         2<sup>zoom + 8</sup>
	 */
	protected static int getSizeInPixel_UC(int zoom)
	{
		return TECH_TILESIZE * (1 << zoom);
	}
	
	/**
	 * The radius of the globe in equatorial pixel width depending on (UnChecked) zoom level.
	 * 
	 * @param zoom
	 *             The parameter zoom has to be checked before using this method!
	 * @return
	 *         2<sup>zoom + 8</sup> / (2 * &Pi;)
	 */
	protected static double radius_UC(int zoom)
	{
		return getSizeInPixel_UC(zoom) / (2.0 * Math.PI);
	}
	
	/**
	 * Calculates horizontal map space coordinate x to 'longitude' in radian depending on zoom level.<br>
	 * 
	 * Private access because of UnChecked parameters!
	 * 
	 * @param x
	 *          UnChecked map space coordinate
	 * @param zoom
	 *             UnChecked zoom level
	 * @return
	 *         Longitude in radian
	 */
	private static double cXToRadian_UC(int x, int zoom)
	{
		return ((2.0 * Math.PI * x) / getSizeInPixel_UC(zoom)) - Math.PI;
	}

	// unrestricted
	private static double cYToRadian_UC(int y, int zoom)
	{
		y += falseNorthing_UC(zoom);
		return 2.0 * Math.atan(Math.exp(-1.0 * y / radius_UC(zoom))) - (Math.PI / 2.0);
	}

	// unrestricted
	private static int cRadianToLeftXBorder(double lonRad, int zoom)
	{
		return  (int) ((getSizeInPixel_UC(zoom) * (lonRad + Math.PI)) / (2.0 * Math.PI));
	}
	
	// NOT RESTRICTED! Check parameters in calling methods! if unchecked: log(0), div(0), ...
	private static int cRadianToUpperYBorder(double latRad, int zoom)
	{
		double sinLat = Math.sin(latRad);
		double log = Math.log((1.0 + sinLat) / (1.0 - sinLat));
		return (int) (getSizeInPixel_UC(zoom) * (0.5 - (log / (4.0 * Math.PI))));
	}
	
	protected static int checkZoom(int zoom, String methodName)
	{
		int nRet = zoom;
		if ((nRet < MIN_TECH_ZOOM) || (nRet > MAX_TECH_ZOOM))
		{
			nRet = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
			log.error("zoom out of range in method " + methodName + ": zoom = " + zoom + " -> changed to new value = " + nRet);
		}
		return nRet;
	}

	/**
	 * @return
	 *         size (height or width) of each tile in pixel. A tile is a square in pixels.<br>
	 *         Currently we support only tiles with 256 x 256 pixels.<br>
	 *         see {@link #MIN_TECH_ZOOM TECH_TILESIZE}.
	 */
	public static int getTileSize()
	{
		return TECH_TILESIZE;
	}
	
	public static int getSizeInPixel(int zoom)
	{
		checkZoom(zoom, "int getSizeInPixel(int zoom)");
		return getSizeInPixel_UC(zoom);
	}
	
	/**
	 * Converts the horizontal map space coordinate x to longitudinal border coordinate depending on zoom level.<br>
	 * 
	 * This method checks and restricts parameters to tolerable values.
	 * 
	 * @param x
	 *          The horizontal map space coordinate at the specified zoom level (restricted to x in { 0 ... 2<sup>zoom + 8</sup> })
	 * @param zoom
	 *             A zoom level (restricted to zoom in { 0 ... 22 })
	 * @return
	 *          Longitudinal border in degrees (in { -180d (W) ... 180d (E)})
	 */
	public static double cXToLon_Borders(int x, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "double cXToLon_Borders(int x, int zoom)");
		int sizeInPixel = getSizeInPixel_UC(checkedZoom);
		if (x < 0 || x > sizeInPixel)
		{
			log.error("OUT OF RANGE: called with x = " + x + " (zoom = " + checkedZoom + ") -> x restricted!");
			x = Math.max(0, Math.min(x, sizeInPixel));
		}
		return Math.toDegrees(cXToRadian_UC(x, checkedZoom));
	}
	
	public static double cXToLon_PixelCoo(int x, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "double cXToLon_PixelCoo(int x, int zoom)");
		int sizeInPixel = getSizeInPixel_UC(checkedZoom);
		if (x < 0 || x >= sizeInPixel)
		{
			log.error("OUT OF RANGE: called with x = " + x + " (zoom = " + checkedZoom + ") -> x restricted!");
			x = Math.max(0, Math.min(x, sizeInPixel - 1));
		}
		return Math.toDegrees(cXToRadian_UC(x, checkedZoom));
	}
	
	public static double cXToLon_PixelCenter(int x, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "double cXToLon_PixelCenter(int x, int zoom)");
		int sizeInPixel = getSizeInPixel_UC(checkedZoom); // checks zoom
		if (x < 0 || x >= sizeInPixel)
		{
			log.error("OUT OF RANGE: called with x = " + x + " (zoom = " + checkedZoom + ") -> x restricted!");
			x = Math.max(0, Math.min(x, sizeInPixel - 1));
		}
		return Math.toDegrees((cXToRadian_UC(x, checkedZoom) + cXToRadian_UC(x + 1, checkedZoom)) / 2);
	}

	/**
	 * Converts the vertical pixel coordinate from map space to latitude.xxx
	 * 
	 * @param y
	 *          The border coordinate at the specified zoom level
	 * @param zoom
	 * @return latitude
	 */
	public static double cYToLat_Borders(int y, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "double cYToLat_Borders(int y, int zoom)");
		if (y < 0 || y > getSizeInPixel_UC(checkedZoom))
		{
			log.error("OUT OF RANGE: called with y = " + y + " (zoom = " + checkedZoom + ") -> y restricted!");
			y = Math.max(0, Math.min(y, getSizeInPixel_UC(checkedZoom)));
		}
		return Math.toDegrees(cYToRadian_UC(y, checkedZoom));
	}

	public static double cYToLat_PixelCoo(int y, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "double cYToLat_Borders(int y, int zoom)");
		int sizeInPixel = getSizeInPixel_UC(checkedZoom);
		if (y < 0 || y >= sizeInPixel)
		{
			log.error("OUT OF RANGE: called with y = " + y + " (zoom = " + checkedZoom + ") -> y restricted!");
			y = Math.max(0, Math.min(y, sizeInPixel - 1));
		}
		return Math.toDegrees(cYToRadian_UC(y, checkedZoom));
	}
	
	public static double cYToLat_PixelCenter(int y, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "double cYToLat_Borders(int y, int zoom)");
		int sizeInPixel = getSizeInPixel_UC(checkedZoom);
		if (y < 0 || y >= sizeInPixel)
		{
			log.error("OUT OF RANGE: called with y = " + y + " (zoom = " + checkedZoom + ") -> y restricted!");
			y = Math.max(0, Math.min(y, sizeInPixel - 1));
		}
		return Math.toDegrees((cYToRadian_UC(y, checkedZoom) + cYToRadian_UC(y + 1, checkedZoom)) / 2);
	}
	
	/**
	 * Converts longitude to the horizontal pixel coordinate from map space.xxx
	 * 
	 * @param lon
	 *          in degrees
	 * @param zoom
	 * @return The border coordinate for the specified zoom level
	 */
	public static int cLonToX_Borders(double lon, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "int cLonToX_Borders(double lon, int zoom)");
		int x = cRadianToLeftXBorder(Math.toRadians(lon), checkedZoom);
		if (x < 0 || x > getSizeInPixel(checkedZoom))
		{
			x = Math.max(0, Math.min(getSizeInPixel(checkedZoom), x));
			log.error("OUT OF RANGE: called with lon = " + lon + " -> output restricted to x = " + x + " (zoom = " + checkedZoom + ")");
		}
		return x;
	}

	public static int cLonToX_Pixel(double lon, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "int cLonToX_Pixel(double lon, int zoom)");
		int x = cRadianToLeftXBorder(Math.toRadians(lon), checkedZoom);
		if (x < 0 || x >= getSizeInPixel(checkedZoom))
		{
			x = Math.max(0, Math.min(getSizeInPixel(checkedZoom) - 1, x));
			log.error("OUT OF RANGE: called with lon = " + lon + " -> output restricted to x = " + x + " (zoom = " + checkedZoom + ")");
		}
		return x;
	}

	/**
	 * Converts latitude to the vertical border coordinate from map space.xxx
	 * 
	 * @param lat
	 *          in degrees
	 * @param zoom
	 * @return The border coordinate for the specified zoom level
	 */

	public static int cLatToY_Borders(double lat, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "int cLatToY_Borders(double lat, int zoom)");
		if (lat < MIN_LAT || lat > MAX_LAT)
		{
			log.error("OUT OF RANGE: called with latitude = " + lat + " -> restricted!");
			lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat)); // to prevent log(0), div(0), ...
		}
		return Math.max(0, Math.min(getSizeInPixel(checkedZoom), cRadianToUpperYBorder(Math.toRadians(lat), checkedZoom))); // W ? min/max needless?
	}

	public static int cLatToY_Pixel(double lat, int zoom)
	{
		int checkedZoom = checkZoom(zoom, "int cLatToY_Pixel(double lat, int zoom)");
		if (lat < MIN_LAT || lat >= MAX_LAT)
		{
			log.error("OUT OF RANGE: called with latitude = " + lat + " -> restricted!");
			lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat)); // to prevent log(0), div(0), ...
		}
		return Math.max(0, Math.min(getSizeInPixel(checkedZoom) - 1, cRadianToUpperYBorder(Math.toRadians(lat), checkedZoom))); // restriction to pixel coord
	}

	// horizontalDistance(0, y, 255) ~= 2 * PI, 
	public static double horizontalDistance(int zoom, int y, int xDist)
	{
		int checkedZoom = checkZoom(zoom, "double horizontalDistance(int zoom, int y, int xDist)");
		int sizeInPixel = getSizeInPixel_UC(checkedZoom);
		if (y < 0 || y > getSizeInPixel_UC(checkedZoom)) // check y, restrictions already in old version
		{
			// W #mapSpace ??? log.debug
			log.debug("called with y = " + y + " (zoom = " + checkedZoom + ") -> y out of range?");
			y = Math.max(0, Math.min(y, sizeInPixel));
			log.debug("y restricted to " + y);
		}
		if (xDist < 0 || xDist >= sizeInPixel) // check xDist
		{
			if (xDist == sizeInPixel)
				return 0; // W #??? or circumference of latitude = y
			else
				log.error("OUT OF RANGE: called with x = " + xDist + " (zoom = " + checkedZoom + ") -> xDist restricted!");
			xDist = Math.max(0, Math.min(xDist, sizeInPixel));
		}
		double latRad = cYToRadian_UC(y, checkedZoom);
		double lon1Rad = -1.0 * Math.PI;
		double lon2Rad = cXToRadian_UC(xDist, checkedZoom);
		double dLonRad = lon2Rad - lon1Rad;
		double cos_lat = Math.cos(latRad);
		return dLonRad * cos_lat;
		
		// W ??? old code instead of return dLonRad * cos_lat;
		// double sin_dLon_2 = Math.sin(dLonRad) / 2;
		// double a = cos_lat * cos_lat * sin_dLon_2 * sin_dLon_2;
		// return 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	public static int xyChangeZoom(int xy, int oldZoom, int newZoom)
	{
		int checkedOldZoom = checkZoom(oldZoom, "int xyChangeZoom(int xy, int oldZoom, int newZoom)");
		int checkedNewZoom = checkZoom(newZoom, "int xyChangeZoom(int xy, int oldZoom, int newZoom)");
		//check xy, restrict? #mapSpace ???
		if (xy < 0 || xy > getSizeInPixel_UC(checkedOldZoom))
			log.error("OUT OF RANGE: called with xy = " + xy + " (oldZoom = " + checkedOldZoom + ")");
				
		int zoomDiff = checkedOldZoom - checkedNewZoom;
		return (zoomDiff > 0) ? xy >> zoomDiff : xy << -zoomDiff;
	}

	/**
	 * #mapSpace ??? better using MP2Corner public MP2Corner adaptToZoomlevel(int aZoomLevel)
	 * 
	 * to satisfy often used 'Point changeZoom(Point pixelCoordinate, int oldZoom, int newZoom)'
	 * 
	 * @param mcc
	 *            insisting on Merator corner coordinate {@link #MP2Corner} (-> checked/modified input)
	 * @param newZoom
	 *                self-documenting 
	 * @return
	 *         Point 
	 */
	public static Point changeZoom(MP2Corner mcc,  int newZoom)
	{
		int x = xyChangeZoom(mcc.getX(), mcc.getZoom(), newZoom);
		int y = xyChangeZoom(mcc.getY(), mcc.getZoom(), newZoom);
		return new Point(x, y);
	}
	
	@Deprecated // often used
	public static Point changeZoom(Point pixelCoordinate, int oldZoom, int newZoom) //???Point-> MP2Coord
	{
		return changeZoom(new MP2Corner(pixelCoordinate.x, pixelCoordinate.y, oldZoom), newZoom);
	}
	
//	public static double horizontalDistance(int zoom, int y, int xDist)
//	{
//		return 1;
//	}

	@Deprecated
	public static double cXToLon(int x, int zoom) // old rules: no limitations
	{
		int sizeInPixel_UC = getSizeInPixel_UC(zoom);
		if (zoom < 0 || zoom > 22)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with zoom = " + zoom);
		if (x < 0)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with x = " + x + " (zoom = " + zoom + ")");
		if (x >= sizeInPixel_UC)
		{
			if (x == sizeInPixel_UC)
				log.error("DEPRECATED METHOD - called with x = getSizeInPixel_UC(zoom) = " + x + " (zoom = " + zoom + ") -> out of range for 'double cXToLon_Pixel...(int y, int zoom)'");
			else // x > sizeInPixel_UC
				log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with x = " + x + " (zoom = " + zoom + ")");
		}
		return Math.toDegrees(cXToRadian_UC(x, zoom));
	}
	
	@Deprecated
	public static double cYToLat(int y, int zoom) // old rules: no limitations, returns lat in { ~ -90 ... ~ 90 }
	{
		int sizeInPixel_UC = getSizeInPixel_UC(zoom);
		if (zoom < 0 || zoom > 22)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with zoom = " + zoom);
		if (y < 0)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with y = " + y + " (zoom = " + zoom + ")");
		if (y >= sizeInPixel_UC)
		{
			if (y == sizeInPixel_UC)
				log.error("DEPRECATED METHOD - called with x = getSizeInPixel_UC(zoom) = " + y + " (zoom = " + zoom + ") -> out of range for 'double cYToLat_Pixel...(int y, int zoom)'");
			else // (y > sizeInPixel_UC
				log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with y = " + y + " (zoom = " + zoom + ")");
		}
		return Math.toDegrees(cYToRadian_UC(y, zoom));
	}
	
	@Deprecated
	public static int cLonToX(double lon, int zoom) // old rules: no limitations to west, limitation to east (~ 180d), ignoring easternmost border
	{
		int x = cRadianToLeftXBorder(Math.toRadians(lon), zoom);
		int sizeInPixel_UC = getSizeInPixel_UC(zoom);
		if (zoom < 0 || zoom > 22)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with zoom = " + zoom);
		if (x < 0)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with longitude = " + lon);
		if (x >= sizeInPixel_UC)
		{
			x = Math.min(sizeInPixel_UC - 1, x);
			log.error("DEPRECATED METHOD - called with longitude = " + lon + " OUT OF RANGE -> output restricted to x = " + x + " (zoom = " + zoom + ")");
		}
		return x;
	}
	
	@Deprecated
	public static int cLatToY(double lat, int zoom) // old rules: input restricted to [MIN_LAT, MAX_LAT] (-> limitations to north, limitation to south), additional ignoring southernmost border
	{
		int sizeInPixel_UC = getSizeInPixel_UC(zoom);
		if (zoom < 0 || zoom > 22)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with zoom = " + zoom);
		if (lat < MIN_LAT || lat >= MAX_LAT)
		{
			log.error("DEPRECATED METHOD - called with latitude = " + lat + " OUT OF RANGE -> input restricted to [MIN_LAT, MAX_LAT]");
			lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat)); // to prevent log(0), div(0), ...
		}
		int y = cRadianToUpperYBorder(Math.toRadians(lat), zoom);
		if (y < 0)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with latitude = " + lat);
		if (y >= sizeInPixel_UC)
		{
			y = Math.min(sizeInPixel_UC - 1, y);
			log.error("DEPRECATED METHOD - called with latitude = " + lat + " OUT OF RANGE -> output restricted to y = " + y + " (zoom = " + zoom + ")");
		}
		return y;
	}
	
	
}
