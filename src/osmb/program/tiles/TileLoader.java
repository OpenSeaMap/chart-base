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

import java.io.IOException;

import org.apache.log4j.Logger;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.IfMapSourceListener;
import osmb.mapsources.TileAddress;
import osmb.program.JobDispatcher;
import osmb.program.tiles.Tile.TileState;
import osmb.program.tilestore.IfStoredTile;
import osmb.utilities.OSMBStrs;

/**
 * A {@link Runnable} providing implementation that loads tiles from some online map source via HTTP and saves all loaded files in a directory located in the
 * the temporary directory. If a tile is present in this file cache it will not be loaded from the online map source again.
 */
public class TileLoader
{
	private static Logger log = Logger.getLogger(TileLoader.class);

	protected IfTileLoaderListener listener = null;
	protected MemoryTileCache mMTC = null;

	public TileLoader(IfTileLoaderListener listener, MemoryTileCache mtc)
	{
		super();
		this.listener = listener;
		this.mMTC = mtc;
	}

	/**
	 * Creates a {@link Runnable} to load one tile from the specified map source. It informs the listener by calling tileLoadingFinished(tile, true) that the
	 * loading is finished. It tries to load the tile in sequence from MemoryTileCache, from TileStore and last from the online MapSource.
	 * If the tile is downloaded, it is automatically updated, or inserted if it did not yet exist, in the TileStore.
	 * 
	 * @param source
	 * @param tileXIdx
	 *          in tile indices
	 * @param tileYIdx
	 *          in tile indices
	 * @param zoom
	 * @return A {@link Runnable} to execute by some thread pool
	 */
	public Runnable createTileLoaderJob(final ACMapSource source, final TileAddress tAddr)
	{
		return new TileAsyncLoadJob(source, tAddr);
	}

	/**
	 * This class implements the actual tile loader. It usually is executed by a {@link JobDispatcher}.
	 * It first tries to load the tile from the tile store for the map source.
	 * If there is no tile in the store it returns an 'error' tile.
	 * If the tile in the store is expired, it tries to download an updated tile from the online map source.
	 * 
	 * @author humbach
	 */
	protected class TileAsyncLoadJob implements Runnable, IfMapSourceListener
	{
		final TileAddress mTAddr;
		final ACMapSource mMapSource;
		Tile mTile = null;
		// boolean fileTilePainted = false;
		protected IfStoredTile tileStoreEntry = null;

		public TileAsyncLoadJob(ACMapSource source, TileAddress tAddr)
		{
			super();
			this.mMapSource = source;
			mMapSource.initialize();
			mTAddr = tAddr;
		}

		/**
		 * Called by the executor, i.e. the {@link JobDispatcher}
		 */
		@Override
		public void run()
		{
			log.trace(OSMBStrs.RStr("START"));
			int sleepTime = 100;
			boolean bLoadOK = false;
			if (((mTile = mMTC.getTile(mMapSource, mTAddr)) != null) && (mTile.getTileState() != TileState.TS_LOADING) && (mTile.getTileState() != TileState.TS_NEW))
			{
				bLoadOK = true;
				log.debug("use " + mTile + " from mtc");
			}
			if ((mTile != null) && (mTile.getTileState() == TileState.TS_LOADING))
			{
				try
				{
					log.info(mTile + " size=" + mTile.getImageData().length);
				}
				catch (IOException e)
				{
				}
			}
			else
			{
				mTile = new Tile(mMapSource, mTAddr);
				log.debug("loading of " + mTile + " started");
				if (!(bLoadOK = loadTileFromStore()))
				{
					log.debug(mTile + " not found in tile store");
					synchronized (mTile)
					{
						while (!(bLoadOK = downloadAndUpdateTile()))
						{
							try
							{
								log.warn("loading of " + mTile + ", sleep stime=" + sleepTime / 1000.0 + "s");
								mTile.wait(sleepTime);
								if (sleepTime < 120000)
									sleepTime *= 2;
							}
							catch (InterruptedException e)
							{
								log.debug("loading of " + mTile + " interrupted");
							}
							catch (Exception e)
							{
								log.error("loading of " + mTile + " failed", e);
							}
						}
						log.debug("loading of " + mTile + " finished with stime=" + sleepTime / 1000.0 + "s");
					}
				}
				else
					log.debug(mTile + " loaded from store");
			}
			listener.tileLoadingFinished(mTile, bLoadOK);
			try
			{
				log.debug("loading of " + mTile + " finished, size=" + mTile.getImageData().length);
			}
			catch (IOException e)
			{
			}
		}

		/**
		 * This loads the tile from the tile store.
		 * If there is no tile in the store it returns an 'error' tile.
		 * If the tile in the store is expired, it tries to download an updated tile from the online map source.
		 * 
		 * @return TRUE if the tile found in the store is not expired. In all other cases it returns FALSE.
		 */
		private boolean loadTileFromStore()
		{
			log.trace(OSMBStrs.RStr("START"));
			boolean bLoadOK = false;
			try
			{
				Tile tmpTile = null;
				if ((tmpTile = mMapSource.getNTileStore().getTile(mTAddr)) != null)
				{
					mTile = tmpTile;
					if (mTile.getTileState() == TileState.TS_LOADED)
						bLoadOK = true;
				}
				if (!bLoadOK)
				{
					if ((tmpTile = mMapSource.getTileStore().getTile(mTAddr, mMapSource)) != null)
					{
						mTile = tmpTile;
						log.debug(mTile + " found in old store, put into new store");
						mMapSource.getNTileStore().putTile(mTile);
						if (mTile.getTileState() == TileState.TS_LOADED)
							bLoadOK = true;
					}
					else
						log.debug(mTile + " not found in TileStore");
				}
				if (bLoadOK && (mTile.isExpired()))
				{
					log.warn(mTile + " has expired");
					mTile.setTileState(TileState.TS_EXPIRED);
					bLoadOK = false;
				}
			}
			catch (Exception e)
			{
				log.error("Exception while load " + mTile + " from tile store", e);
			}
			return bLoadOK;
		}

		/**
		 * This loads the tile from the online map source. When the tile is downloaded successfully, it is put in the tile store by
		 * {@link ACMapSource.loadTile(TileAddress tAddr)}.
		 */
		private boolean downloadAndUpdateTile()
		{
			log.trace(OSMBStrs.RStr("START"));
			boolean bLoadOK = false;
			try
			{
				Tile tile = mMapSource.loadTile(mTAddr);
				if (tile != null)
				{
					mTile = tile;
					log.debug(mTile + " loaded from online map source");
					// mMapSource.getNTileStore().putTile(mTile);
					bLoadOK = true;
				}
				else
					log.info("no image for " + mTile + " received from online map source");
			}
			catch (Exception e)
			{
				log.error("Downloading of " + mTile + " failed", e);
				mTile.setErrorImage();
			}
			return bLoadOK;
		}

		@Override
		public void tileDownloaded(int size)
		{
			listener.tileDownloaded(mTile, size);
			log.info(mTile + " loaded from online map source, size=" + size);
		}

		@Override
		public void tileLoadedFromCache(int size)
		{
			listener.tileLoadedFromCache(mTile, size);
			log.info(mTile + " loaded from mtc, size=" + size);
		}
	}
}
