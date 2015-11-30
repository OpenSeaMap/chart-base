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
package osmb.mapsources.mapspace;

import java.awt.Point;

import osmb.program.map.IfMapSpace;

/**
 * Mercator projection with a world width and height of 256 * 2<sup>zoom</sup> pixel. This is the common projection used by OpenStreetMap and Google. It
 * provides methods to translate coordinates from 'map space' into latitude and longitude (on the WGS84 ellipsoid) and vice versa. Map space is measured in
 * pixels. The origin of the map space is the top left corner. The map space origin (0,0) has latitude ~85 and longitude -180.
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
 * {@link http://de.wikipedia.org/wiki/UTM-Koordinatensystem}
 * Longitude / Länge: (traditionell +Ost, −West) https://de.wikipedia.org/wiki/Geographische_Länge
 * Latitude / Breite: (traditionell +Nord, −Süd) https://de.wikipedia.org/wiki/Geographische_Breite
 * 
 * @see IfMapSpace
 */
public class MercatorPower2MapSpace implements IfMapSpace
{
	/**
	 * The northernmost border of map space (85.05112877980659).
	 */
	public static final double MAX_LAT = 85.05112877980659;
	/**
	 * The southernmost border of map space (-85.05112877980659).
	 */
	public static final double MIN_LAT = -85.05112877980659;
	protected final int tileSize;

	/**
	 * Pre-computed values for the world size (height respectively width) in the different zoom levels.
	 */
	protected final int[] worldSize;
	public static final IfMapSpace INSTANCE_256 = new MercatorPower2MapSpace(IfMapSpace.TECH_TILESIZE);

	/**
	 * this represents the size of one tile and the number of tiles for each zoom level
	 * 
	 * @param tileSize
	 */
	protected MercatorPower2MapSpace(int tileSize)
	{
		this.tileSize = tileSize;
		worldSize = new int[MAX_TECH_ZOOM + 1];
		for (int zoom = MIN_TECH_ZOOM; zoom < worldSize.length; zoom++)
			worldSize[zoom] = tileSize * (1 << zoom);
	}

	protected double radius(int zoom)
	{
		return getMaxPixels(zoom) / (2.0 * Math.PI);
	}

	@Override
	public ProjectionCategory getProjectionCategory()
	{
		return ProjectionCategory.SPHERE;
	}

	/**
	 * Returns the absolute number of pixels in y or x, defined as: 2<sup>zoom</sup> * <code>tileSize</code>
	 * 
	 * @param zoom
	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
	 * @return
	 */
	@Override
	public int getMaxPixels(int zoom)
	{
		return worldSize[zoom];
	}

	protected int falseNorthing(int aZoomlevel)
	{
		return (-1 * getMaxPixels(aZoomlevel) / 2);
	}

	/**
	 * Transforms latitude to pixelspace
	 * 
	 * @param lat
	 *          [-90...90]
	 * @param zoom
	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
	 * @return [0..2^zoom*tileSize[
	 * @author Jan Peter Stotz
	 */
	@Override
	public int cLatToY(double lat, int zoom)
	{
		lat = Math.max(MIN_LAT, Math.min(MAX_LAT, lat));
		double sinLat = Math.sin(Math.toRadians(lat));
		double log = Math.log((1.0 + sinLat) / (1.0 - sinLat));
		int mp = getMaxPixels(zoom);
		int y = (int) (mp * (0.5 - (log / (4.0 * Math.PI))));
		y = Math.min(y, mp - 1);
		return y;
	}

	/**
	 * Transform longitude to pixelspace
	 * 
	 * @param lon
	 *          [-180..180]
	 * @param zoom
	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
	 * @return [0..2^zoom * TILE_SIZE[
	 * @author Jan Peter Stotz
	 */
	@Override
	public int cLonToX(double lon, int zoom)
	{
		int mp = getMaxPixels(zoom);
		int x = (int) ((mp * (lon + 180l)) / 360l);
		x = Math.min(x, mp - 1);
		return x;
	}

	/**
	 * Transforms pixel coordinate X to longitude
	 * 
	 * @param x
	 *          [0..2^zoom * tileSize[
	 * @param zoom
	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM] (0..22 for tileSize = 256)
	 * @return ]-180..180[
	 * @author Jan Peter Stotz
	 */
	@Override
	public double cXToLon(int x, int zoom)
	{
		return ((360d * x) / getMaxPixels(zoom)) - 180.0;
	}

	/**
	 * Transforms pixel coordinate Y to latitude
	 * 
	 * @param y
	 *          [0..2^zoom * tileSize[
	 * @param zoom
	 *          [MIN_TECH_ZOOM..MAX_TECH_ZOOM]
	 * @return [MIN_LAT..MAX_LAT] is about [-85..85]
	 */
	@Override
	public double cYToLat(int y, int zoom)
	{
		y += falseNorthing(zoom);
		double latitude = (Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius(zoom))));
		return -1 * Math.toDegrees(latitude);
	}

	@Override
	public int getTileSize()
	{
		return tileSize;
	}

	@Override
	public int moveOnLatitude(int startX, int y, int zoom, double angularDist)
	{
		y += falseNorthing(zoom);
		double lat = -1 * ((Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * y / radius(zoom)))));

		double lon = cXToLon(startX, zoom);
		double sinLat = Math.sin(lat);

		lon += Math.toDegrees(Math.atan2(Math.sin(angularDist) * Math.cos(lat), Math.cos(angularDist) - sinLat * sinLat));
		int newX = cLonToX(lon, zoom);
		int w = newX - startX;
		return w;
	}

	@Override
	public double horizontalDistance(int zoom, int y, int xDist)
	{
		y = Math.max(y, 0);
		y = Math.min(y, getMaxPixels(zoom));
		double lat = cYToLat(y, zoom);
		double lon1 = -180.0;
		double lon2 = cXToLon(xDist, zoom);

		double dLon = Math.toRadians(lon2 - lon1);

		double cos_lat = Math.cos(Math.toRadians(lat));
		double sin_dLon_2 = Math.sin(dLon) / 2;

		double a = cos_lat * cos_lat * sin_dLon_2 * sin_dLon_2;
		return 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	@Override
	public int xChangeZoom(int x, int oldZoom, int newZoom)
	{
		int zoomDiff = oldZoom - newZoom;
		return (zoomDiff > 0) ? x >> zoomDiff : x << -zoomDiff;
	}

	@Override
	public int yChangeZoom(int y, int oldZoom, int newZoom)
	{
		int zoomDiff = oldZoom - newZoom;
		return (zoomDiff > 0) ? y >> zoomDiff : y << -zoomDiff;
	}

	@Override
	public Point changeZoom(Point pixelCoordinate, int oldZoom, int newZoom)
	{
		int x = xChangeZoom(pixelCoordinate.x, oldZoom, newZoom);
		int y = yChangeZoom(pixelCoordinate.y, oldZoom, newZoom);
		return new Point(x, y);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + tileSize;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MercatorPower2MapSpace other = (MercatorPower2MapSpace) obj;
		if (tileSize != other.tileSize)
			return false;
		return true;
	}
}
