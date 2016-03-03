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

import org.apache.log4j.Logger;

import osmb.program.ACSettings;
//W #mapSpace	import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.program.tilestore.ACTileStore;

public abstract class ACMultiLayerMapSource extends ACMapSource implements Iterable<ACMapSource>
{
	protected ACMapSource[] mapSources;

	protected ACMultiLayerMapSource()
	{
		super();
	}

	public ACMultiLayerMapSource(String name, TileImageType tileImageType)
	{
		super();
		log = Logger.getLogger(this.getClass());
		this.mName = name;
		this.mTileType = tileImageType;
	}

	protected void initializeValues()
	{
		log.trace("START");
		@SuppressWarnings("unused") // W #unused
		ACMapSource refMapSource = mapSources[0];
		mMaxZoom = MP2MapSpace.MAX_TECH_ZOOM; // 18; // W MAX_TECH_ZOOM
		mMinZoom = MP2MapSpace.MIN_TECH_ZOOM; // 0;
		for (ACMapSource ms : mapSources)
		{
			mMaxZoom = Math.min(mMaxZoom, ms.getMaxZoom());
			mMinZoom = Math.max(mMinZoom, ms.getMinZoom());
		}
	}

	@Override
	public void initialize()
	{
		log.trace("START");
		initializeValues();
	}

	public ACMapSource[] getLayerMapSources()
	{
		return mapSources;
	}

	@Override
	public Color getBackgroundColor()
	{
		return Color.BLACK;
	}

	@Override
	public int getMaxZoom()
	{
		return mMaxZoom;
	}

	@Override
	public int getMinZoom()
	{
		return mMinZoom;
	}

	@Override
	public String getName()
	{
		return mName;
	}

	/**
	 * @return The name to be used by the tile store. The layer of a MultiLayerMapSource are stored in separate tile stores.
	 *         Each layer is a map source in itself, so use that map sources name.
	 */
	public String getStoreName()
	{
		return null;
	}

	/**
	 * This loads the image data via {@link #getTileImage}() and converts them by {@link ImageIO.write}() into a byte array.
	 */
	@Override
	public byte[] getTileData(int zoom, int x, int y) throws IOException, InterruptedException, TileException
	{
		log.trace("START");
		ByteArrayOutputStream buf = new ByteArrayOutputStream(16000);
		BufferedImage image = getTileImage(zoom, x, y);
		if (image == null)
		{
			log.debug("tile not found:(" + zoom + "|" + x + "|" + y + ")");
			return null;
		}
		ImageIO.write(image, mTileType.getFileExt(), buf);
		log.debug("tile written:(" + zoom + "|" + x + "|" + y + ")");
		return buf.toByteArray();
	}

	/**
	 * This loads the image via {@link IfMapSource.getTileImage}() for each layer in this multi layer map source.
	 * When all layer images are loaded, they were combined via Graphics2D.drawImage().
	 */
	@Override
	public BufferedImage getTileImage(int zoom, int x, int y) throws IOException, InterruptedException, TileException
	{
		log.trace("START");
		BufferedImage image = null;
		Graphics2D g2 = null;
		try
		{
			ArrayList<BufferedImage> layerImages = new ArrayList<BufferedImage>(mapSources.length);
			int maxSize = MP2MapSpace.getTileSize();
			for (int i = 0; i < mapSources.length; i++)
			{
				ACMapSource layerMapSource = mapSources[i];
				BufferedImage layerImage = layerMapSource.getTileImage(zoom, x, y);
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
				ImageIO.write(image, mTileType.getFileExt(), buf);
				log.trace("tile written:(" + zoom + "|" + x + "|" + y + ")");
				long timeLastModified = System.currentTimeMillis();
				long timeExpires = timeLastModified + ACSettings.getTileDefaultExpirationTime();
				log.debug("put composed tile:(" + zoom + "|" + x + "|" + y + ") into tile store");
				ACTileStore.getInstance().putTileData(buf.toByteArray(), x, y, zoom, this, timeLastModified, timeExpires, "-");
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

	public BufferedImage downloadTileImage(int zoom, int x, int y) throws IOException, TileException, InterruptedException
	{
		return getTileImage(zoom, x, y);
	}

	protected float getLayerAlpha(int layerIndex)
	{
		return 1.0f;
	}

	@Override
	public Iterator<ACMapSource> iterator()
	{
		return Arrays.asList(mapSources).iterator();
	}

}
