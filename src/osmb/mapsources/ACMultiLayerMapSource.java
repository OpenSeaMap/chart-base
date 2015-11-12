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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;

import osmb.program.ACSettings;
import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.program.tilestore.ACSiTileStore;

public abstract class ACMultiLayerMapSource implements IfInitializableMapSource, Iterable<IfMapSource>
{
	protected Logger log;

	protected String name = "";
	protected TileImageType tileType = TileImageType.PNG;
	protected IfMapSource[] mapSources;

	private int maxZoom;
	private int minZoom;
	private IfMapSpace mapSpace;
	protected MapSourceLoaderInfo loaderInfo = null;

	public ACMultiLayerMapSource(String name, TileImageType tileImageType)
	{
		this();
		this.name = name;
		this.tileType = tileImageType;
	}

	protected ACMultiLayerMapSource()
	{
		log = Logger.getLogger(this.getClass());
	}

	protected void initializeValues()
	{
		log.trace("START");
		IfMapSource refMapSource = mapSources[0];
		mapSpace = refMapSource.getMapSpace();
		maxZoom = 18;
		minZoom = 0;
		for (IfMapSource ms : mapSources)
		{
			maxZoom = Math.min(maxZoom, ms.getMaxZoom());
			minZoom = Math.max(minZoom, ms.getMinZoom());
			if (!ms.getMapSpace().equals(mapSpace))
				throw new RuntimeException("Different map spaces used in multi-layer map source");
		}
	}

	@Override
	public void initialize()
	{
		log.trace("START");
		IfMapSource refMapSource = mapSources[0];
		mapSpace = refMapSource.getMapSpace();
		maxZoom = 18;
		minZoom = 0;
		for (IfMapSource ms : mapSources)
		{
			if (ms instanceof IfInitializableMapSource)
				((IfInitializableMapSource) ms).initialize();
			maxZoom = Math.min(maxZoom, ms.getMaxZoom());
			minZoom = Math.max(minZoom, ms.getMinZoom());
		}
	}

	public IfMapSource[] getLayerMapSources()
	{
		return mapSources;
	}

	@Override
	public Color getBackgroundColor()
	{
		return Color.BLACK;
	}

	@Override
	public IfMapSpace getMapSpace()
	{
		return mapSpace;
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
		return null;
	}

	/**
	 * This loads the image data via {@link #getTileImage}() and converts them by {@link ImageIO.write}() into a byte array.
	 */
	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, InterruptedException, TileException
	{
		log.trace("START");
		ByteArrayOutputStream buf = new ByteArrayOutputStream(16000);
		BufferedImage image = getTileImage(zoom, x, y, loadMethod);
		if (image == null)
		{
			log.debug("tile not found:(" + zoom + "|" + x + "|" + y + ")");
			return null;
		}
		ImageIO.write(image, tileType.getFileExt(), buf);
		log.debug("tile written:(" + zoom + "|" + x + "|" + y + ")");
		return buf.toByteArray();
	}

	/**
	 * This loads the image via {@link IfMapSource.getTileImage}() for each layer in this multi layer map source.
	 * When all layer images are loaded, they were combined via Graphics2D.drawImage().
	 */
	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, InterruptedException, TileException
	{
		log.trace("START");
		BufferedImage image = null;
		Graphics2D g2 = null;
		try
		{
			ArrayList<BufferedImage> layerImages = new ArrayList<BufferedImage>(mapSources.length);
			int maxSize = mapSpace.getTileSize();
			for (int i = 0; i < mapSources.length; i++)
			{
				IfMapSource layerMapSource = mapSources[i];
				BufferedImage layerImage = layerMapSource.getTileImage(zoom, x, y, loadMethod);
				if (layerImage != null)
				{
					log.debug("Multi layer loaded: '" + layerMapSource + "' (" + zoom + "|" + x + "|" + y + ") into Layer=" + i);
					layerImages.add(layerImage);
					int size = layerImage.getWidth();
					if (size > maxSize)
					{
						maxSize = size;
					}
				}
				else
					log.debug("Multi layer empty: " + layerMapSource + "' (" + zoom + "|" + x + "|" + y + ") into Layer=" + i);
			}

			// optimize for when only one layer exist
			if (layerImages.size() == 1)
			{
				return layerImages.get(0);
			}
			else if (layerImages.size() > 1)
			{
				image = new BufferedImage(maxSize, maxSize, BufferedImage.TYPE_3BYTE_BGR);
				g2 = image.createGraphics();
				g2.setColor(getBackgroundColor());
				g2.fillRect(0, 0, maxSize, maxSize);

				for (int i = 0; i < layerImages.size(); i++)
				{
					BufferedImage layerImage = layerImages.get(i);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getLayerAlpha(i)));
					g2.drawImage(layerImage, 0, 0, maxSize, maxSize, null);
					log.debug("Multi layer added: Alpha=" + getLayerAlpha(i) + "; Layer=" + i);
				}

				ByteArrayOutputStream buf = new ByteArrayOutputStream(32000);
				ImageIO.write(image, tileType.getFileExt(), buf);
				log.trace("tile written:(" + zoom + "|" + x + "|" + y + ")");
				long timeLastModified = System.currentTimeMillis();
				long timeExpires = timeLastModified + ACSettings.getTileDefaultExpirationTime();
				log.debug("put composed tile:(" + zoom + "|" + x + "|" + y + ") into tile store");
				ACSiTileStore.getInstance().putTileData(buf.toByteArray(), x, y, zoom, this, timeLastModified, timeExpires, "-");
				return image;
			}
			else
			{
				return null;
			}
		}
		finally
		{
			if (g2 != null)
			{
				g2.dispose();
			}
		}
	}

	protected float getLayerAlpha(int layerIndex)
	{
		return 1.0f;
	}

	@Override
	public TileImageType getTileImageType()
	{
		return tileType;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public Iterator<IfMapSource> iterator()
	{
		return Arrays.asList(mapSources).iterator();
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
		log.trace("START");
		if (this.loaderInfo != null)
			throw new RuntimeException("LoaderInfo already set");
		this.loaderInfo = loaderInfo;
	}
}
