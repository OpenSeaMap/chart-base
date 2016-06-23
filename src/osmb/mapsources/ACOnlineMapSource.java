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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;

import org.apache.log4j.Logger;

import osmb.program.tiles.Tile;
//import osmb.mapsources.mapspace.MercatorPower2MapSpace; // W #mapSpace
//import osmb.program.map.IfMapSpace; // W #mapSpace
import osmb.program.tiles.TileDownLoader;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.program.tiles.UnrecoverableDownloadException;
import osmb.utilities.OSMBStrs;

/**
 * Abstract base class for map sources.
 */
public abstract class ACOnlineMapSource extends ACMapSource implements IfOnlineMapSource
{
	// class data

	// instance data
	@XmlElement(name = "tileUpdate", required = true, defaultValue = "NONE")
	protected TileUpdate mTileUpdate;

	@XmlElement(name = "url", required = true, nillable = false, defaultValue = "http://127.0.0.1/{$x}_{$y}_{$z}")
	protected String mUrl = "http://127.0.0.1/{$x}_{$y}_{$z}";

	@XmlElement(name = "serverParts", required = false, defaultValue = "")
	@XmlList
	protected String[] mServerParts = null;
	protected int currentServerPart = 0;

	@XmlElement(name = "invertYCoordinate", required = false, defaultValue = "false")
	protected boolean mInvertYCoordinate = false;

	@XmlElement(name = "ignoreErrors", required = false, defaultValue = "false")
	protected boolean mIgnoreErrors = false;

	public ACOnlineMapSource(String name, int minZoom, int maxZoom, TileImageType tileType)
	{
		this(name, minZoom, maxZoom, tileType, IfOnlineMapSource.TileUpdate.None);
	}

	/**
	 * Do not use - for JAXB only
	 */
	protected ACOnlineMapSource()
	{
	}

	/**
	 * Creates a new map source able to connect and retrieve tiles from an online source by http requests.
	 * 
	 * @param name
	 *          The map sources name.
	 * @param minZoom
	 *          The smallest zoom level this map source supports.
	 * @param maxZoom
	 *          The highest zoom level this map source supports.
	 * @param tileType
	 *          The image file type of the tiles as delivered by the map source.
	 * @param tileUpdate
	 *          The updating mechanism supported by this map source.
	 */
	public ACOnlineMapSource(String name, int minZoom, int maxZoom, TileImageType tileType, IfOnlineMapSource.TileUpdate tileUpdate)
	{
		log = Logger.getLogger(this.getClass());
		this.mName = name;
		this.mMinZoom = minZoom;
		this.mMaxZoom = Math.min(maxZoom, MP2MapSpace.MAX_TECH_ZOOM);
		this.mTileType = tileType;
		this.mTileUpdate = tileUpdate;
	}

	@Override
	public HttpURLConnection getTileUrlConnection(TileAddress tAddr) throws IOException
	{
		String url = getTileUrl(tAddr);
		if (url == null)
			return null;
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		prepareTileUrlConnection(conn);
		return conn;
	}

	@Override
	public String getTileUrl(TileAddress tAddr)
	{
		String strUrl = null;
		return strUrl;
	}

	protected void prepareTileUrlConnection(HttpURLConnection conn)
	{
	}

	/**
	 * This retrieves the tile image as a byte array either from the online source or from the tile store depending on the specified loadMethod.
	 */
	@Override
	public byte[] loadTileData(TileAddress tAddr)
	{
		initialize();
		return TileDownLoader.downloadTileAndUpdateStore(tAddr, this);
	}

	@Override
	public Tile loadTile(TileAddress tAddr) throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		Tile tile = null;
		tile = TileDownLoader.downloadTile(tAddr, this);
		if (tile != null)
		{
			mNTS.putTile(tile);
			log.debug("put " + tile + " into TS " + mNTS);
		}
		return tile;
	}

	@Override
	public Tile updateTile(Tile tTile)
	{
		log.trace(OSMBStrs.RStr("START"));
		Tile tile = null;
		try
		{
			tile = TileDownLoader.updateTile(tTile, this);
		}
		catch (UnrecoverableDownloadException | IOException e)
		{
			log.error("download timeout " + tTile);
		}
		if (tile != null)
		{
			mNTS.putTile(tile);
			log.debug("put " + tile + " into TS " + mNTS);
		}
		return tile;
	}

	/**
	 * This retrieves the tile image as an {@link BufferedImage} either from the online source or from the tile store depending on the specified loadMethod.
	 * It simply converts the result of {@link #getTileData}().
	 */
	@Override
	public BufferedImage loadTileImage(TileAddress tAddr)
	{
		log.warn(OSMBStrs.RStr("START"));
		BufferedImage img = null;
		try
		{
			byte[] data = loadTileData(tAddr);
			if (data != null)
			{
				img = ImageIO.read(new ByteArrayInputStream(data));
			}
		}
		catch (IOException e)
		{
			log.error("loading of " + tAddr + " failed", e);
			e.printStackTrace();
		}
		return img;
	}

	/**
	 * This retrieves the tile image directly from the online map source.
	 * <p>
	 * 
	 * @param zoom
	 * @param x
	 * @param y
	 * @return The tile data as an image or null if no image available.
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	public BufferedImage downloadTileImage(TileAddress tAddr)
	{
		log.warn(OSMBStrs.RStr("START"));
		BufferedImage img = null;
		initialize();
		byte[] data = null;
		try
		{
			data = TileDownLoader.downloadTileAndUpdateStore(tAddr, this);
			img = ImageIO.read(new ByteArrayInputStream(data));
		}
		catch (IOException e)
		{
			log.error("loading of " + tAddr + " failed", e);
			e.printStackTrace();
		}
		if (img == null)
		{
			log.error("no image data");
		}
		return img;
	}

	@Override
	public IfOnlineMapSource.TileUpdate getTileUpdate()
	{
		return mTileUpdate;
	}

}
