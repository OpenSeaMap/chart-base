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
package osmb.program.tilestore.berkeleydb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.evolve.Renamer;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.TileAddress;
import osmb.program.ACSettings;
import osmb.program.DelayedInterruptThread;
import osmb.program.tiles.Tile;
import osmb.program.tilestore.ACTileStore;
import osmb.program.tilestore.IfStoredTile;
import osmb.program.tilestore.TileStoreException;
import osmb.program.tilestore.TileStoreInfo;
import osmb.program.tilestore.berkeleydb.TileDbEntry.TileDbKey;
import osmb.utilities.OSMBUtilities;
import osmb.utilities.file.DeleteFileFilter;
import osmb.utilities.file.DirInfoFileFilter;
import osmb.utilities.file.DirectoryFileFilter;

/**
 * The database based tile store implementation. It still is a singleton.
 */
public class SiBerkeleyDbTileStore extends ACTileStore
{
	/**
	 * Max count of tile stores opened
	 */
	private static final int MAX_CONCURRENT_ENVIRONMENTS = 5;

	public static SiBerkeleyDbTileStore getInstance()
	{
		if (INSTANCE == null)
			try
			{
				INSTANCE = new SiBerkeleyDbTileStore();
			}
			catch (TileStoreException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return (SiBerkeleyDbTileStore) INSTANCE;
	}

	private EnvironmentConfig envConfig;
	/**
	 * Map holding the database for each map source.
	 */
	private Map<String, TileDatabase> tileDbMap;
	private FileLock tileStoreLock = null;
	private Mutations mutations;

	public SiBerkeleyDbTileStore() throws TileStoreException
	{
		super();
		acquireTileStoreLock();
		tileDbMap = new TreeMap<String, TileDatabase>();

		envConfig = new EnvironmentConfig();
		envConfig.setTransactional(false);
		envConfig.setLocking(true);
		// envConfig.setExceptionListener(GUIExceptionHandler.getInstance());
		envConfig.setAllowCreate(true);
		envConfig.setSharedCache(true);
		envConfig.setCachePercent(50);

		mutations = new Mutations();

		String oldPackage1 = "tac.tilestore.berkeleydb";
		String oldPackage2 = "tac.program.tilestore.berkeleydb";
		String oldPackage3 = "mobac.program.tilestore.berkeleydb";
		String oldPackage4 = "osmcb.program.tilestore.berkeleydb";
		String oldPackage5 = "osmcd.program.tilestore.berkeleydb";
		String oldPackage6 = "osmcb.program.tilestore.berkeleydb";
		String entry = ".TileDbEntry";
		String key = ".TileDbEntry$TileDbKey";
		mutations.addRenamer(new Renamer(oldPackage1 + entry, 0, TileDbEntry.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage1 + key, 0, TileDbKey.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage1 + entry, 1, TileDbEntry.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage1 + key, 1, TileDbKey.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage2 + entry, 2, TileDbEntry.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage2 + key, 2, TileDbKey.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage3 + entry, 3, TileDbEntry.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage3 + key, 3, TileDbKey.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage4 + entry, 4, TileDbEntry.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage4 + key, 4, TileDbKey.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage5 + entry, 5, TileDbEntry.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage5 + key, 5, TileDbKey.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage6 + entry, 6, TileDbEntry.class.getName()));
		mutations.addRenamer(new Renamer(oldPackage6 + key, 6, TileDbKey.class.getName()));

		// for (Renamer r : mutations.getRenamers())
		// log.debug(r.toString());
		log.trace("before addShutdownHook()");
		Runtime.getRuntime().addShutdownHook(new ShutdownThread(true));
	}

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
	}

	protected void acquireTileStoreLock() throws TileStoreException
	{
		try
		{
			// Get a file channel for the file
			File file = new File(mTileStoreDir, "lock");
			if (!mTileStoreDir.isDirectory())
				try
				{
					OSMBUtilities.mkDirs(mTileStoreDir);
				}
				catch (IOException e)
				{
					throw new TileStoreException("Unable to create tile store directory: \"" + mTileStoreDir.getPath() + "\"");
				}
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

			// Use the file channel to create a lock on the file.
			// This method blocks until it can retrieve the lock.

			// Try acquiring the lock without blocking. This method returns
			// null or throws an exception if the file is already locked.
			tileStoreLock = channel.tryLock();
			if (tileStoreLock == null)
				throw new TileStoreException("Unable to obtain tile store lock - another instance of OpenSeaMap ChartBundler is running!");

			// // Release the lock
			// lock.release();
			//
			// // Close the file
			// channel.close();
		}
		catch (Exception e)
		{
			log.error("", e);
			throw new TileStoreException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public IfStoredTile createNewEntry(int x, int y, int zoom, byte[] data, long timeLastModified, long timeExpires, String eTag)
	{
		return new TileDbEntry(x, y, zoom, data, timeLastModified, timeExpires, eTag);
	}

	@Override
	public IfStoredTile createNewEmptyEntry(int x, int y, int zoom)
	{
		long time = System.currentTimeMillis();
		long timeExpires = time + ACSettings.getTileDefaultExpirationTime();
		// We set the tile data to an empty array because we can not store null
		return new TileDbEntry(x, y, zoom, new byte[] {}, time, timeExpires, "");
	}

	/**
	 * This retrieves the database holding all downloaded tiles for the specified map source.
	 * 
	 * @param mapSource
	 * @return
	 * @throws DatabaseException
	 */
	private TileDatabase getTileDatabase(ACMapSource mapSource) throws DatabaseException
	{
		TileDatabase db;
		if (tileDbMap == null)
		  // Tile store has been closed already
		  return null;
		String storeName = mapSource.getName();
		if (storeName == null)
			return null;
		synchronized (tileDbMap)
		{
			db = tileDbMap.get(storeName);
		}
		if (db != null)
			return db;
		try
		{
			synchronized (tileDbMap)
			{
				cleanupDatabases();
				db = tileDbMap.get(storeName);
				if (db == null)
				{
					db = new TileDatabase(storeName);
					db.lastAccess = System.currentTimeMillis();
					tileDbMap.put(mapSource.getName(), db);
				}
				return db;
			}
		}
		catch (Exception e)
		{
			log.error("Error creating tile store db \"" + mapSource.getName() + "\"", e);
			throw new TileStoreException(e);
		}
	}

	private TileDatabase getTileDatabase(String storeName) throws DatabaseException
	{
		TileDatabase db;
		if (tileDbMap == null)
		  // Tile store has been closed already
		  return null;
		if (storeName == null)
			return null;
		synchronized (tileDbMap)
		{
			db = tileDbMap.get(storeName);
		}
		if (db != null)
			return db;
		try
		{
			synchronized (tileDbMap)
			{
				cleanupDatabases();
				db = tileDbMap.get(storeName);
				if (db == null)
				{
					db = new TileDatabase(storeName);
					db.lastAccess = System.currentTimeMillis();
					tileDbMap.put(storeName, db);
				}
				return db;
			}
		}
		catch (Exception e)
		{
			log.error("Error creating tile store db \"" + storeName + "\"", e);
			throw new TileStoreException(e);
		}
	}

	@Override
	public TileStoreInfo getStoreInfo(String storeName) throws InterruptedException
	{
		int tileCount = getNrOfTiles(storeName);
		long storeSize = getStoreSize(storeName);
		return new TileStoreInfo(storeSize, tileCount);
	}

	@Override
	public void putTileData(byte[] tileData, TileAddress tAddr, ACMapSource mapSource, long timeLastModified, long timeExpires, String eTag) throws IOException
	{
		TileDbEntry tile = new TileDbEntry(tAddr.getX(), tAddr.getY(), tAddr.getZoom(), tileData, timeLastModified, timeExpires, eTag);
		TileDatabase db = null;
		try
		{
			if (log.isTraceEnabled())
				log.trace("Saved " + mapSource.getName() + " " + tile);
			db = getTileDatabase(mapSource);
			if (db != null)
				db.put(tile);
		}
		catch (Exception e)
		{
			if (db != null)
				db.close();
			log.error("Failed to write tile to tile store \"" + mapSource.getName() + "\"", e);
		}
	}

	@Override
	public void putTileData(byte[] tileData, TileAddress tAddr, ACMapSource mapSource) throws IOException
	{
		this.putTileData(tileData, tAddr, mapSource, -1, -1, null);
	}

	@Override
	@Deprecated
	public void putTileData(byte[] tileData, int x, int y, int zoom, ACMapSource mapSource) throws IOException
	{
		this.putTileData(tileData, new TileAddress(x, y, zoom), mapSource, -1, -1, null);
	}

	@Override
	@Deprecated
	public void putTileData(byte[] tileData, int x, int y, int zoom, ACMapSource mapSource, long timeLastModified, long timeExpires, String eTag)
	    throws IOException
	{
		this.putTileData(tileData, new TileAddress(x, y, zoom), mapSource, timeLastModified, timeExpires, eTag);
	}

	@Override
	public void putTile(IfStoredTile tile, ACMapSource mapSource)
	{
		TileDatabase db = null;
		try
		{
			if (log.isTraceEnabled())
				log.trace("Saved " + mapSource.getName() + " " + tile);
			db = getTileDatabase(mapSource);
			db.put((TileDbEntry) tile);
		}
		catch (Exception e)
		{
			if (db != null)
				db.close();
			log.error("Failed to write tile to tile store \"" + mapSource.getName() + "\"", e);
		}
	}

	@Override
	public IfStoredTile getTileEntry(int x, int y, int zoom, ACMapSource mapSource)
	{
		TileDatabase db = null;
		try
		{
			db = getTileDatabase(mapSource);
			if (db == null)
				return null;
			IfStoredTile tile = db.get(new TileDbKey(x, y, zoom));
			if (log.isTraceEnabled())
			{
				if (tile == null)
					log.trace("Tile store cache miss: (x,y,z)" + x + "/" + y + "/" + zoom + " " + mapSource.getName());
				else
					log.trace("Loaded " + mapSource.getName() + " " + tile);
			}
			return tile;
		}
		catch (Exception e)
		{
			if (db != null)
				db.close();
			log.error("failed to retrieve tile from tile store \"" + mapSource.getName() + "\"", e);
			return null;
		}
	}

	@Override
	public boolean contains(int x, int y, int zoom, ACMapSource mapSource)
	{
		try
		{
			return getTileDatabase(mapSource).contains(new TileDbKey(x, y, zoom));
		}
		catch (DatabaseException e)
		{
			log.error("", e);
			return false;
		}
	}

	@Override
	public void prepareTileStore(ACMapSource mapSource)
	{
		try
		{
			getTileDatabase(mapSource);
		}
		catch (DatabaseException e)
		{
		}
	}

	@Override
	public void clearStore(String storeName)
	{
		File databaseDir = getStoreDir(storeName);

		TileDatabase db;
		synchronized (tileDbMap)
		{
			db = tileDbMap.get(storeName);
			if (db != null)
				db.close(false);
			if (databaseDir.exists())
			{
				DeleteFileFilter dff = new DeleteFileFilter();
				databaseDir.listFiles(dff);
				databaseDir.delete();
				log.debug("Tilestore " + storeName + " cleared: " + dff);
			}
			tileDbMap.remove(storeName);
		}
	}

	/**
	 * This method returns the amount of tiles in the store of tiles which is specified by the {@link IfMapSource} object.
	 * 
	 * @param mapSourceName
	 *          the store to calculate number of tiles in
	 * @return the amount of tiles in the specified store.
	 * @throws InterruptedException
	 */
	public int getNrOfTiles(String mapSourceName) throws InterruptedException
	{
		try
		{
			File storeDir = getStoreDir(mapSourceName);
			if (!storeDir.isDirectory())
				return 0;
			TileDatabase db = getTileDatabase(mapSourceName);
			int tileCount = (int) db.entryCount();
			db.close();
			return tileCount;
		}
		catch (DatabaseException e)
		{
			log.error("", e);
			return -1;
		}
	}

	public long getStoreSize(String storeName) throws InterruptedException
	{
		File tileStore = getStoreDir(storeName);
		if (tileStore.exists())
		{
			DirInfoFileFilter diff = new DirInfoFileFilter();
			try
			{
				tileStore.listFiles(diff);
			}
			catch (RuntimeException e)
			{
				throw new InterruptedException();
			}
			return diff.getDirSize();
		}
		else
		{
			return 0;
		}
	}

	@Override
	public BufferedImage getCacheCoverage(ACMapSource mapSource, int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException
	{
		TileDatabase db;
		try
		{
			db = getTileDatabase(mapSource);
			return db.getCacheCoverage(zoom, tileNumMin, tileNumMax);
		}
		catch (DatabaseException e)
		{
			log.error("", e);
			return null;
		}
	}

	protected void cleanupDatabases()
	{
		if (tileDbMap.size() < MAX_CONCURRENT_ENVIRONMENTS)
			return;
		synchronized (tileDbMap)
		{
			List<TileDatabase> list = new ArrayList<TileDatabase>(tileDbMap.values());
			Collections.sort(list, new Comparator<TileDatabase>()
			{

				@Override
				public int compare(TileDatabase o1, TileDatabase o2)
				{
					if (o1.lastAccess == o2.lastAccess)
						return 0;
					return (o1.lastAccess < o2.lastAccess) ? -1 : 1;
				}
			});
			for (int i = 0; i < list.size() - 2; i++)
				list.get(i).close();
		}
	}

	@Override
	public void closeAll()
	{
		Thread t = new ShutdownThread(false);
		t.start();
		try
		{
			t.join();
		}
		catch (InterruptedException e)
		{
			log.error("", e);
		}
	}

	/**
	 * Returns <code>true</code> if the tile store directory of the specified {@link IfMapSource} exists.
	 * 
	 * @param mapSource
	 * @return
	 */
	public boolean storeExists(ACMapSource mapSource)
	{
		File tileStore = getStoreDir(mapSource);
		return (tileStore.isDirectory()) && (tileStore.exists());
	}

	/**
	 * Returns the directory used for storing the tile database of the {@link IfMapSource} specified by <code>mapSource</code>
	 * 
	 * @param mapSource
	 * @return
	 */
	protected File getStoreDir(ACMapSource mapSource)
	{
		return getStoreDir(mapSource.getName());
	}

	/**
	 * @param mapSourceName
	 * @return directory used for storing the tile database belonging to <code>mapSource</code>
	 */
	protected File getStoreDir(String mapSourceName)
	{
		return new File(mTileStoreDir, "db-" + mapSourceName);
	}

	@Override
	public String[] getAllStoreNames()
	{
		File[] dirs = mTileStoreDir.listFiles(new DirectoryFileFilter());
		ArrayList<String> storeNames = new ArrayList<String>(dirs.length);
		for (File d : dirs)
		{
			String name = d.getName();
			if (name.startsWith("db-"))
			{
				name = name.substring(3);
				storeNames.add(name);
			}
		}
		String[] result = new String[storeNames.size()];
		storeNames.toArray(result);
		return result;
	}

	/**
	 * To shutdown the whole app we employ one thread to perform the necessary actions.
	 * Especially it performs a clean shutdown of the tile store databases.
	 * 
	 * @author humbach
	 *
	 */
	private class ShutdownThread extends DelayedInterruptThread
	{
		private final boolean shutdown;

		public ShutdownThread(boolean shutdown)
		{
			super("DBShutdown");
			this.shutdown = shutdown;
		}

		@Override
		public void run()
		{
			log.debug("Closing all tile databases...");
			synchronized (tileDbMap)
			{
				for (TileDatabase db : tileDbMap.values())
				{
					db.close(false);
				}
				tileDbMap.clear();
				if (shutdown)
				{
					tileDbMap = null;
					try
					{
						tileStoreLock.release();
					}
					catch (IOException e)
					{
						log.error("", e);
					}
				}
			}
			log.debug("All tile databases have been closed");
		}
	}

	protected class TileDatabase
	{
		final String mapSourceName;
		final Environment env;
		final EntityStore store;
		final PrimaryIndex<TileDbKey, TileDbEntry> tileIndex;
		boolean dbClosed = false;

		long lastAccess;

		public TileDatabase(String mapSourceName) throws IOException, EnvironmentLockedException, DatabaseException
		{
			this(mapSourceName, getStoreDir(mapSourceName));
		}

		public TileDatabase(String mapSourceName, File databaseDirectory) throws IOException, EnvironmentLockedException, DatabaseException
		{
			log.debug("Opening tile store db: \"" + databaseDirectory + "\"");
			File storeDir = databaseDirectory;
			// DelayedInterruptThread t = (DelayedInterruptThread) Thread.currentThread();
			try
			{
				// t.pauseInterrupt();
				this.mapSourceName = mapSourceName;
				lastAccess = System.currentTimeMillis();

				OSMBUtilities.mkDirs(storeDir);

				env = new Environment(storeDir, envConfig);

				StoreConfig storeConfig = new StoreConfig();
				storeConfig.setAllowCreate(true);
				storeConfig.setTransactional(false);
				storeConfig.setMutations(mutations);
				store = new EntityStore(env, "TilesEntityStore", storeConfig);

				tileIndex = store.getPrimaryIndex(TileDbKey.class, TileDbEntry.class);
			}
			finally
			{
				// if (t.interruptedWhilePaused())
				// close();
				// t.resumeInterrupt();
			}
		}

		public boolean isClosed()
		{
			return dbClosed;
		}

		public long entryCount() throws DatabaseException
		{
			return tileIndex.count();
		}

		public void put(TileDbEntry tile) throws DatabaseException
		{
			// DelayedInterruptThread t = (DelayedInterruptThread) Thread.currentThread();
			try
			{
				// t.pauseInterrupt();
				tileIndex.put(tile);
			}
			finally
			{
				// if (t.interruptedWhilePaused())
				// close();
				// t.resumeInterrupt();
			}
		}

		public boolean contains(TileDbKey key) throws DatabaseException
		{
			return tileIndex.contains(key);
		}

		public TileDbEntry get(TileDbKey key) throws DatabaseException
		{
			return tileIndex.get(key);
		}

		public PrimaryIndex<TileDbKey, TileDbEntry> getTileIndex()
		{
			return tileIndex;
		}

		public BufferedImage getCacheCoverage(int zoom, Point tileNumMin, Point tileNumMax) throws DatabaseException, InterruptedException
		{
			log.debug("Loading cache coverage for region " + tileNumMin + " " + tileNumMax + " of zoom level " + zoom);
			// DelayedInterruptThread t = (DelayedInterruptThread) Thread.currentThread();
			int width = tileNumMax.x - tileNumMin.x + 1;
			int height = tileNumMax.y - tileNumMin.y + 1;
			byte ff = (byte) 0xFF;
			byte[] colors = new byte[]
			{ 120, 120, 120, 120, // alpha-gray
			    10, ff, 0, 120 // alpha-green
			};
			IndexColorModel colorModel = new IndexColorModel(2, 2, colors, 0, true);
			BufferedImage image = null;
			try
			{
				image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
			}
			catch (Throwable e)
			{
				log.error("Failed to create coverage image: " + e.toString());
				image = null;
				System.gc();
				return null;
			}
			WritableRaster raster = image.getRaster();

			// We are loading the coverage of the selected area column by column which is much faster than loading the
			// whole region at once
			for (int x = tileNumMin.x; x <= tileNumMax.x; x++)
			{
				TileDbKey fromKey = new TileDbKey(x, tileNumMin.y, zoom);
				TileDbKey toKey = new TileDbKey(x, tileNumMax.y, zoom);
				EntityCursor<TileDbKey> cursor = tileIndex.keys(fromKey, true, toKey, true);
				try
				{
					TileDbKey key = cursor.next();
					while (key != null)
					{
						int pixelx = key.x - tileNumMin.x;
						int pixely = key.y - tileNumMin.y;
						raster.setSample(pixelx, pixely, 0, 1);
						key = cursor.next();
						// if (t.isInterrupted())
						if (Thread.currentThread().isInterrupted())
						{
							log.debug("Cache coverage loading aborted");
							throw new InterruptedException();
						}
					}
				}
				finally
				{
					cursor.close();
				}
			}
			return image;
		}

		protected void purge()
		{
			try
			{
				store.sync();
				env.cleanLog();
			}
			catch (DatabaseException e)
			{
				log.error("database compression failed: ", e);
			}
		}

		public void close()
		{
			close(true);
		}

		public void close(boolean removeFromMap)
		{
			if (dbClosed)
				return;
			if (removeFromMap)
			{
				synchronized (tileDbMap)
				{
					TileDatabase db2 = tileDbMap.get(mapSourceName);
					if (db2 == this)
						tileDbMap.remove(mapSourceName);
				}
			}
			// DelayedInterruptThread t = (DelayedInterruptThread) Thread.currentThread();
			try
			{
				// t.pauseInterrupt();
				try
				{
					log.debug("Closing tile store db \"" + mapSourceName + "\"");
					if (store != null)
						store.close();
				}
				catch (Exception e)
				{
					log.error("", e);
				}
				try
				{
					env.close();
				}
				catch (Exception e)
				{
					log.error("", e);
				}
				finally
				{
					dbClosed = true;
				}
			}
			finally
			{
				// if (t.interruptedWhilePaused())
				// close();
				// t.resumeInterrupt();
			}
		}

		@Override
		protected void finalize() throws Throwable
		{
			close();
			super.finalize();
		}
	}

	@Override
	public Tile getTile(TileAddress tAddr, ACMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(TileAddress tAddr, ACMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
