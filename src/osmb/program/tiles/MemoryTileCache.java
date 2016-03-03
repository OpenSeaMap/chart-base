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

/**
 * {@link TileImageCache} implementation that stores all {@link Tile} objects in memory up to a certain limit ( {@link #getCacheSize()}). If the limit is
 * exceeded the least recently used {@link Tile} objects will be deleted.
 * <p>
 * Its based upon code from Jan Peter Stotz.
 * 
 * @author humbach
 */
public class MemoryTileCache implements NotificationListener
{
	// static / class data
	protected static final Logger log = Logger.getLogger(MemoryTileCache.class);

	// instance data
	/**
	 * Default cache size in tiles. May be modified by constructor {@link #MemoryTileCache(int cacheSize)}.
	 */
	protected int mCacheSize = 0;
	// protected Hashtable<String, CacheEntry> mHT;
	/**
	 * LinkedHashMap holding the actual tile data.
	 */
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
		mHM = Collections.synchronizedMap(new LinkedHashMap<String, Tile>(mCacheSize / 10, 0.75f, true));
		// mruTiles = new CacheLinkedListElement();

		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		NotificationBroadcaster emitter = (NotificationBroadcaster) mbean;
		emitter.addNotificationListener(this, null, null);
		// Set-up each memory pool to notify if the free memory falls below 10%
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

	protected boolean removeEldestEntry(Map.Entry<String, Tile> eldest)
	{
		return (mHM.size() > mCacheSize);
	}

	/**
	 * This adds a tile to the cache and removes any old entries for this tile from it.
	 */
	public void addTile(Tile tile)
	{
		// CacheEntry entry = createCacheEntry(tile);
		// mHT.put(tile.getKey(), entry);
		mHM.put(tile.getKey(), tile);
		// mruTiles.addFirst(entry);
		// if (mHM.size() > mCacheSize)
		// // removeOldEntries();
		// mHM.removeEldestEntry(null);
		log.debug("mtc[" + mHM.size() + "] modified");
	}

	/**
	 * Retrieves the tile from the memory tile cache. The tile is identified by its x and y indices. Each zoom level holds 2<sup>zoom</sup> tiles.
	 * This currently does not handle 'special' tiles (hourglass, error, empty, plain sea), they exist multiple times in the cache.
	 * 
	 * @param source
	 * @param x
	 *          tile x-index
	 * @param y
	 *          tile y-index
	 * @param z
	 *          Zoom level
	 * @return The tile or null if the tile is not in the cache.
	 */
	public Tile getTile(ACMapSource source, int x, int y, int z)
	{
		return mHM.get(Tile.getTileKey(source, x, y, z));
	}

	public Tile getTile(ACMapSource mapSource, TileAddress tAddr)
	{
		return mHM.get(Tile.getTileKey(mapSource, tAddr.getX(), tAddr.getY(), tAddr.getZoom()));
	}

	/**
	 * Tries to get tiles of a lower or higher zoom level (one or two level difference) from cache and use it as a
	 * placeholder until the tile has been loaded.
	 */
	// This does belong here. It has been moved from Tile to here. The Tile should not know anything about the TileStore/Cache structures.
	public Tile loadPlaceholderFromCache(Tile tile)
	{
		Tile phTile = tile;
		int tileSize = MP2MapSpace.getTileSize();
		BufferedImage tmpImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) tmpImage.getGraphics();
		// g.drawImage(image, 0, 0, null);
		for (int zoomDiff = 1; zoomDiff < 5; zoomDiff++)
		{
			// first we check if there are already the 2^x tiles
			// of a higher detail level
			int zoom_high = tile.mZoom + zoomDiff;
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
				}
			}
			else
			{
				int zoom_low = tile.mZoom - zoomDiff;
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
	 * Linked list element holding the {@link Tile} and links to the {@link #next} and {@link #prev} item in the list.
	 */
	protected static class CacheEntry
	{
		// Tile tile;
		String tileID;

		CacheEntry next;
		CacheEntry prev;

		protected CacheEntry(Tile tile)
		{
			this.tileID = tile.getKey();
		}

		// public String getTile()
		// {
		// return tileID;
		// }
		//
		public CacheEntry getNext()
		{
			return next;
		}

		public CacheEntry getPrev()
		{
			return prev;
		}
	}

	/**
	 * 
	 * @author humbach
	 */
	protected static class CacheLinkedListElement
	{
		protected int elementCount = 0;
		protected CacheEntry firstElement = null;
		protected CacheEntry lastElement = null;

		public CacheLinkedListElement()
		{
			clear();
		}

		public synchronized void clear()
		{
			elementCount = 0;
			firstElement = null;
			lastElement = null;
		}

		/**
		 * Add the element to the head of the list.
		 * 
		 * @param element
		 *          element to be added
		 */
		public synchronized void addFirst(CacheEntry element)
		{
			if (elementCount == 0)
			{
				firstElement = element;
				lastElement = element;
				element.prev = null;
				element.next = null;
			}
			else
			{
				removeEntry(element);

				element.next = firstElement;
				firstElement.prev = element;
				element.prev = null;
				firstElement = element;
			}
			elementCount++;
		}

		/**
		 * Removes the specified element from the list.
		 * 
		 * @param element
		 *          to be removed
		 */
		public synchronized void removeEntry(CacheEntry element)
		{
			if (element.next != null)
			{
				element.next.prev = element.prev;
			}
			if (element.prev != null)
			{
				element.prev.next = element.next;
			}
			if (element == firstElement)
				firstElement = element.next;
			if (element == lastElement)
				lastElement = element.prev;
			element.next = null;
			element.prev = null;
			elementCount--;
		}

		public synchronized void moveElementToFirstPos(CacheEntry entry)
		{
			if (firstElement == entry)
				return;
			removeEntry(entry);
			addFirst(entry);
		}

		public int getElementCount()
		{
			return elementCount;
		}

		public CacheEntry getLastElement()
		{
			return lastElement;
		}

		public CacheEntry getFirstElement()
		{
			return firstElement;
		}
	}

	/**
	 * This completely removes all tiles from the mtc by clearing the underlying HashMap.
	 */
	public void clear()
	{
		mHM.clear();
	}
}
