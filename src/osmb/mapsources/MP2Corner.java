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
 * Klasse soll wieder rausfliegen!!!
 * 
 * @author wilbert
 *
 */
public class MP2Corner
{
	private static final Logger log = Logger.getLogger(MP2Corner.class);

	private final int x;
	private final int y;
	private final int zoom;

	public MP2Corner(int x, int y, int zoom)
	{
		this.zoom = Math.max(MP2MapSpace.MIN_TECH_ZOOM, Math.min(zoom, MP2MapSpace.MAX_TECH_ZOOM));
		int sizeInPixel = MP2MapSpace.getSizeInPixel_UC(this.zoom);
		if (x < 0 || x > sizeInPixel)
		{
			log.error("OUT OF RANGE: called with x = " + x + " -> restricted!");
			x = Math.max(0, Math.min(x, sizeInPixel));
		}
		this.x = x;
		if (y < 0 || y > sizeInPixel)
		{
			log.error("OUT OF RANGE: called with y = " + y + " -> restricted!");
			y = Math.max(0, Math.min(y, sizeInPixel));
		}
		this.y = y;
	}

	public MP2Corner(MP2Pixel mpc)
	{
		this.zoom = mpc.getZoom();
		this.x = mpc.getX();
		this.y = mpc.getY();
	}

	/**
	 * 
	 * 
	 * @param lat
	 * @param lon
	 */
	public MP2Corner(double lat, double lon)
	{
		this(MP2MapSpace.cLonToX(lon, MP2MapSpace.MAX_TECH_ZOOM), MP2MapSpace.cLatToY(lat, MP2MapSpace.MAX_TECH_ZOOM), MP2MapSpace.MAX_TECH_ZOOM);
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getZoom()
	{
		return zoom;
	}

	public GeoCoordinate toGeoCoordinate()
	{
		double lon = MP2MapSpace.cXToLon(x, zoom);
		double lat = MP2MapSpace.cYToLat(y, zoom);
		return new GeoCoordinate(lat, lon);
	}

	// be careful
	public MP2Corner adaptToZoomlevel(int aZoomLevel)
	{
		aZoomLevel = Math.max(MP2MapSpace.MIN_TECH_ZOOM, Math.min(aZoomLevel, MP2MapSpace.MAX_TECH_ZOOM));
		return new MP2Corner(MP2MapSpace.xyChangeZoom_UC(x, zoom, aZoomLevel), MP2MapSpace.xyChangeZoom_UC(y, zoom, aZoomLevel), aZoomLevel);
	}
}
