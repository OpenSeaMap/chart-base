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
package osmb.program.tilestore;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSource;
import osmb.program.ACSettings;
import osmb.program.tiles.Tile;
import osmb.program.tilestore.berkeleydb.SiBerkeleyDbTileStore;
import osmb.program.tilestore.sqlitedb.SQLiteDbTileStore;

/**
 * abstract class
 * The BerkeleyDB implementation offers no multi thread access (afaik) so a singleton is used.
 * 
 * @author humbach
 * 
 */
public abstract class ACTileStore
{
	// class data
	protected static Logger log = Logger.getLogger(ACTileStore.class);
	protected static ACTileStore INSTANCE = null;

	public static synchronized void initialize()
	{
		if (INSTANCE != null)
			return;
		try
		{
			INSTANCE = new SiBerkeleyDbTileStore();
		}
		catch (TileStoreException e)
		{
			System.exit(1);
		}
		// testing of SQLite tile store
		try
		{
			// SQLiteDbTileStore ttt = new SQLiteDbTileStore();
			// ttt.initializeCommonDB();
			SQLiteDbTileStore.initializeCommonDB();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static ACTileStore getInstance()
	{
		return INSTANCE;
	}

	// instance data
	protected File mTileStoreDir;

	protected ACTileStore()
	{
		log = Logger.getLogger(this.getClass());
		mTileStoreDir = ACSettings.getInstance().getTileStoreDirectory();
		log.debug("Tile store path: " + mTileStoreDir);
	}

	/**
	 * This writes one tile into the tile store for the specified {@link IfMapSource}. It employs {@link putTileData(byte[] tileData, int x, int y, int zoom,
	 * IfMapSource mapSource,
	 * long timeLastModified, long timeExpires, String eTag)} for this task.
	 * 
	 * @param tileData
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @throws IOException
	 */
	public abstract void putTileData(byte[] tileData, int x, int y, int zoom, IfMapSource mapSource) throws IOException;

	/**
	 * This writes one tile into the tile store for the specified {@link IfMapSource}.
	 * 
	 * @param tileData
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @param timeLastModified
	 * @param timeExpires
	 * @param eTag
	 * @throws IOException
	 */
	public abstract void putTileData(byte[] tileData, int x, int y, int zoom, IfMapSource mapSource, long timeLastModified, long timeExpires, String eTag)
	    throws IOException;

	public abstract void putTile(IfTileStoreEntry tile, IfMapSource mapSource);

	public void putTile(Tile tile, IfMapSource mapSource)
	{
		if (this instanceof SiBerkeleyDbTileStore)
		{
			log.error("called for wrong tile store, SiBerkeleyDbTileStore does not support putTile(Tile, IfMapSource)");
		}
	}

	/**
	 * This retrieves one tile from the tilestore for the specified {@link IfMapSource}.
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @return
	 */
	public abstract IfTileStoreEntry getTile(int x, int y, int zoom, IfMapSource mapSource);

	/**
	 * This checks if a requested tile exists in the tilestore for a specified {@link IfMapSource}.
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @return
	 */
	public abstract boolean contains(int x, int y, int zoom, IfMapSource mapSource);

	/**
	 * 
	 * @param mapSource
	 * @throws TileStoreException
	 */
	public abstract void prepareTileStore(IfMapSource mapSource) throws TileStoreException;

	/**
	 * 
	 * @param storeName
	 */
	public abstract void clearStore(String storeName);

	/**
	 * 
	 * @return
	 */
	public abstract String[] getAllStoreNames();

	/**
	 * Returns <code>true</code> if the tile store directory of the specified {@link IfMapSource} exists.
	 * 
	 * @param mapSource
	 * @return
	 */
	public abstract boolean storeExists(IfMapSource mapSource);

	/**
	 * 
	 * @param mapSourceName
	 * @return
	 * @throws InterruptedException
	 */
	public abstract TileStoreInfo getStoreInfo(String mapSourceName) throws InterruptedException;

	/**
	 * This provides an image visualizing the coverage of the tilestore for the specified {@link IfMapSource}.
	 * 
	 * @param mapSource
	 * @param zoom
	 * @param tileNumMin
	 * @param tileNumMax
	 * @return
	 * @throws InterruptedException
	 */
	public abstract BufferedImage getCacheCoverage(IfMapSource mapSource, int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException;

	public abstract void closeAll();

	public abstract IfTileStoreEntry createNewEntry(int x, int y, int zoom, byte[] data, long timeLastModified, long timeExpires, String eTag);

	/**
	 * Creates a new empty {@link IfTileStoreEntry} that represents a missing tile in a sparse map source. This we should use twice: For empty tiles in OpenSeaMap
	 * AND for 'empty' sea tiles in the BaseMap.
	 * We still have to find a way to detect these...
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @return
	 */
	public abstract IfTileStoreEntry createNewEmptyEntry(int x, int y, int zoom);

	public boolean isTileExpired(IfTileStoreEntry tileStoreEntry)
	{
		ACSettings settings = ACSettings.getInstance();
		if (tileStoreEntry == null)
			return true;
		long expiredTime = tileStoreEntry.getTimeExpires();
		log.trace("ts.expires=" + expiredTime);
		if (expiredTime >= 0)
		{
			// server had set an expiration time
			long maxExpirationTime = settings.getTileMaxExpirationTime() + tileStoreEntry.getTimeDownloaded();
			long minExpirationTime = settings.getTileMinExpirationTime() + tileStoreEntry.getTimeDownloaded();
			expiredTime = Math.max(minExpirationTime, Math.min(maxExpirationTime, expiredTime));
			log.trace("ts/set.expires=" + expiredTime + ", tDl=" + tileStoreEntry.getTimeDownloaded() + ", sMax=" + maxExpirationTime + ", sMin=" + minExpirationTime
			    + ", now=" + System.currentTimeMillis() + ", bExp=" + (expiredTime < System.currentTimeMillis()) + ", tile(" + tileStoreEntry.getZoom() + "|"
			    + tileStoreEntry.getX() + "|" + tileStoreEntry.getY() + ")");
			log.trace("ts/set.expires=" + expiredTime + ", now=" + System.currentTimeMillis() + ", bExp=" + (expiredTime < System.currentTimeMillis()) + ", tile("
			    + tileStoreEntry.getZoom() + "|" + tileStoreEntry.getX() + "|" + tileStoreEntry.getY() + ")");
		}
		else
		{
			// no expiration time set by server - use the default one
			expiredTime = tileStoreEntry.getTimeDownloaded() + ACSettings.getTileDefaultExpirationTime();
			log.trace("def.expires=" + expiredTime);
		}
		return (expiredTime < System.currentTimeMillis());
	}

}
