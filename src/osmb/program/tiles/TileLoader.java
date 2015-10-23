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
import osmb.program.tiles.Tile.TileState;
import osmb.program.tilestore.IfTileStoreEntry;

/**
 * A {@link Runnable} providing implementation that loads tiles from some online map source via HTTP and saves all loaded files in a directory located in the
 * the temporary directory. If a tile is present in this file cache it will not be loaded from the online map source again.
 */
public class TileLoader
{
	private static final Logger log = Logger.getLogger(TileLoader.class);

	// protected ACSiTileStore tileStore;
	protected IfTileLoaderListener listener;

	public TileLoader(IfTileLoaderListener listener)
	{
		super();
		this.listener = listener;
		// tileStore = ACSiTileStore.getInstance();
	}

	/**
	 * Creates a {@link Runnable} to download one tile from the specified map source. It informs the listener by calling tileLoadingFinished(tile, true) that the
	 * download is finished. The listener is responsible to store the downloaded data into a tile store.
	 * 
	 * @param source
	 * @param tilex
	 * @param tiley
	 * @param zoom
	 * @return A {@link Runnable} to execute by some thread pool
	 */
	public Runnable createTileLoaderJob(final IfMapSource source, final int tilex, final int tiley, final int zoom)
	{
		return new TileAsyncLoadJob(source, tilex, tiley, zoom);
	}

	protected class TileAsyncLoadJob implements Runnable
	{
		final int mTileX, mTileY, mZoom;
		final IfMapSource mMapSource;
		Tile mTile;
		boolean fileTilePainted = false;
		protected IfTileStoreEntry tileStoreEntry = null;

		public TileAsyncLoadJob(IfMapSource source, int tilex, int tiley, int zoom)
		{
			super();
			this.mMapSource = source;
			this.mTileX = tilex;
			this.mTileY = tiley;
			this.mZoom = zoom;
		}

		@Override
		public void run()
		{
			MemoryTileCache cache = listener.getTileImageCache();
			if (cache != null)
			{
				// log.debug("Cache=" + cache.toString());
				synchronized (cache)
				{
					mTile = cache.getTile(mMapSource, mTileX, mTileY, mZoom);
					if (mTile == null || mTile.mTileState != TileState.TS_NEW)
						return;
					mTile.setTileState(TileState.TS_LOADING);
				}
			}
			else
				mTile = new Tile(mMapSource, mTileX, mTileY, mZoom);
			log.trace(mTile);
			if (loadTileFromStore())
				return;
			// if (fileTilePainted)
			// {
			// Runnable job = new Runnable()
			// {
			// @Override
			// public void run()
			// {
			// loadOrUpdateTile();
			// }
			// };
			// }
			// else
			// {
			loadOrUpdateTile();
			// }
		}

		/**
		 * This loads the tile from the online map source. The caller has to push it into the tile store.
		 */
		protected void loadOrUpdateTile()
		{
			try
			{
				BufferedImage image = mMapSource.getTileImage(mZoom, mTileX, mTileY, LoadMethod.DEFAULT);
				if (image != null)
				{
					mTile.setImage(image);
					mTile.setTileState(TileState.TS_LOADED);
					log.debug("tile " + mTile + " loaded from online map source");
					listener.tileLoadingFinished(mTile, true);
				}
				else
				{
					mTile.setErrorImage();
					listener.tileLoadingFinished(mTile, false);
				}
				return;
			}
			catch (ConnectException e)
			{
				log.warn("Downloading of " + mTile + " failed: " + e.getMessage());
			}
			catch (DownloadFailedException e)
			{
				log.warn("Downloading of " + mTile + " failed: " + e.getMessage());
			}
			catch (IOException e)
			{
				log.debug("Downloading of " + mTile + " failed: " + e.getMessage());
			}
			catch (Exception e)
			{
				log.debug("Downloading of " + mTile + " failed", e);
			}
			mTile.setErrorImage();
			listener.tileLoadingFinished(mTile, false);
		}

		protected boolean loadTileFromStore()
		{
			try
			{
				BufferedImage image = mMapSource.getTileImage(mZoom, mTileX, mTileY, LoadMethod.CACHE);
				if (image == null)
				{
					log.debug("tile (" + mZoom + "|" + mTileX + "|" + mTileY + ")  not in store -> load from online");
					return false;
				}
				mTile.setImage(image);
				listener.tileLoadingFinished(mTile, true);
				if (TileDownLoader.isTileExpired(tileStoreEntry))
				{
					log.debug("expired tile (" + mZoom + "|" + mTileX + "|" + mTileY + ")  in store -> load from online");
					return false;
				}
				fileTilePainted = true;
				log.debug("tile (" + mZoom + "|" + mTileX + "|" + mTileY + ")  loaded from store");
				return true;
			}
			catch (Exception e)
			{
				log.error("Failed to load tile (" + mZoom + "|" + mTileX + "|" + mTileY + ") from tile store", e);
			}
			return false;
		}
	}
}
