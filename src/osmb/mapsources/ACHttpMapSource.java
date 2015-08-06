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
package osmb.mapsources;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileDownLoader;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.program.tilestore.ACSiTileStore;
import osmb.program.tilestore.IfTileStoreEntry;

//import osmcd.gui.mapview.JMapViewer;

/**
 * Abstract base class for map sources.
 */
public abstract class ACHttpMapSource implements IfHttpMapSource
{
	protected Logger log;
	private boolean initialized = false;
	protected String name;
	protected int minZoom;
	protected int maxZoom;
	protected TileImageType tileType;
	protected IfHttpMapSource.TileUpdate tileUpdate;
	protected IfMapSpace mapSpace = MercatorPower2MapSpace.INSTANCE_256;
	protected MapSourceLoaderInfo loaderInfo = null;

	public ACHttpMapSource(String name, int minZoom, int maxZoom, TileImageType tileType)
	{
		this(name, minZoom, maxZoom, tileType, IfHttpMapSource.TileUpdate.None);
	}

	/**
	 * Do not use - for JAXB only
	 */
	protected ACHttpMapSource()
	{
	}

	public ACHttpMapSource(String name, int minZoom, int maxZoom, TileImageType tileType, IfHttpMapSource.TileUpdate tileUpdate)
	{
		log = Logger.getLogger(this.getClass());
		this.name = name;
		this.minZoom = minZoom;
		this.maxZoom = Math.min(maxZoom, 18);
		this.tileType = tileType;
		this.tileUpdate = tileUpdate;
	}

	public boolean ignoreContentMismatch()
	{
		return false;
	}

	@Override
	public HttpURLConnection getTileUrlConnection(int zoom, int tilex, int tiley) throws IOException
	{
		String url = getTileUrl(zoom, tilex, tiley);
		if (url == null)
			return null;
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		prepareTileUrlConnection(conn);
		return conn;
	}

	protected void prepareTileUrlConnection(HttpURLConnection conn)
	{
		// Derived classes may override this method
	}

	public abstract String getTileUrl(int zoom, int tilex, int tiley);

	/**
	 * Can be used to e.g. retrieve the url pattern before the first call
	 */
	protected final void initializeHttpMapSource()
	{
		if (initialized)
			return;
		// Prevent multiple initializations in case of multi-threaded access
		try
		{
			synchronized (this)
			{
				if (initialized)
					// Another thread has already completed initialization while this one was blocked
					return;
				initernalInitialize();
				initialized = true;
				log.debug("Map source has been initialized");
			}
		}
		catch (Exception e)
		{
			log.error("Map source initialization failed: " + e.getMessage(), e);
			// TODO: inform user
		}
		initialized = true;
	}

	protected void initernalInitialize() throws MapSourceInitializationException
	{
	}

	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException
	{
		if (loadMethod == LoadMethod.CACHE)
		{
			IfTileStoreEntry entry = ACSiTileStore.getInstance().getTile(x, y, zoom, this);
			if (entry == null)
				return null;
			byte[] data = entry.getData();
			if (Thread.currentThread() instanceof IfMapSourceListener)
			{
				((IfMapSourceListener) Thread.currentThread()).tileDownloaded(data.length);
			}
			return data;
		}
		else if (loadMethod == LoadMethod.SOURCE)
		{
			initializeHttpMapSource();
			return TileDownLoader.downloadTileAndUpdateStore(x, y, zoom, this);
		}
		else
		{
			initializeHttpMapSource();
			return TileDownLoader.getImage(x, y, zoom, this);
		}
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException
	{
		byte[] data = getTileData(zoom, x, y, loadMethod);
		if (data == null)
			return null;
		return ImageIO.read(new ByteArrayInputStream(data));
	}

	@Override
	public int getMaxZoom()
	{
		return maxZoom;
	}

	@Override
	public int getMinZoom()
	{
		return minZoom;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public String getStoreName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public TileImageType getTileImageType()
	{
		return tileType;
	}

	@Override
	public IfHttpMapSource.TileUpdate getTileUpdate()
	{
		return tileUpdate;
	}

	public boolean allowFileStore()
	{
		return true;
	}

	@Override
	public IfMapSpace getMapSpace()
	{
		return mapSpace;
	}

	@Override
	public Color getBackgroundColor()
	{
		return Color.BLACK;
	}

	@Override
	public MapSourceLoaderInfo getLoaderInfo()
	{
		return loaderInfo;
	}

	@Override
	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo)
	{
		if (this.loaderInfo != null)
			throw new RuntimeException("LoaderInfo already set");
		this.loaderInfo = loaderInfo;
	}

	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IfMapSource))
			return false;
		IfMapSource other = (IfMapSource) obj;
		return other.getName().equals(getName());
	}
}
