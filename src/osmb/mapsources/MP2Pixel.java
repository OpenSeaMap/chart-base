package osmb.mapsources;

import osmb.utilities.geo.GeoCoordinate;

public class MP2Pixel
{
	private int mX = 0;
	private int mY = 0;
	private int mZoom = 0;

	public MP2Pixel(int xIdx, int yIdx, int zoomLvl)
	{
		mX = xIdx;
		mY = yIdx;
		mZoom = zoomLvl;
	}

	public MP2Pixel(MP2Corner mccMin_mZoom)
	{
		// TODO Auto-generated constructor stub
	}

	public int getZoom()
	{
		return mZoom;
	}

	public int getX()
	{
		return mX;
	}

	public int getY()
	{
		return mY;
	}

	public MP2Tile getTileCoordinate()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public GeoCoordinate toGeoUpperLeftCorner()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
