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

// W TODO revise class comment
/**
 * Mercator projection (globe <-> map space) with a map space width and height of {@value #TECH_TILESIZE} * 2<sup>zoom</sup> pixel.<br>
 * 
 * This is the common projection used by OpenStreetMap and Google.<br>
 * It provides methods to translate coordinates from 'map space' into latitude and longitude (on the WGS84 ellipsoid) and vice versa.<br>
 * Map space is measured in nonnegative integer coordinates, representing pixel or corners.
 * The origin of the map space is the coordinate ( 0 | 0 ) of the top left pixel, respectively the top left corner of the pixel.
 * This makes: xMin (0) is west (-180d) ; yMin (0) is north (85.0511...)
 * <ul>
 * Differentiations: (x/y/u/v integers, lat/lon doubles)
 * <li>Border coordinates: Every pixel ( x | y ) has four borders ( x: western, x + 1: eastern, y: northern, y + 1: southern ).<br>
 * The mapping x <-> lon / y <-> lat is within the limits of rounding.
 * <li>Pixel coordinate: ( x | y ) coordinate with x / y in { 0 ... {@value #TECH_TILESIZE} * 2<sup>zoom</sup> - 1 }.<br>
 * The mapping x <-> lon / y <-> lat corresponds to the top left (north west) corner of the pixel.
 * <li>Pixel center: ( lat | lon ) coordinate representing the center of a pixel.
 * <li>TileCoordinate: ( x | y ) coordinate with x / y in { 0 ... 2<sup>zoom</sup> - 1 }.<br>
 * A pixel ( u | v ) belongs to tile ( x | y ) with x = u / {@value #TECH_TILESIZE} and y = v / {@value #TECH_TILESIZE}.
 * </ul>
 * While the geo coordinates are independent of zoom level the map space is NOT.
 * Map space coordinates, pixel and tiles, are depending on the zoom level for the same geographic point.
 * 
 * <p>
 * This is the only implementation that is currently supported by OpenSeaMap ChartBundler.
 * </p>
 * 
 * Currently it supports a world up to zoom level {@value #MAX_TECH_ZOOM} because of the use of 32bit integer for pixel coordinates.
 * 
 * <ul>
 * Determinations:
 * <li>{@link #TECH_TILESIZE} = {@value #TECH_TILESIZE} = 2<sup>8</sup></li>
 * <li>{@link #MAX_TECH_ZOOM} = {@value #MAX_TECH_ZOOM}</li>
 * <li>{@link #MIN_TECH_ZOOM} = {@value #MIN_TECH_ZOOM}</li>
 * <li>{@link #MAX_LAT} = 85.0511...° North</li>
 * <li>{@link #MIN_LAT} = -85.0511...° South</li>
 * <li>-180° (West) == 180° (East) <-> longitudinal cut</li>
 * </ul>
 * 
 * @author wilbert
 */
public class MP2MapSpace
{
	private static final Logger log = Logger.getLogger(MP2MapSpace.class);

	/**
	 * These are the software technical limits because we use an int for the pixel coordinates. In zoom level 23 the coordinate would exceed the int...
	 * If we will want to provide higher zoom levels, we have to change the data type used to 64bit int... This is a lot of work, so we do it not (now).
	 * <p>
	 * see {@link #TECH_TILESIZE} and {@link #MAX_TECH_ZOOM}
	 * <p>
	 * 
	 * The value is <code>MIN_TECH_ZOOM = {@value}</code>
	 */
	public static final int MIN_TECH_ZOOM = 0;
	/**
	 * The value is <code>MAX_TECH_ZOOM = {@value}</code>
	 */
	public static final int MAX_TECH_ZOOM = 22;
	/**
	 * The value is <code>TECH_TILESIZE = {@value}</code> = 2<sup>8</sup>
	 */
	public static final int TECH_TILESIZE = 256;

	/**
	 * The northernmost border of map space.<br>
	 * 
	 * (MAX_LAT = cYToLatUpperBorder(0, zoom) = 85.05112877980659...)
	 */
	public static final double MAX_LAT = cYToLatUpperBorder(0, MIN_TECH_ZOOM);
	/**
	 * The southernmost border of map space.<br>
	 * 
	 * (MIN_LAT = cYToLatLowerBorder(getMaxPixels(zoom) - 1, zoom) = -85.05112877980659...)
	 */
	public static final double MIN_LAT = cYToLatLowerBorder(TECH_TILESIZE - 1, MIN_TECH_ZOOM);

	/**
	 * Calculates horizontal pixel index x to longitude of the left border of the pixel.<br>
	 * 
	 * Private access because of unchecked parameters!
	 * 
	 * @param x
	 *          The horizontal pixel index, not checked by this method!
	 * @param zoom
	 *          The zoom level, not checked by this method!
	 * @return
	 *         The longitude of the left border of the pixel.
	 */
	private static double cXToLongitude(int x, int zoom)
	{
		return ((360.0 * x) / getSizeInPixel(zoom)) - 180.0;
	}

	/**
	 * Calculates vertical pixel index y to 'latitude' in radian of the upper border of the pixel.<br>
	 * 
	 * Private access because of UnChecked parameters!
	 * 
	 * @param y
	 *          The vertical pixel index, not checked by this method!
	 * @param zoom
	 *          The zoom level, not checked by this method!
	 * @return The 'latitude' in radian of the upper border of the pixel.
	 */
	private static double cYToLatitudeRadian(int y, int zoom)
	{
		y -= getSizeInPixel_UC(zoom) / 2; // set equator to 0
		// The radius of the globe in equatorial pixel: getSizeInPixel_UC(zoom) / (2.0 * Math.PI)
		double arg = -1.0 * y / (getSizeInPixel_UC(zoom) / (2.0 * Math.PI));
		return 2.0 * Math.atan(Math.exp(arg)) - (Math.PI / 2.0); // Gudermannian function
	}

	/**
	 * Calculates vertical pixel index y to latitude of the upper border of the pixel.<br>
	 * 
	 * Private access because of UnChecked parameters!
	 * 
	 * @param y
	 *          The vertical pixel index, not checked by this method!
	 * @param zoom
	 *          The zoom level, not checked by this method!
	 * @return The latitude of the upper border of the pixel.
	 */
	private static double cYToLatitude(int y, int zoom)
	{
		return Math.toDegrees(cYToLatitudeRadian(y, zoom));
	}

	/**
	 * Maps longitude to horizontal index x of the pixel, that includes the longitudinal coordinate.<br>
	 * 
	 * Private access because of UnChecked parameters!
	 * 
	 * @param lon
	 *          Longitude.
	 * @param zoom
	 *          The zoom level, not checked by this method!
	 * @return
	 *         The pixel index x.
	 */
	private static int cLongitudeToXIndex(double lon, int zoom)
	{
		return (int) ((getSizeInPixel_UC(zoom) * (lon + 180.0)) / 360.0);
	}

	/**
	 * Maps latitude to vertical index y of the pixel, that includes the latitudinal coordinate.<br>
	 * 
	 * Private access because of UnChecked parameters!<br>
	 * Check parameters in calling methods! If unchecked exceptions by: log(0), div(0), ...
	 * 
	 * @param lon
	 *          Latitude.
	 * @param zoom
	 *          The zoom level, not checked by this method!
	 * @return
	 *         The pixel index y.
	 */
	private static int cLatitudeToYIndex(double lat, int zoom)
	{
		// Mercator projection with:
		// The radius of the globe in equatorial pixel: getSizeInPixel_UC(zoom) / (2.0 * Math.PI)
		// Setting equator to 0 -> y -= getSizeInPixel_UC(zoom) / 2;
		double sinLat = Math.sin(Math.toRadians(lat));
		double log = Math.log((1.0 + sinLat) / (1.0 - sinLat));
		return (int) (getSizeInPixel_UC(zoom) * (0.5 - (log / (4.0 * Math.PI))));
	}

	/**
	 * The size (width or height) of the map space in pixel depending on (UnChecked) zoom level.<br>
	 * 
	 * There are 2<sup>zoom</sup> tiles in width and height. Each tile is {@value #TECH_TILESIZE} pixels wide.
	 * 
	 * @param zoom
	 *          The parameter zoom has to be checked before using this method!
	 * @return
	 *         2<sup>zoom + 8</sup>
	 */
	protected static int getSizeInPixel_UC(int zoom)
	{
		return TECH_TILESIZE * (1 << zoom);
	}

	/**
	 * Shifting intput values depending on difference of (UnChecked) zoom levels.
	 * 
	 * @param xy
	 *          Horizontal or vertical distance in pixel
	 * @param oldZoom
	 *          The parameter oldZoom has to be checked before using this method!
	 * @param newZoom
	 *          The parameter newZoom has to be checked before using this method!
	 * @return
	 *         Shifted value of xy
	 */
	protected static int xyChangeZoom_UC(int xy, int oldZoom, int newZoom)
	{
		int zoomDiff = oldZoom - newZoom;
		return (zoomDiff > 0) ? xy >> zoomDiff : xy << -zoomDiff;
	}

	/**
	 * @return
	 *         The size (height or width) of each tile in pixel. A tile is a square in pixels.<br>
	 *         Currently we support only tiles with 256 x 256 pixels.<br>
	 *         see {@link #TECH_TILESIZE}.
	 */
	public static int getTileSize()
	{
		return TECH_TILESIZE;
	}

	/**
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return The size (width or height) of the world in pixels for the specified zoom level.
	 */
	public static int getSizeInPixel(int zoom)
	{
		return TECH_TILESIZE * (1 << (Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM))));
	}

	/**
	 * This returns the geographic coordinate of the left border of the specified pixel.
	 * 
	 * @param x
	 *          The horizontal pixel index. ( restricted to x in { 0 ... 2<sup>zoom + 8</sup> - 1 } )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return The longitude of the left border of the pixel. ( -180d (W) <= double < 180d (E) )
	 */
	public static double cXToLonLeftBorder(int x, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		x = Math.max(0, Math.min(x, getSizeInPixel(checkedZoom) - 1));
		return cXToLongitude(x, checkedZoom);
	}

	/**
	 * This returns the geographic coordinate of the right border of the specified pixel.
	 * 
	 * @param x
	 *          The horizontal pixel index. ( restricted to x in { 0 ... 2<sup>zoom + 8</sup> - 1 } )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return The longitude of the right border of the pixel. ( -180d (W) < double <= 180d (E) )
	 */
	public static double cXToLonRightBorder(int x, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		x = Math.max(0, Math.min(x, getSizeInPixel(checkedZoom) - 1)) + 1;
		return cXToLongitude(x, checkedZoom);
	}

	/**
	 * This returns the geographic coordinate of the horizontal center of the specified pixel.
	 * 
	 * @param x
	 *          The horizontal pixel index. ( restricted to x in { 0 ... 2<sup>zoom + 8</sup> - 1 } )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return The longitude of the center of the pixel. ( -180d (W) < double < 180d (E) )
	 */
	public static double cXToLonPixelCenter(int x, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		x = Math.max(0, Math.min(x, getSizeInPixel(checkedZoom) - 1));
		return (cXToLongitude(x, checkedZoom) + cXToLongitude(x + 1, checkedZoom)) / 2;
	}

	/**
	 * This returns the geographic coordinate of the upper border of the specified pixel.
	 * 
	 * @param y
	 *          The vertical pixel index. ( restricted to y in { 0 ... 2<sup>zoom + 8</sup> - 1 } )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return The latitude of the upper border of the pixel. ( {@link #MIN_LAT} (S) < double <= {@link #MAX_LAT} (N) )
	 */
	public static double cYToLatUpperBorder(int y, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		y = Math.max(0, Math.min(y, getSizeInPixel(checkedZoom) - 1));
		return cYToLatitude(y, checkedZoom);
	}

	/**
	 * This returns the geographic coordinate of the lower border of the specified pixel.
	 * 
	 * @param y
	 *          The vertical pixel index. ( restricted to y in { 0 ... 2<sup>zoom + 8</sup> - 1 } )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return The latitude of the lower border of the pixel. ( {@link #MIN_LAT} (S) <= double < {@link #MAX_LAT} (N) )
	 */
	public static double cYToLatLowerBorder(int y, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		y = Math.max(0, Math.min(y, getSizeInPixel(checkedZoom) - 1)) + 1;
		return cYToLatitude(y, checkedZoom);
	}

	/**
	 * This returns the geographic coordinate of the vertical center of the specified pixel.
	 * 
	 * @param y
	 *          The vertical pixel index. ( restricted to y in { 0 ... 2<sup>zoom + 8</sup> - 1 } )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return The latitude of the center of the pixel. ( {@link #MIN_LAT} (S) < double < {@link #MAX_LAT} (N) )
	 */
	public static double cYToLatPixelCenter(int y, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		y = Math.max(0, Math.min(y, getSizeInPixel(checkedZoom) - 1));
		return (cYToLatitude(y, checkedZoom) + cYToLatitude(y + 1, checkedZoom)) / 2;
		// W not exact! exact: return cYToLatitude(2 * y + 1, checkedZoom + 1); // but getSizeInPixel_UC(23) = 2³¹ > Integer.MAX_VALUE = 2³¹ - 1
	}

	/**
	 * Converts longitude to the horizontal index x of the pixel, that includes the longitudinal coordinate, depending on zoom level.<br>
	 * 
	 * @param lon
	 *          The longitude in degrees. ( restricted by -180d (W) <= lon < 180d (E) } )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return
	 *         The horizontal pixel index for the specified zoom level. ( { 0 ... 2<sup>zoom + 8</sup> - 1 } )
	 */
	public static int cLonToXIndex(double lon, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		int x = cLongitudeToXIndex(lon, checkedZoom);
		return Math.max(0, Math.min(getSizeInPixel(checkedZoom) - 1, x));
	}

	/**
	 * Converts latitude to the vertical index y of the pixel, that includes the latitudinal coordinate, depending on zoom level.<br>
	 * 
	 * @param lat
	 *          The Latitude in degrees. ( restricted by {@link #MIN_LAT} < lat <= {@link #MAX_LAT} )
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @return
	 *         The vertical pixel index for the specified zoom level. ( { 0 ... 2<sup>zoom + 8</sup> - 1 })
	 */
	public static int cLatToYIndex(double lat, int zoom)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat)); // to prevent log(0), div(0), ...
		return Math.max(0, Math.min(getSizeInPixel(checkedZoom) - 1, cLatitudeToYIndex(lat, checkedZoom))); // restriction to pixel index
	}

	/**
	 * Calculates distance on a latitude from longitude = -180° eastwards to by xDist given longitude in units of earth's radius.<br>
	 * 
	 * For the equator the horizontalDistance is the angular distance, e.g. horizontalDistance(0, sizeInPixel / 2, 255) ~= 2 * PI.<br>
	 * 
	 * @param zoom
	 *          The zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= zoom <= {@value #MAX_TECH_ZOOM} )
	 * @param y
	 *          The vertical pixel index, whose upper border specifies the latitude to measure on. (will be rounded to y in { 0 ... sizeInPixel })
	 * @param xDist
	 *          Distance in pixel on the x-axis (will be rounded to x in { 0 ... sizeInPixel - 1 })
	 * @return
	 *         Distance / earth's radius (e.g. 6367.5 km or 3956.6 miles, thus independent of unit system)
	 */
	public static double horizontalDistance(int zoom, int y, int xDist)
	{
		int checkedZoom = Math.max(MIN_TECH_ZOOM, Math.min(zoom, MAX_TECH_ZOOM));
		int sizeInPixel = getSizeInPixel_UC(checkedZoom);
		y = Math.max(0, Math.min(y, sizeInPixel));
		xDist = Math.max(0, Math.min(xDist, sizeInPixel - 1));
		double deltaLongitudeRadian = Math.toRadians(cXToLongitude(xDist, checkedZoom) + 180.0);
		double cos_lat = Math.cos(cYToLatitudeRadian(y, checkedZoom));
		// log.debug("cos_lat=" + cos_lat);
		// log.debug("deltaLongitudeRadian * cos_lat=" + deltaLongitudeRadian * cos_lat);
		return deltaLongitudeRadian * cos_lat;
	}

	// W TODO split changeZoom
	/**
	 * Changes coordinates by shifting intput values depending on new zoom level.<br>
	 * 
	 * The old zoom level is given by MP2Corner, using {@link MP2Corner#adaptToZoomlevel(int)} may be more clearly.<br>
	 * Written to satisfy often used {@link #changeZoom(Point, int, int)}.
	 * 
	 * @param mcc
	 *          insisting on Mercator corner coordinate {@link #MP2Corner} ( -> checked / modified input )
	 * @param newZoom
	 *          The new zoom level is restricted by this method. ( {@value #MIN_TECH_ZOOM} <= newZoom <= {@value #MAX_TECH_ZOOM} )
	 * @return
	 *         Point, thus no zoom information.
	 */
	public static Point changeZoom(MP2Corner mcc, int newZoom)
	{
		int checkedNewZoom = Math.max(MIN_TECH_ZOOM, Math.min(newZoom, MAX_TECH_ZOOM));
		int x = xyChangeZoom_UC(mcc.getX(), mcc.getZoom(), checkedNewZoom);
		int y = xyChangeZoom_UC(mcc.getY(), mcc.getZoom(), checkedNewZoom);
		return new Point(x, y);
	}

	@Deprecated
	public static Point changeZoom(Point pixelCoordinate, int oldZoom, int newZoom) // often used
	{
		return changeZoom(new MP2Corner(pixelCoordinate.x, pixelCoordinate.y, oldZoom), newZoom);
	}

	/**
	 * Deprecated method because there are no limitations.<br>
	 * 
	 * Use {@link #cXToLonLeftBorder(x, zoom)}, {@link #cXToLonRightBorder(x, zoom)} or {@link #cXToLonPixelCenter(x, zoom)} instead of this.
	 * Calculates, if x and zoom are in range, horizontal pixel index x to longitude of the left border of the pixel.<br>
	 * 
	 * @param x
	 * @param zoom
	 * @return
	 */
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
				log.error("DEPRECATED METHOD - called with x = getSizeInPixel_UC(zoom) = " + x + " (zoom = " + zoom
				    + ") -> out of range for 'double cXToLon_Pixel...(int y, int zoom)'");
			else // x > sizeInPixel_UC
				log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with x = " + x + " (zoom = " + zoom + ")");
		}
		return cXToLongitude(x, zoom);
	}

	/**
	 * Deprecated method because there are no limitations.<br>
	 * 
	 * Use {@link #cYToLatUpperBorder(y, zoom)}, {@link #cYToLatLowerBorder(y, zoom)} or {@link #cYToLatPixelCenter(y, zoom)} instead of this.
	 * Calculates, if y and zoom are in range, vertical pixel index y to latitude of the upper border of the pixel.<br>
	 * 
	 * @param y
	 * @param zoom
	 * @return
	 */
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
				log.error("DEPRECATED METHOD - called with x = getSizeInPixel_UC(zoom) = " + y + " (zoom = " + zoom
				    + ") -> out of range for 'double cYToLat_Pixel...(int y, int zoom)'");
			else // (y > sizeInPixel_UC
				log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with y = " + y + " (zoom = " + zoom + ")");
		}
		return cYToLatitude(y, zoom);
	}

	/**
	 * Deprecated method because there are varying limitations .<br>
	 *
	 * Use {@link #cLonToXIndex(x, zoom)} instead of this.
	 * Converts, if x and zoom are in range, longitude to the horizontal index x of the pixel, that includes the longitudinal coordinate, depending on zoom
	 level.
	 * <br>
	 *
	 * @param lon
	 * @param zoom
	 * @return
	 */
	 @Deprecated
	public static int cLonToX(double lon, int zoom) // old rules: no limitations to west, limitation to east (~ 180d), ignoring easternmost border
	{
		int x = cLongitudeToXIndex(lon, zoom);
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

	/**
	 * Deprecated method because there is no zoom limitation .<br>
	 *
	 * Use {@link #cLatToYIndex(y, zoom)} instead of this.
	 * Converts, if zoom is in range, latitude to the vertical index y of the pixel, that includes the latitudinal coordinate depending on zoom level.<br>
	 *
	 * @param lat
	 * @param zoom
	 * @return
	 */
	 @Deprecated
	public static int cLatToY(double lat, int zoom) // old rules: input restricted to [MIN_LAT, MAX_LAT] (-> limitations to north, limitation to south),
	                                                // additional ignoring southernmost border
	{
		int sizeInPixel_UC = getSizeInPixel_UC(zoom);
		if (zoom < 0 || zoom > 22)
			log.error("DEPRECATED METHOD - OUT OF RANGE_NEW: called with zoom = " + zoom);
		if (lat < MIN_LAT || lat > MAX_LAT)
		{
			log.error("DEPRECATED METHOD - called with latitude = " + lat + " OUT OF RANGE -> input restricted to [MIN_LAT, MAX_LAT]");
			lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat)); // to prevent log(0), div(0), ...
		}
		int y = cLatitudeToYIndex(lat, zoom);
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
