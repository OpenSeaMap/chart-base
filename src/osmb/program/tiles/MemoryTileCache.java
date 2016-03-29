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
package osmb.program.tiles;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.MP2MapSpace;
import osmb.mapsources.TileAddress;
import osmb.program.tiles.Tile.TileState;
import osmb.utilities.OSMBStrs;

/**
 * {@link TileImageCache} implementation that stores all {@link Tile} objects in memory up to a certain limit ( {@link #getCacheSize()}). If the limit is
 * exceeded the least recently used {@link Tile} objects will be deleted.
 * <p>
 * Its based upon code from Jan Peter Stotz.
 * 
 * @author humbach
 */
public final class MemoryTileCache implements NotificationListener
{
	// static / class data
	protected static Logger log = Logger.getLogger(MemoryTileCache.class);

	// instance data
	/**
	 * Default cache size in tiles. May be modified by constructor {@link #MemoryTileCache(int cacheSize)}.
	 */
	protected int mCacheSize = 0;

	/**
	 * LinkedHashMap holding the actual tile data. This LinkedHashMap is ordered by access, so it automatically can remove the eldest entry if the size is
	 * exhausted.
	 */
	protected class OSMMemTileMap extends LinkedHashMap<String, Tile>
	{
		private static final long serialVersionUID = 1L;

		OSMMemTileMap(int nSize, float fFillFact)
		{
			super(nSize, fFillFact, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Tile> eldest)
		{
			return (size() > mCacheSize);
		}
	}

	// protected ConcurrentHashMap<String, Tile> mHM = null;
	protected Map<String, Tile> mHM = null;

	public MemoryTileCache()
	{
		this(5000);
	}

	/**
	 * This initializes a memory tile cache with a specified size (in tiles, not in MiB).
	 * 
	 * @param cacheSize
	 */
	public MemoryTileCache(int cacheSize)
	{
		mCacheSize = cacheSize;
		mHM = Collections.synchronizedMap(new OSMMemTileMap(mCacheSize / 10, 0.75f));

		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		NotificationBroadcaster emitter = (NotificationBroadcaster) mbean;
		emitter.addNotificationListener(this, null, null);
		// Set-up each memory pool to notify if the free memory falls below 5% ????
		for (MemoryPoolMXBean memPool : ManagementFactory.getMemoryPoolMXBeans())
		{
			if (memPool.isUsageThresholdSupported())
			{
				MemoryUsage memUsage = memPool.getUsage();
				memPool.setUsageThreshold((long) (memUsage.getMax() * 0.95));
			}
		}
		log.debug("mtc[" + mHM.size() + ", " + mCacheSize + "] created");
	}

	/**
	 * In case we are running out of memory we free half of the cache, down to a minimum of 25 cached tiles.
	 * We get lots of notifs about 'PS Old Gen' from GC...
	 */
	@Override
	public void handleNotification(Notification notification, Object handback)
	{
		if ((!MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED.equals(notification.getType()))
		    && (!MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED.equals(notification.getType())))
		{
			log.trace("Memory notification: " + notification.toString());
			return;
		}

		// MemoryNotificationInfo info = MemoryNotificationInfo.from((CompositeData) notification.getUserData());
		// log.warn("Memory notification: " + notification.getMessage() + ", " + info.getPoolName());
		// synchronized (mruTiles)
		// {
		// int count_half = mruTiles.getElementCount() / 2;
		// count_half = Math.max(25, count_half);
		// if (mruTiles.getElementCount() <= count_half)
		// return;
		// log.debug("memory low - freeing cached tiles: " + mruTiles.getElementCount() + " -> " + count_half);
		// try
		// {
		// while (mruTiles.getElementCount() > count_half)
		// {
		// removeEntry(mruTiles.getLastElement());
		// }
		// }
		// catch (Exception e)
		// {
		// log.error("", e);
		// }
		// }
		// log.debug("mtc[" + mHT.size() + "] modified");
	}

	/**
	 * This adds a tile to the cache and removes any old entries for this tile from it.
	 */
	public void addTile(Tile tile)
	{
		mHM.put(tile.getKey(), tile);
		log.debug("mtc[" + mHM.size() + "] modified");
	}

	/**
	 * Retrieves the tile from the memory tile cache. The tile is identified by its x and y indices. Each zoom level holds 2<sup>zoom</sup> tiles.
	 * This currently does not handle 'special' tiles (hourglass, error, empty, plain sea), they exist multiple times in the cache.
	 * 
	 * @param mapSource
	 * @param x
	 *          tile x-index
	 * @param y
	 *          tile y-index
	 * @param z
	 *          Zoom level
	 * @return The tile or null if the tile is not in the cache.
	 */
	public Tile getTile(ACMapSource mapSource, int x, int y, int z)
	{
		return mHM.get(Tile.getTileKey(mapSource, new TileAddress(x, y, z)));
	}

	public Tile getTile(ACMapSource mapSource, TileAddress tAddr)
	{
		return mHM.get(Tile.getTileKey(mapSource, tAddr));
	}

	/**
	 * Tries to get tiles of a lower or higher zoom level (one or two level difference) from cache and use it as a
	 * placeholder until the tile has been loaded.
	 */
	// This does belong here. It has been moved from Tile to here. The Tile should not know anything about the TileStore/Cache structures.
	public Tile loadPlaceholderFromCache(Tile tile)
	{
		log.trace(OSMBStrs.RStr("START"));
		// Tile phTile = tile;
		int tileSize = MP2MapSpace.getTileSize();
		BufferedImage tmpImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) tmpImage.getGraphics();
		// g.drawImage(image, 0, 0, null);
		for (int zoomDiff = 1; zoomDiff < 5; zoomDiff++)
		{
			// first we check if there are already the 2^x tiles
			// of a higher detail level
			int zoom_high = tile.getZoom() + zoomDiff;
			if (zoomDiff < 3 && zoom_high <= tile.getSource().getMaxZoom())
			{
				int factor = 1 << zoomDiff;
				int xtile_high = tile.getXtile() << zoomDiff;
				int ytile_high = tile.getYtile() << zoomDiff;
				double scale = 1.0 / factor;
				g.setTransform(AffineTransform.getScaleInstance(scale, scale));
				int paintedTileCount = 0;
				for (int x = 0; x < factor; x++)
				{
					for (int y = 0; y < factor; y++)
					{
						Tile tmpTile = getTile(tile.getSource(), xtile_high + x, ytile_high + y, zoom_high);
						if (tmpTile != null && tmpTile.mTileState == TileState.TS_LOADED)
						{
							paintedTileCount++;
							tmpTile.paint(g, x * tileSize, y * tileSize);
						}
					}
				}
				if (paintedTileCount == factor * factor)
				{
					tile.setImage(tmpImage);
					tile.setTileState(TileState.TS_LOADING);
				}
			}
			else
			{
				int zoom_low = tile.getZoom() - zoomDiff;
				if (zoom_low >= tile.getSource().getMinZoom())
				{
					int xtile_low = tile.getXtile() >> zoomDiff;
					int ytile_low = tile.getYtile() >> zoomDiff;
					int factor = (1 << zoomDiff);
					double scale = factor;
					AffineTransform at = new AffineTransform();
					int translate_x = (tile.getXtile() % factor) * tileSize;
					int translate_y = (tile.getYtile() % factor) * tileSize;
					at.setTransform(scale, 0, 0, scale, -translate_x, -translate_y);
					g.setTransform(at);
					Tile tmpTile = getTile(tile.getSource(), xtile_low, ytile_low, zoom_low);
					if (tmpTile != null && tmpTile.mTileState == TileState.TS_LOADED)
					{
						tmpTile.paint(g, 0, 0);
					}
				}
				tile.setImage(tmpImage);
				tile.setTileState(TileState.TS_LOADING);
			}
		}
		return tile;
	}

	public int getTileCount()
	{
		return mHM.size();
	}

	public int getCacheSize()
	{
		return mCacheSize;
	}

	/**
	 * Changes the maximum number of {@link Tile} objects that this cache holds.
	 * 
	 * @param cacheSize
	 *          new maximum number of tiles
	 */
	public void setCacheSize(int cacheSize)
	{
		this.mCacheSize = cacheSize;
	}

	/**
	 * This completely removes all tiles from the mtc by clearing the underlying HashMap.
	 */
	public void clear()
	{
		mHM.clear();
	}
}
