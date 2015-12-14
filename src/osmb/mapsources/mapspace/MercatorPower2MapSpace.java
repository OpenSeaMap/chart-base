///*******************************************************************************
// * Copyright (c) OSMCB developers
// * 
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 2 of the License, or
// * (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ******************************************************************************/
//package osmb.mapsources.mapspace;
//
//import java.awt.Point;
//
//import org.apache.log4j.Logger;
//
//import osmb.program.map.IfMapSpace;
//
///**
// * Mercator projection with a world width and height of 256 * 2<sup>zoom</sup> pixel. This is the common projection used by OpenStreetMap and Google. It
// * provides methods to translate coordinates from 'map space' into latitude and longitude (on the WGS84 ellipsoid) and vice versa. Map space is measured in
// * pixels. The origin of the map space is the top left corner. The map space origin (0,0) has latitude ~85 and longitude -180.
// * this makes: xMin - west; yMin - north; xMax - east; yMax - south.
// * While the geo coordinates are independent of zoom level the map space is NOT. Map space coordinates, pixel and tiles, are depending on the zoom level for
// * the same geographic point.
// * 
// * <p>
// * This is the only implementation that is currently supported by OpenSeaMap ChartBundler.
// * </p>
// * 
// * Currently it supports a world up to zoom level 22 because of the use of 32bit integer for pixel coordinates.
// * 
// * {@link http://de.wikipedia.org/wiki/UTM-Koordinatensystem}
// * Longitude / Länge: (traditionell +Ost, −West) https://de.wikipedia.org/wiki/Geographische_Länge
// * Latitude / Breite: (traditionell +Nord, −Süd) https://de.wikipedia.org/wiki/Geographische_Breite
// * 
// * </p>
// * W 20151206<br>
// * added static members and methods to go round inconsistencies and to expand functionality<br>
// * see {@link #log Logger used instead of exceptions}
// * </p>
// * 
// * @see IfMapSpace
// */
//public class MercatorPower2MapSpace implements IfMapSpace
//{
//	/**
//	 * Logger used instead of exceptions!<br>
//	 * 
//	 * The input of the functions is restricted where required, the output is unrestricted!<br>
//	 * The functions map between (enlarged) double geographic coordinates and (enlarged) int map space coordinates.
//	 * The int coordinates and the double coordinates of the grids borders correlate to each other.
//	 * Apart these the longitudinal double coordinates are mapped to the neighboring left borders,
//	 * the latitudinal double coordinates are mapped to the neighboring upper borders.
//	 * 
//	 * @see #cLatToY_UR(double, int)
//	 * @see #cLonToX_UR(double, int)
//	 * @see #cXToLon_UR(int, int)
//	 * @see #cYToLat_UR(int, int)
//	 */
//	protected static Logger log = Logger.getLogger(MercatorPower2MapSpace.class);
//	
//	// static members and methods needed to expand functionality
//
//	// to prevent division by 0.0
//	private static final double MAX_TECH_LAT = 89.9d;
//	private static final double MIN_TECH_LAT = -89.9d;
//	
//	// to calculate: public int getMaxPixels(int zoom)
//	private static int getMaxPixels_UR(int zoom)
//	{
//		if (zoom < MIN_TECH_ZOOM || zoom > MAX_TECH_ZOOM)
//			log.error("called with zoom = " + zoom + " -> restricted!");
//		zoom = Math.min(Math.max(zoom, IfMapSpace.MIN_TECH_ZOOM), MAX_TECH_ZOOM);
//		
//		return TECH_TILESIZE * (1 << zoom);
//	}
//
//	// to calculate: protected int falseNorthing(int aZoomlevel)
//	private static int falseNorthing_UR(int aZoomlevel)
//	{
//		if (aZoomlevel < MIN_TECH_ZOOM || aZoomlevel > MAX_TECH_ZOOM)
//			log.error("called with zoom = " + aZoomlevel + " -> restricted!");
//		aZoomlevel = Math.min(Math.max(aZoomlevel, IfMapSpace.MIN_TECH_ZOOM), MAX_TECH_ZOOM);
//		
//		return (-1 * getMaxPixels_UR(aZoomlevel) / 2);
//	}
//
//	// to calculate: protected double radius(int zoom)
//	private static double radius_UR(int zoom)
//	{
//		if (zoom < MIN_TECH_ZOOM || zoom > MAX_TECH_ZOOM)
//			log.error("called with zoom = " + zoom + " -> restricted!");
//		zoom = Math.min(Math.max(zoom, IfMapSpace.MIN_TECH_ZOOM), MAX_TECH_ZOOM);
//		
//		return getMaxPixels_UR(zoom) / (2.0 * Math.PI);
//	}
//	
//	/**
//	 * Transforms (enlarged) latitude to (enlarged) map space border coordinate.<p>
//	 * 
//	 * lat = 0.0 maps to (getMaxPixels(zoom) / 2).<br>
//	 * Southern latitude maps to (positive) 'greater' int, northern to 'smaller' or negative int.
//	 * Apart the border coordinates a latitudinal double coordinate maps to the neighboring upper border.
//	 * 
//	 * @param lat
//	 *            restricted to double in [ -89.9 ... 89.9 ]
//	 * @param zoom
//	 *             restricted to int in { 0 ... 22 }
//	 * @return
//	 *         int in { ~ Integer.MIN_VALUE ... ~ Integer.MAX_VALUE }
//	 */
//	public static int cLatToY_UR(double lat, int zoom)
//	{
//		if (zoom < MIN_TECH_ZOOM || zoom > MAX_TECH_ZOOM)
//			log.error("called with zoom = " + zoom + " -> restricted!");
//		zoom = Math.min(Math.max(zoom, IfMapSpace.MIN_TECH_ZOOM), MAX_TECH_ZOOM);
//		if (lat < MIN_TECH_LAT || lat > MAX_TECH_LAT)
//			log.error("called with lat = " + lat + " -> restricted!");
//		lat = Math.max(MIN_TECH_LAT, Math.min(MAX_TECH_LAT, lat));
//		
//		double sinLat = Math.sin(Math.toRadians(lat));
//		double log = Math.log((1.0 + sinLat) / (1.0 - sinLat));
//		int mp = getMaxPixels_UR(zoom);
//		return (int) (mp * (0.5 - (log / (4.0 * Math.PI))));
//	}
//
//	/**
//	 * Transforms (enlarged) longitude to (enlarged) map space border coordinate.<p>
//	 * 
//	 * lon = 0.0 maps to (getMaxPixels(zoom) / 2).<br>
//	 * Eastern longitude maps to 'smaller' (or negative) int, western to (positive) 'greater' int.
//	 * Apart the border coordinates a longitudinal double coordinate maps to the neighboring left borders
//	 * 
//	 * @param lon
//	 *            double
//	 * @param zoom
//	 *             restricted to int in { 0 ... 22 }
//	 * @return
//	 *         int in { Integer.MIN_VALUE ... Integer.MAX_VALUE }
//	 */
//	public static int cLonToX_UR(double lon, int zoom)
//	{
//		if (zoom < MIN_TECH_ZOOM || zoom > MAX_TECH_ZOOM)
//			log.error("called with zoom = " + zoom + " -> restricted!");
//		zoom = Math.min(Math.max(zoom, IfMapSpace.MIN_TECH_ZOOM), MAX_TECH_ZOOM);
//		
//		int mp = getMaxPixels_UR(zoom);
//		return  (int) ((mp * (lon + 180l)) / 360l);
//	}
//	
//	/**
//	 * Transforms (enlarged) map space int (border) coordinate to (enlarged) longitudinal double border coordinate.<p>
//	 * 
//	 * x = 0 maps to -180.0 (West), x = getMaxPixels(zoom) maps to 180.0 (East)
//	 * 
//	 * @param x
//	 *          int
//	 * @param zoom
//	 *             restricted to int in { 0 ... 22 }
//	 * @return 
//	 *         double in: at least (zoom 22) { -900.0 ... ~ 540d }
//	 */
//	public static double cXToLon_UR(int x, int zoom)
//	{
//		if (zoom < MIN_TECH_ZOOM || zoom > MAX_TECH_ZOOM)
//			log.error("called with zoom = " + zoom + " -> restricted!");
//		zoom = Math.min(Math.max(zoom, IfMapSpace.MIN_TECH_ZOOM), MAX_TECH_ZOOM);
//		
//		return ((360d * x) / getMaxPixels_UR(zoom)) - 180.0;
//	}
//	
//	/**
//	 * Transforms (enlarged) map space int Y (border) coordinate to (enlarged) longitudinal double border coordinate.<p>
//	 * 
//	 * y = 0 maps to {@link #MAX_LAT 85.051...} (North), y = getMaxPixels(zoom) maps to {@link #MIN_LAT -85.051...} (South)
//	 * 
//	 * @param y
//	 *          int
//	 * @param zoom
//	 *             restricted to int in { 0 ... 22 }
//	 * @return 
//	 *         double in { ~ -90d (S) ... ~ 90d (N) }
//	 */
//	public static double cYToLat_UR(int y, int zoom)
//	{
//		if (zoom < MIN_TECH_ZOOM || zoom > MAX_TECH_ZOOM)
//			log.error("called with zoom = " + zoom + " -> restricted!");
//		zoom = Math.min(Math.max(zoom, IfMapSpace.MIN_TECH_ZOOM), MAX_TECH_ZOOM);
//		
//		y += falseNorthing_UR(zoom);
//		double latitude = (Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius_UR(zoom))));
//		return -1 * Math.toDegrees(latitude);
//	}
//	//////////////////////////////////////////////////////////////////////////////////////////////////////////
//	
//	/**
//	 * The northernmost border of map space (MAX_LAT = cYToLat_UR(0, zoom) = 85.05112877980659...).
//	 */
//	public static final double MAX_LAT = cYToLat_UR(0, 0); // 85.05112877980659;
//	/**
//	 * The southernmost border of map space (MIN_LAT = cYToLat_UR(getMaxPixels(zoom), zoom) = -85.05112877980659...).
//	 */
//	public static final double MIN_LAT = cYToLat_UR(TECH_TILESIZE, MIN_TECH_ZOOM); // -85.05112877980659;
//	protected final int tileSize;
//
//	/**
//	 * Pre-computed values for the world size (height respectively width) in the different zoom levels.
//	 */
//	protected final int[] worldSize;
//	public static final IfMapSpace INSTANCE_256 = new MercatorPower2MapSpace(IfMapSpace.TECH_TILESIZE);
//
//	/**
//	 * this represents the size of one tile and the number of tiles for each zoom level
//	 * 
//	 * @param tileSize
//	 */
//	// W ? tileSize has to be TECH_TILESIZE
//	protected MercatorPower2MapSpace(int tileSize)
//	{
//		this.tileSize = tileSize; // compare to: TECH_TILESIZE
//		worldSize = new int[MAX_TECH_ZOOM + 1];
//		for (int zoom = MIN_TECH_ZOOM; zoom < worldSize.length; zoom++) // compare to:  static int cWorldSize(int zoom): TECH_TILESIZE * (1 << zoom);
//			worldSize[zoom] = tileSize * (1 << zoom);
//	}
//
//	protected double radius(int zoom) // compare to: private static double radius_UR(int zoom)
//	{
//		return getMaxPixels(zoom) / (2.0 * Math.PI);
//	}
//
//	@Override
//	public ProjectionCategory getProjectionCategory()
//	{
//		return ProjectionCategory.SPHERE;
//	}
//
//	/**
//	 * Returns the absolute number of pixels in y or x, defined as: 2<sup>zoom</sup> * <code>tileSize</code>
//	 * 
//	 * @param zoom
//	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
//	 * @return
//	 */
//	@Override
//	public int getMaxPixels(int zoom) // compare to: private static int getMaxPixels_UR(int zoom)
//	{
//		return worldSize[zoom];
//	}
//
//	protected int falseNorthing(int aZoomlevel) // compare to: private static int falseNorthing_UR(int aZoomlevel)
//	{
//		return (-1 * getMaxPixels(aZoomlevel) / 2);
//	}
//
//	/**
//	 * Transforms latitude to pixelspace
//	 * 
//	 * @param lat
//	 *          [-85...85] - is south
//	 * @param zoom
//	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
//	 * @return [0..2^zoom*tileSize -1]
//	 * @author humbach based on code from Jan Peter Stotz
//	 */
//	@Override
//	public int cLatToY(double lat, int zoom)
//	{
//		if ((lat < MIN_LAT) || (lat > MAX_LAT))
//			log.error("latitude out of range=" + lat);
//		if ((zoom < IfMapSpace.MIN_TECH_ZOOM) || (zoom > IfMapSpace.MAX_TECH_ZOOM))
//			log.error("zoom out of range=" + zoom);
//		// lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat)); // restrictions to negative int: lat <= MAX_LAT; restrictions to int > mp : lat >= MIN_LAT
//		// double sinLat = Math.sin(Math.toRadians(lat));
//		// double log = Math.log((1.0 + sinLat) / (1.0 - sinLat));
//		// int mp = getMaxPixels(zoom);
//		// int y = (int) (mp * (0.5 - (log / (4.0 * Math.PI))));
//		// y = Math.min(y, mp - 1); // W why (mp - 1)? toPixelCoord!?
//		// return y;
//		
//		lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat));
//		int mp = getMaxPixels(zoom);
//		return Math.min(cLatToY_UR(lat, zoom), mp - 1);
//	}
//
//	/**
//	 * Transform longitude to pixelspace
//	 * 
//	 * @param lon
//	 *          [-180..180] - is west
//	 * @param zoom
//	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
//	 * @return [0..2^zoom * TILE_SIZE -1] <br>
//	 *          W if no limitation to param lon: return int in { Integer.MIN_VALUE ... 2<sup>zoom + 8</sup> - 1 }
//	 * @author humbach based on code from Jan Peter Stotz
//	 */
//	@Override
//	public int cLonToX(double lon, int zoom)
//	{
//		if ((lon < -180) || (lon >= 180))
//			log.error("longitude out of range=" + lon);
//		if ((zoom < IfMapSpace.MIN_TECH_ZOOM) || (zoom >= IfMapSpace.MAX_TECH_ZOOM))
//			log.error("zoom out of range=" + zoom);
//		lon = Math.max(-180, Math.min(180, lon)); // xxx = 180???
//		// int mp = getMaxPixels(zoom);
//		// int x = (int) ((mp * (lon + 180l)) / 360l);
//		
//		int x = cLonToX_UR(lon, zoom);
//		int mp = getMaxPixels(zoom);
//		x = Math.max(0, Math.min(x, mp - 1));
//		return x;
//	}
//
//	/**
//	 * Transforms pixel coordinate X to longitude of the left pixel border
//	 * 
//	 * @param x
//	 *          [0..2^zoom * tileSize[
//	 * @param zoom
//	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
//	 * @return ]-180..180[
//	 *          W if no limitation to param x: return double in: at least (zoom 22) { -900.0 ... ~ 540d }
//	 * @author Jan Peter Stotz
//	 */
//	@Override
//	public double cXToLon(int x, int zoom)
//	{
//		if ((zoom < IfMapSpace.MIN_TECH_ZOOM) || (zoom > IfMapSpace.MAX_TECH_ZOOM))
//			log.error("zoom out of range=" + zoom);
//		// return ((360d * x) / getMaxPixels(zoom)) - 180.0;
//		
//		return cXToLon_UR(x, zoom);
//	}
//
//	/**
//	 * Transforms pixel coordinate Y to latitude of upper pixel border
//	 * 
//	 * @param y
//	 *          [0..2^zoom * tileSize[
//	 * @param zoom
//	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM]
//	 * @return [MIN_LAT..MAX_LAT] is about [-85..85]<br>
//	 *          W if no limitation to param y: return double in { ~ -90d ...  ~ 90d}
//	 */
//	@Override
//	public double cYToLat(int y, int zoom)
//	{
//		if ((zoom < IfMapSpace.MIN_TECH_ZOOM) || (zoom > IfMapSpace.MAX_TECH_ZOOM))
//			log.error("zoom out of range=" + zoom);
//		// y += falseNorthing(zoom);
//		// double latitude = (Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius(zoom))));
//		// return -1 * Math.toDegrees(latitude);
//		
//		return cYToLat_UR(y, zoom);
//	}
//
//	@Override
//	public int getTileSize()
//	{
//		return tileSize;
//	}
//
//	// TODO test method before using
//	@Override
//	public int moveOnLatitude(int startX, int y, int zoom, double angularDist)
//	{
//		//double lat = cYToRadian(y, zoom)
//		y += falseNorthing(zoom);
//		double lat = -1 * ((Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius(zoom)))));
//
//		double lon = cXToLon(startX, zoom);
//		double sinLat = Math.sin(lat);
//
//		lon += Math.toDegrees(Math.atan2(Math.sin(angularDist) * Math.cos(lat), Math.cos(angularDist) - sinLat * sinLat));
//		int newX = cLonToX(lon, zoom);
//		int w = newX - startX;
//		return w;
//	}
//
//	@Override
//	public double horizontalDistance(int zoom, int y, int xDist)
//	{
//		y = Math.max(y, 0);
//		y = Math.min(y, getMaxPixels(zoom));
//		double lat = cYToLat(y, zoom);
//		double lon1 = -180.0;
//		double lon2 = cXToLon(xDist, zoom);
//
//		double dLon = Math.toRadians(lon2 - lon1);
//
//		double cos_lat = Math.cos(Math.toRadians(lat));
////		double sin_dLon_2 = Math.sin(dLon) / 2;
////
////		double a = cos_lat * cos_lat * sin_dLon_2 * sin_dLon_2;
////		return 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//		
//		return dLon * cos_lat;
//	}
//
//	@Override
//	public int xChangeZoom(int x, int oldZoom, int newZoom)
//	{
//		int zoomDiff = oldZoom - newZoom;
//		return (zoomDiff > 0) ? x >> zoomDiff : x << -zoomDiff;
//	}
//
//	@Override
//	public int yChangeZoom(int y, int oldZoom, int newZoom)
//	{
//		int zoomDiff = oldZoom - newZoom;
//		return (zoomDiff > 0) ? y >> zoomDiff : y << -zoomDiff;
//	}
//
//	@Override
//	public Point changeZoom(Point pixelCoordinate, int oldZoom, int newZoom)
//	{
//		int x = xChangeZoom(pixelCoordinate.x, oldZoom, newZoom);
//		int y = yChangeZoom(pixelCoordinate.y, oldZoom, newZoom);
//		return new Point(x, y);
//	}
//
//	@Override
//	public int hashCode()
//	{
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + tileSize;
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj)
//	{
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		MercatorPower2MapSpace other = (MercatorPower2MapSpace) obj;
//		if (tileSize != other.tileSize)
//			return false;
//		return true;
//	}
//}
