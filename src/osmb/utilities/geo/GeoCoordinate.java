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
package osmb.utilities.geo;

import osmb.mapsources.MP2Corner;
import osmb.mapsources.MP2MapSpace;
import osmb.mapsources.PixelAddress;
import osmb.utilities.OSMBUtilities;

public class GeoCoordinate
{
	/**
	 * used as @XmlAttribute see {@link #toCatalog()}
	 */
	public double lat;
	/**
	 * used as @XmlAttribute see {@link #toCatalog()}
	 */
	public double lon;

	public GeoCoordinate()
	{
		lat = Double.NaN;
		lon = Double.NaN;
	}
	
	public GeoCoordinate(double lat, double lon)
	{
		this.lat = lat;
		this.lon = lon;
	}
	
	public GeoCoordinate(MP2Corner mcc)
	{
		mcc.toGeoCoordinate();
	}
	
	// W #mapSpace ??? only used in unused!!! AddGpxTrackPolygonMap 
	public PixelAddress toPixelCoordinate(int zoom)
	{
		return new PixelAddress(MP2MapSpace.cLonToXIndex(lon, zoom), MP2MapSpace.cLatToYIndex(lat, zoom), zoom);
	}

	// W  ??? where used
	@Override
	public String toString()
	{
		return OSMBUtilities.prettyPrintLatLon(lat, true) + " " + OSMBUtilities.prettyPrintLatLon(lon, false);
	}
	
	/**
	 * This creates a string from the geographic coordinate.<br>
	 * 
	 * @return
	 *          the created string: "latitude, longitude" with formated coordinates.
	 */
	public String toCatalog()
	{
		// return "" + lat + ", " + lon; // /W
		return String.format(null, "%.8f, %.8f", new Object[]
		{ lat, lon });
	}
	
	
}
