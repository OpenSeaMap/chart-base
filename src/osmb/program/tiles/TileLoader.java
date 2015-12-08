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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.IfMapSource.LoadMethod;
import osmb.program.JobDispatcher;
import osmb.program.tiles.Tile.TileState;
import osmb.program.tilestore.ACSiTileStore;
import osmb.program.tilestore.IfTileStoreEntry;

/**
 * A {@link Runnable} providing implementation that loads tiles from some online map source via HTTP and saves all loaded files in a directory located in the
 * the temporary directory. If a tile is present in this file cache it will not be loaded from the online map source again.
 */
public class TileLoader
{
	private static final Logger log = Logger.getLogger(TileLoader.class);

	protected IfTileLoaderListener listener;

	public TileLoader(IfTileLoaderListener listener)
	{
		super();
		this.listener = listener;
	}

	/**
	 * Creates a {@link Runnable} to download one tile from the specified map source. It informs the listener by calling tileLoadingFinished(tile, true) that the
	 * download is finished. The listener is responsible to store the downloaded data into a tile store.
	 * 
	 * @param source
	 * @param tilex
	 *          in tile coordinates
	 * @param tiley
	 *          in tile coordinates
	 * @param zoom
	 * @return A {@link Runnable} to execute by some thread pool
	 */
	public Runnable createTileLoaderJob(final IfMapSource source, final int tileX, final int tileY, final int zoom)
	{
		return new TileAsyncLoadJob(source, tileX, tileY, zoom);
	}

	/**
	 * This class implements the actual tile loader. It usually is executed by a {@link JobDispatcher}.
	 * It first tries to load the tile from the tile store for the map source.
	 * If there is no tile in the store it returns an 'error' tile.
	 * If the tile in the store is expired, it tries to download an updated tile from the online map source.
	 * 
	 * @author humbach
	 */
	protected class TileAsyncLoadJob implements Runnable
	{
		final int mTileX, mTileY, mZoom;
		final IfMapSource mMapSource;
		Tile mTile;
		// boolean fileTilePainted = false;
		protected IfTileStoreEntry tileStoreEntry = null;

		public TileAsyncLoadJob(IfMapSource source, int tilex, int tiley, int zoom)
		{
			super();
			this.mMapSource = source;
			this.mTileX = tilex;
			this.mTileY = tiley;
			this.mZoom = zoom;
		}

		/**
		 * Called by the executor, i.e. the {@link JobDispatcher}
		 */
		@Override
		public void run()
		{
			boolean bLoadOK = false;
			// We don't use the memory cache here currently (2015).
			// The check if the tile is available in the memory cache should be done before starting a loader job.
			// MemoryTileCache cache = listener.getTileImageCache();
			// if (cache != null)
			// {
			// // log.debug("Cache=" + cache.toString());
			// synchronized (cache)
			// {
			// mTile = cache.getTile(mMapSource, mTileX, mTileY, mZoom);
			// if (mTile == null || mTile.mTileState != TileState.TS_NEW)
			// return;
			// mTile.setTileState(TileState.TS_LOADING);
			// }
			// }
			// else
			mTile = new Tile(mMapSource, mTileX, mTileY, mZoom);
			log.trace(mTile);
			log.debug("load " + mTile + " started");
			if (!(bLoadOK = loadTileFromStore()))
				bLoadOK = loadAndUpdateTile();
			listener.tileLoadingFinished(mTile, bLoadOK);
			log.debug("load " + mTile + " finished");
		}

		/**
		 * This loads the tile from the tile store.
		 * If there is no tile in the store it returns an 'error' tile.
		 * If the tile in the store is expired, it tries to download an updated tile from the online map source.
		 * sometime the result seems strange
		 * 
		 * @return TRUE if the tile found in the store is not expired. In all other cases it returns FALSE.
		 */
		private boolean loadTileFromStore()
		{
			boolean bLoadOK = false;
			try
			{
				BufferedImage image = mMapSource.getTileImage(mZoom, mTileX, mTileY, LoadMethod.STORE);
				if (image == null)
				{
					log.warn("tile " + mTile + " not in store -> use empty and load from online");
					mTile.setErrorImage();
				}
				else
				{
					mTile.setTileState(Tile.TileState.TS_LOADED);
					mTile.setImage(image);
					// listener.tileLoadingFinished(mTile, true);

					tileStoreEntry = ACSiTileStore.getInstance().getTile(mTileX, mTileY, mZoom, mMapSource);

					if (ACSiTileStore.getInstance().isTileExpired(tileStoreEntry))
					{
						mTile.setTileState(Tile.TileState.TS_EXPIRED);
						log.warn("expired tile " + mTile + " in store -> use old and load from online");
					}
					else
					{
						bLoadOK = true;
						log.debug("tile " + mTile + " loaded from store");
					}
				}
			}
			catch (Exception e)
			{
				log.error("Failed to load tile " + mTile + " from tile store", e);
			}
			return bLoadOK;
		}

		/**
		 * This loads the tile from the online map source. The caller has to push it into the tile store.
		 */
		private boolean loadAndUpdateTile()
		{
			boolean bLoadOK = false;
			try
			{
				BufferedImage image = mMapSource.getTileImage(mZoom, mTileX, mTileY, LoadMethod.SOURCE);
				if (image != null)
				{
					mTile.setImage(image);
					mTile.setTileState(TileState.TS_LOADED);
					bLoadOK = true;
					log.debug("tile " + mTile + " loaded from online map source");
				}
			}
			catch (ConnectException e)
			{
				log.warn("Downloading of " + mTile + " failed: " + e.getMessage());
				mTile.setErrorImage();
			}
			catch (DownloadFailedException e)
			{
				log.warn("Downloading of " + mTile + " failed: " + e.getMessage());
				mTile.setErrorImage();
			}
			catch (IOException e)
			{
				log.warn("Downloading of " + mTile + " failed: " + e.getMessage());
				mTile.setErrorImage();
			}
			catch (Exception e)
			{
				log.warn("Downloading of " + mTile + " failed", e);
				mTile.setErrorImage();
			}
			listener.tileLoadingFinished(mTile, bLoadOK);
			return bLoadOK;
		}

	}
}
