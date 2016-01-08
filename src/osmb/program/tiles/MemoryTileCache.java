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

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Hashtable;

import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSource;

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
	protected final Logger log;

	/**
	 * Default cache size in tiles. May be modified by constructor {@link #MemoryTileCache(int cacheSize)}.
	 */
	protected int mCacheSize = 2000;
	// protected Hashtable<String, CacheEntry> mHT;
	/**
	 * hashtable holding the actual tile data.
	 */
	protected Hashtable<String, Tile> mHT;

	/**
	 * List of all tiles by their key in their most recently used order.
	 */
	protected CacheLinkedListElement mruTiles;

	public MemoryTileCache()
	{
		log = Logger.getLogger(this.getClass());
		mCacheSize = 5000;
		// mHT = new Hashtable<String, CacheEntry>(mCacheSize);
		mHT = new Hashtable<String, Tile>(mCacheSize);
		mruTiles = new CacheLinkedListElement();

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
		log.debug("mtc[" + mHT.size() + ", " + mCacheSize + "] created");
	}

	/**
	 * This initializes a memory tile cache with a specified size (in tiles, not in MiB).
	 * 
	 * @param cacheSize
	 */
	public MemoryTileCache(int cacheSize)
	{
		log = Logger.getLogger(this.getClass());
		mCacheSize = cacheSize;
		// mHT = new Hashtable<String, CacheEntry>(mCacheSize);
		mHT = new Hashtable<String, Tile>(mCacheSize);
		mruTiles = new CacheLinkedListElement();

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
		log.debug("mtc[" + mHT.size() + ", " + mCacheSize + "] created");
	}

	/**
	 * In case we are running out of memory we free half of the cached down to a minimum of 25 cached tiles.
	 */
	@Override
	public void handleNotification(Notification notification, Object handback)
	{
		log.trace("Memory notification: " + notification.toString());
		if (!MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED.equals(notification.getType()))
			return;
		synchronized (mruTiles)
		{
			int count_half = mruTiles.getElementCount() / 2;
			count_half = Math.max(25, count_half);
			if (mruTiles.getElementCount() <= count_half)
				return;
			log.warn("memory low - freeing cached tiles: " + mruTiles.getElementCount() + " -> " + count_half);
			try
			{
				while (mruTiles.getElementCount() > count_half)
				{
					removeEntry(mruTiles.getLastElement());
				}
			}
			catch (Exception e)
			{
				log.error("", e);
			}
		}
		log.debug("mtc[" + mHT.size() + "] modified");
	}

	/**
	 * This adds a tile to the cache and removes any old entries for this tile from it.
	 */
	public void addTile(Tile tile)
	{
		CacheEntry entry = createCacheEntry(tile);
		// mHT.put(tile.getKey(), entry);
		mHT.put(tile.getKey(), tile);
		mruTiles.addFirst(entry);
		if (mHT.size() > mCacheSize)
			removeOldEntries();
		log.debug("mtc[" + mHT.size() + "] modified");
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
	public Tile getTile(IfMapSource source, int x, int y, int z)
	{
		return mHT.get(Tile.getTileKey(source, x, y, z));
	}

	/**
	 * Removes the least recently used tiles
	 */
	protected void removeOldEntries()
	{
		synchronized (mruTiles)
		{
			try
			{
				while (mruTiles.getElementCount() > mCacheSize)
				{
					removeEntry(mruTiles.getLastElement());
				}
			}
			catch (Exception e)
			{
				log.warn("", e);
			}
		}
	}

	protected void removeEntry(CacheEntry entry)
	{
		mHT.remove(entry.tileID);
		mruTiles.removeEntry(entry);
		log.debug("mtc[" + mHT.size() + "] modified");
	}

	protected CacheEntry createCacheEntry(Tile tile)
	{
		return new CacheEntry(tile);
	}

	/**
	 * Clears the cache deleting all tiles from memory
	 */
	public void clear()
	{
		synchronized (mruTiles)
		{
			mHT.clear();
			mruTiles.clear();
		}
	}

	public int getTileCount()
	{
		return mHT.size();
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
		if (mHT.size() > cacheSize)
			removeOldEntries();
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
}
