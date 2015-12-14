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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

// W #mapSpace import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.jaxb.ColorAdapter;
// W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileDownLoader;
import osmb.program.tiles.TileImageType;
import osmb.program.tiles.UnrecoverableDownloadException;
import osmb.program.tilestore.ACSiTileStore;
import osmb.program.tilestore.IfTileStoreEntry;

/**
 * Custom tile store provider, configurable via settings.xml.
 */
@XmlRootElement
public class CustomMapSource implements IfHttpMapSource
{

	@XmlElement(nillable = false, defaultValue = "Custom")
	private String name = "Custom";

	@XmlElement(defaultValue = "0")
	private int minZoom = 0;

	@XmlElement(required = true)
	private int maxZoom = 0;

	@XmlElement(defaultValue = "PNG")
	protected TileImageType tileType = TileImageType.PNG;

	@XmlElement(defaultValue = "NONE")
	private IfHttpMapSource.TileUpdate tileUpdate;

	@XmlElement(required = true, nillable = false)
	protected String url = "http://127.0.0.1/{$x}_{$y}_{$z}";

	@XmlElement(defaultValue = "false")
	private boolean invertYCoordinate = false;

	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	private Color backgroundColor = Color.BLACK;

	@XmlElement(required = false, defaultValue = "false")
	private boolean ignoreErrors = false;

	@XmlElement(required = false, defaultValue = "")
	@XmlList
	private String[] serverParts = null;
	private int currentServerPart = 0;

	// @XmlElement(required = false, defaultValue = "false")
	// protected boolean ignoreContentMismatch = false;

	private MapSourceLoaderInfo loaderInfo = null;

	/**
	 * Constructor without parameters - required by JAXB
	 */
	protected CustomMapSource()
	{
	}

	public CustomMapSource(String name, String url)
	{
		this.name = name;
		this.url = url;
	}

	// public boolean ignoreContentMismatch()
	// {
	// return ignoreContentMismatch;
	// }

	@Override
	public TileUpdate getTileUpdate()
	{
		return tileUpdate;
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
	public TileImageType getTileImageType()
	{
		return tileType;
	}

	@Override
	public HttpURLConnection getTileUrlConnection(int zoom, int tilex, int tiley) throws IOException
	{
		String url = getTileUrl(zoom, tilex, tiley);
		if (url == null)
			return null;
		return (HttpURLConnection) new URL(url).openConnection();
	}

	public String getTileUrl(int zoom, int tilex, int tiley)
	{
		if (serverParts == null || serverParts.length == 0)
		{
			return MapSourceTools.formatMapUrl(url, zoom, tilex, tiley);
		}
		else
		{
			currentServerPart = (currentServerPart + 1) % serverParts.length;
			String serverPart = serverParts[currentServerPart];
			return MapSourceTools.formatMapUrl(url, serverPart, zoom, tilex, tiley);
		}
	}

	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, UnrecoverableDownloadException, InterruptedException
	{
		if (invertYCoordinate)
			y = ((1 << zoom) - y - 1);

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
		if (ignoreErrors)
		{
			try
			{
				return TileDownLoader.getTileData(x, y, zoom, this);
			}
			catch (Exception e)
			{
				return null;
			}
		}
		else
		{
			return TileDownLoader.getTileData(x, y, zoom, this);
		}
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, UnrecoverableDownloadException, InterruptedException
	{

		byte[] data = getTileData(zoom, x, y, loadMethod);

		if (data == null)
		{
			if (!ignoreErrors)
				return null;
			else
			{
				int tileSize = MP2MapSpace.getTileSize(); // #mapSpace  this.getMapSpace().getTileSize();
				BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_4BYTE_ABGR);
				Graphics g = image.getGraphics();
				try
				{
					g.setColor(backgroundColor);
					g.fillRect(0, 0, tileSize, tileSize);
				}
				finally
				{
					g.dispose();
				}
				return image;
			}
		}
		else
		{
			return ImageIO.read(new ByteArrayInputStream(data));
		}
	}

	@Override
	public String toString()
	{
		return name;
	}

// #mapSpace  
//	@Override
//	public IfMapSpace getMapSpace()
//	{
//		return MercatorPower2MapSpace.INSTANCE_256; // W #mapSpace =
//	}

	@Override
	public Color getBackgroundColor()
	{
		return backgroundColor;
	}

	@Override
	@XmlTransient
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
}
