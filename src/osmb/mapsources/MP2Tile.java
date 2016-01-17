package osmb.mapsources;

/**
 * A map tile address. It is defined by x index, y index and zoom level.
 * - It is NOT the tile. -
 * 
 * @author humbach
 */
public class MP2Tile
{
	private int mX = 0;
	private int mY = 0;
	private int mZoom = 0;

	/**
	 * This creates the address of the tile which the specified pixel contains.
	 * 
	 * @param pixel
	 */
	public MP2Tile(MP2Pixel pixel)
	{
		mX = pixel.getX() / MP2MapSpace.TECH_TILESIZE;
		mY = pixel.getY() / MP2MapSpace.TECH_TILESIZE;
		mZoom = pixel.getZoom();
	}

	/**
	 * @return The x index of this tile.
	 */
	public int getX()
	{
		return mX;
	}

	/**
	 * @return The y index of this tile.
	 */
	public int getY()
	{
		return mY;
	}

	/**
	 * @return The zoom level of this tile.
	 */
	public int getZoom()
	{
		return mZoom;
	}

	/**
	 * @return The address of the pixel at the upper left corner of this tile.
	 */
	public MP2Corner getUpperLeftCorner()
	{
		// TODO Auto-generated method stub
		return new MP2Corner(this);
	}
}
