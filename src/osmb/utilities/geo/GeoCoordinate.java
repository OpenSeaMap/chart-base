package osmb.utilities.geo;

import osmb.mapsources.MP2Corner;
import osmb.mapsources.MP2MapSpace;
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
	
	@Deprecated//? // #mapSpace ??? toCornerCoordinate ??? -> only used in unused!!! AddGpxTrackPolygonMap 
	public MP2Corner toPixelCoordinate(int zoom)
	{
		return new MP2Corner(MP2MapSpace.cLonToX_Pixel(lon, zoom), MP2MapSpace.cLatToY_Pixel(lat, zoom), zoom);
	}

// W #mapSpace ??? where used
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
