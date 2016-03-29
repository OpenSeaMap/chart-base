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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.TileAddress;
import osmb.program.ACSettings;
import osmb.program.tiles.Tile;
import osmb.program.tiles.TileImageType;

/**
 * abstract class
 * 
 * @author humbach
 * 
 */
public abstract class ACNTileStore
{
	// class data
	protected static Logger log = Logger.getLogger(ACNTileStore.class);

	protected static Path sTileStoreDir;

	protected static HashMap<ACMapSource, ACNTileStore> sHM = new HashMap<>();

	protected static boolean sInitialized = false;

	protected static synchronized void init()
	{
		sTileStoreDir = ACSettings.getInstance().getTileStoreDirectory().toPath();
		log.debug("Tile store path: " + sTileStoreDir);
	}

	// Instance data

	protected boolean mInitialized = false;

	protected boolean isInitialized()
	{
		return mInitialized;
	}

	/**
	 * This should give the space on disk and the number of tiles for the specified tile store.
	 * 
	 * @param mapSourceName
	 * @return
	 * @throws InterruptedException
	 */
	public static TileStoreInfo getStoreInfo(String mapSourceName) throws InterruptedException
	{
		return null;
	}

	// instance data
	protected ACNTileStore()
	{
		log = Logger.getLogger(this.getClass());
		sTileStoreDir = ACSettings.getInstance().getTileStoreDirectory().toPath();
		log.debug("Tile store path: " + sTileStoreDir);
	}

	/**
	 * @throws Exception
	 *           In this super class it is unknown, which kind of Exception will be thrown.
	 */
	protected void initializeDB() throws Exception
	{
		log.error("no specific initialization is made, is this intended?");
	}

	/**
	 * This writes one tile into the tile store for the specified {@link ACMapSource}. It employs {@link putTileData(byte[] tileData, TileAddress tAddr,
	 * long timeLastModified, long timeExpires, String eTag)} for this task.
	 * 
	 * @param tileData
	 *          The image data as a byte array.
	 * @param tAddr
	 *          The tiles address.
	 * @throws IOException
	 */
	public abstract void putTileData(byte[] tileData, TileAddress tAddr) throws IOException;

	/**
	 * This writes one tile into the tile store for the specified {@link ACMapSource}.
	 * 
	 * @param byteArray
	 *          The image data as specified by the map sources {@link TileImageType}.
	 * @param tileData
	 *          The image data as a byte array.
	 * @param tAddr
	 *          The tiles address.
	 * @param timeLastModified
	 *          The timestamp of the last modification of the tiles data. Either the creation time of a local default tile, or the download time of the online
	 *          tile.
	 * @param timeExpires
	 *          The timestamp of the expiration of the tiles data.
	 * @param eTag
	 *          The eTag as provided by the server or an empty eTag for local default tiles.
	 * @throws IOException
	 */
	public abstract void putTileData(byte[] tileData, TileAddress tAddr, long timeLastModified, long timeExpires, String eTag) throws IOException;

	public abstract void putTile(Tile tile);

	public abstract Tile getTile(TileAddress tAddr);

	/**
	 * This checks if a requested tile exists in the tile store instance.
	 */
	public abstract boolean containsTile(TileAddress tAddr);

	/**
	 */
	public abstract void clearStore() throws TileStoreException;

	/**
	 */
	public abstract String[] getAllStoreNames();

	/**
	 * This should give the space on disk and the number of tiles for the specified tile store.
	 */
	public TileStoreInfo getStoreInfo() throws InterruptedException
	{
		return new TileStoreInfo(0, 0);
	}

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
	public abstract BufferedImage getCacheCoverage(ACMapSource mapSource, int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException;

	public abstract boolean isTileExpired(TileAddress tAddr);

	public boolean isTileExpired(Tile tile)
	{
		boolean bExp = false;
		ACSettings settings = ACSettings.getInstance();
		if (tile == null)
			return true;
		long maxExpirationTime = settings.getTileMaxExpirationTime();
		long minExpirationTime = settings.getTileMinExpirationTime();
		long modMillis = tile.getMod().getTime();
		long now = System.currentTimeMillis();
		// if (modDate + maxExpirationTime) has expired. then the tile has expired, regardless of the servers expiration date.
		if ((modMillis + maxExpirationTime) < now)
		{
			bExp = true;
		}
		// only if (modDate + minExpirationTime) has expired. then ...
		else if ((modMillis + minExpirationTime) < now)
		{
			Date expiryDate = tile.getExp();
			if (expiryDate != null)
			{
				// server had set an expiration time, use that.
				log.trace("tile expires=" + expiryDate);
				bExp = (expiryDate.getTime() < now);
			}
			else
			{
				// server had not set an expiration time, the tile has expired.
				bExp = true;
			}
		}
		return bExp;
	}

	public class TileInfo
	{
		private TileAddress mTA;
		private long mMod;
		private long mExp;
		private String mETag;
		private int mIID;

		TileInfo()
		{

		}

	}

}
