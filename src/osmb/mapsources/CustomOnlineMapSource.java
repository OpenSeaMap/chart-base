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
import javax.xml.bind.annotation.XmlRootElement;

// W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileDownLoader;
import osmb.program.tilestore.TileStoreException;

/**
 * Custom online map source, configurable via 'MapSource.xml'.
 */
@XmlRootElement(name = "customMapSource")
public class CustomOnlineMapSource extends ACOnlineMapSource
{
	/**
	 * Constructor without parameters - required by JAXB
	 */
	protected CustomOnlineMapSource()
	{
		super();
	}

	public CustomOnlineMapSource(String name, String url) throws TileStoreException
	{
		this.mName = name;
		this.mUrl = url;
		mTS.prepareTileStore(this);
	}

	@Override
	public HttpURLConnection getTileUrlConnection(TileAddress tAddr) throws IOException
	{
		String url = getTileUrl(tAddr);
		if (url == null)
			return null;
		return (HttpURLConnection) new URL(url).openConnection();
	}

	@Override
	public String getTileUrl(TileAddress tAddr)
	{
		if (mServerParts == null || mServerParts.length == 0)
		{
			return MapSourceTools.formatMapUrl(mUrl, tAddr);
		}
		else
		{
			currentServerPart = (currentServerPart + 1) % mServerParts.length;
			String serverPart = mServerParts[currentServerPart];
			return MapSourceTools.formatMapUrl(mUrl, serverPart, tAddr);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] loadTileData(TileAddress tAddr)
	{
		log.debug("called with 'LoadMethod.SOURCE'");
		initialize();
		return TileDownLoader.downloadTileAndUpdateStore(tAddr, this);
		/*
		 * if (invertYCoordinate)
		 * y = ((1 << zoom) - y - 1);
		 * 
		 * if (loadMethod == LoadMethod.CACHE)
		 * {
		 * log.error("no image data from mtc");
		 * return null;
		 * }
		 * else if (loadMethod == LoadMethod.STORE)
		 * {
		 * IfTileStoreEntry entry = ACTileStore.getInstance().getTile(x, y, zoom, this);
		 * if (entry == null)
		 * return null;
		 * byte[] data = entry.getData();
		 * // if (Thread.currentThread() instanceof IfMapSourceListener)
		 * // {
		 * // ((IfMapSourceListener) Thread.currentThread()).tileDownloaded(data.length);
		 * // }
		 * return data;
		 * }
		 * // if (ignoreErrors)
		 * else if (loadMethod == LoadMethod.SOURCE)
		 * {
		 * try
		 * {
		 * return TileDownLoader.getTileData(x, y, zoom, this);
		 * }
		 * catch (Exception e)
		 * {
		 * log.error("download failed with exception: " + e.getCause());
		 * return null;
		 * }
		 * }
		 * else
		 * {
		 * return TileDownLoader.getTileData(x, y, zoom, this);
		 * }
		 */
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedImage loadTileImage(TileAddress tAddr)
	{
		byte[] data;
		try
		{
			data = loadTileData(tAddr);

			if (data == null)
			{
				// if (!mIgnoreErrors)
				// {
				// log.error("no image data: ");
				// return null;
				// }
				// else
				{
					int tileSize = MP2MapSpace.getTileSize();
					BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_4BYTE_ABGR);
					Graphics g = image.getGraphics();
					try
					{
						g.setColor(mBackgroundColor);
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
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public BufferedImage loadTileImageXX(TileAddress tAddr)
	{
		BufferedImage img = null;
		try
		{
			byte[] data = TileDownLoader.downloadTileAndUpdateStore(tAddr, this);
			if (data != null)
				img = ImageIO.read(new ByteArrayInputStream(data));
			else
			{
				log.error("no image data");
				return null;
			}
			img = ImageIO.read(new ByteArrayInputStream(data));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return img;
	}

	@Override
	public Color getBackgroundColor()
	{
		return mBackgroundColor;
	}

	@Override
	public void initialize()
	{
	}

	@Override
	public TileUpdate getTileUpdate()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
