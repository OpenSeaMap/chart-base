package osmb.mapsources;

import osmb.utilities.geo.GeoCoordinate;

/**
 * A map space pixel address. It is defined by x index, y index and zoom level.
 * - It is NOT the pixel. -
 * 
 * @author humbach
 */
public class MP2Pixel
{
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
	public MP2Pixel(int xIdx, int yIdx, int zoomLvl)
	{
		mX = xIdx;
		mY = yIdx;
		mZoom = zoomLvl;
	}

	/**
	 * @param corner
	 */
	public MP2Pixel(MP2Corner corner)
	{
		// TODO Auto-generated constructor stub
	}

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
	public MP2Tile getTileCoordinate()
	{
		return new MP2Tile(this);
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
}
