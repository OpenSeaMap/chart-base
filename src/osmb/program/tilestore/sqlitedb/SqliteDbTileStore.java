package osmb.program.tilestore.sqlitedb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;

import osmb.mapsources.IfMapSource;
import osmb.program.tilestore.ACSiTileStore;
import osmb.program.tilestore.IfTileStoreEntry;
import osmb.program.tilestore.TileStoreInfo;

/**
 * This class is intended to replace the 'old' berkelydb tilestore. It is not a singleton any longer.
 * It is neccessary to have several tile stores open at the same time.
 * This is valid for one app, as well as for different apps.
 * 
 * The class holds an enumeration of all instances.
 * 
 * @author humbach
 */
public class SqliteDbTileStore extends ACSiTileStore
{
	/**
	 * Runs over all map source stores in this tile store instance.
	 */
	@Override
	public void closeAll()
	{
		// TODO Auto-generated method stub
	}

	/**
	 * This clears the whole tile store.
	 */
	@Override
	public void clearStore(String storeName)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void putTileData(byte[] tileData, int x, int y, int zoom, IfMapSource mapSource) throws IOException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void putTileData(byte[] tileData, int x, int y, int zoom, IfMapSource mapSource, long timeLastModified, long timeExpires, String eTag)
	    throws IOException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public IfTileStoreEntry getTile(int x, int y, int zoom, IfMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(int x, int y, int zoom, IfMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void prepareTileStore(IfMapSource mapSource)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public String[] getAllStoreNames()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean storeExists(IfMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TileStoreInfo getStoreInfo(String mapSourceName) throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage getCacheCoverage(IfMapSource mapSource, int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putTile(IfTileStoreEntry tile, IfMapSource mapSource)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public IfTileStoreEntry createNewEntry(int x, int y, int zoom, byte[] data, long timeLastModified, long timeExpires, String eTag)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IfTileStoreEntry createNewEmptyEntry(int x, int y, int zoom)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
