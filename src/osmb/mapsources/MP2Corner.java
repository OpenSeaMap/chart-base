package osmb.mapsources;

import org.apache.log4j.Logger;

import osmb.utilities.geo.GeoCoordinate;

// W #mapspace ??? MP2PixelCoo / MP2PixelCenter
public class MP2Corner
{
	private static final Logger log = Logger.getLogger(MP2Corner.class);
	
	private final int x;
	private final int y;
	private final int zoom;
	
	public MP2Corner(int x, int y, int zoom)
	{
		this.zoom = MP2MapSpace.checkZoom(zoom, "MP2Corner(int x, int y, int zoom)");
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
	
	/**
	 * 
	 * 
	 * @param lat
	 * @param lon
	 */
	public MP2Corner(double lat, double lon)
	{
		this(MP2MapSpace.cLonToX_Borders(lon, MP2MapSpace.MAX_TECH_ZOOM) , MP2MapSpace.cLatToY_Borders(lat, MP2MapSpace.MAX_TECH_ZOOM), MP2MapSpace.MAX_TECH_ZOOM);
	}
	
	// other constructors: GeoCoo NWCoo NECoo SWCoo SECoo ???
	
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
	
	// W #mapspace ??? ..._Borders / ..._PixelCoo / ..._PixelCenter
	public GeoCoordinate toGeoCoordinate()
	{
		double lon = MP2MapSpace.cXToLon_Borders(x, zoom);
		double lat = MP2MapSpace.cYToLat_Borders(y, zoom);
		return new GeoCoordinate(lat, lon);
	}

	public MP2Corner adaptToZoomlevel(int aZoomLevel)
	{
		aZoomLevel = MP2MapSpace.checkZoom(aZoomLevel, "MP2Corner adaptToZoomlevel(int aZoomLevel)");
		return new MP2Corner(MP2MapSpace.xyChangeZoom(x, zoom, aZoomLevel), MP2MapSpace.xyChangeZoom(y, zoom, aZoomLevel), aZoomLevel);
	}
}
