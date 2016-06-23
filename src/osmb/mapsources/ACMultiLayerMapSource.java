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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import osmb.program.ACSettings;
import osmb.program.tiles.Tile;
import osmb.program.tiles.Tile.TileState;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.program.tilestore.ACTileStore;
import osmb.utilities.OSMBStrs;

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
		log.trace(OSMBStrs.RStr("START"));
		mMaxZoom = MP2MapSpace.MAX_TECH_ZOOM;
		mMinZoom = MP2MapSpace.MIN_TECH_ZOOM;
		for (ACMapSource ms : mapSources)
		{
			ms.initialize();
			mMaxZoom = Math.min(mMaxZoom, ms.getMaxZoom());
			mMinZoom = Math.max(mMinZoom, ms.getMinZoom());
		}
	}

	@Override
	public void initialize()
	{
		log.trace(OSMBStrs.RStr("START"));
		initializeValues();
	}

	public ACMapSource[] getLayerMapSources()
	{
		return mapSources;
	}

	// @Override
	// public Color getBackgroundColor()
	// {
	// return Color.BLACK;
	// }

	/**
	 * This loads the image data via {@link #getTileImage}() and converts them via {@link ImageIO.write}() into a byte array.
	 */
	@Override
	@Deprecated
	public byte[] getTileData(int zoom, int x, int y) throws IOException, InterruptedException, TileException
	{
		log.trace(OSMBStrs.RStr("START"));
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
	 * 
	 * @throws IOException
	 */
	@Override
	public Tile loadTile(TileAddress tAddr) throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		Tile tile = new Tile(this, tAddr);
		BufferedImage image = null;
		Graphics2D g2 = null;
		try
		{
			ArrayList<Tile> layerTiles = new ArrayList<Tile>(mapSources.length);
			int maxSize = MP2MapSpace.getTileSize();
			for (int i = 0; i < mapSources.length; i++)
			{
				ACMapSource layerMapSource = mapSources[i];
				// try to load the layer tile from mtc
				Tile layerTile = null;
				if (mMTC != null)
				{
					layerTile = mMTC.getTile(layerMapSource, tAddr);
					if (layerTile.getTileState() == TileState.TS_LOADING)
						log.debug("'loading' tile found in mtc: " + layerTile);
				}
				while (layerTile == null)
				{
					// try to load the tile from the tile store
					layerTile = layerMapSource.loadTile(tAddr);
					if (layerTile != null)
					{
						log.debug("Multi layer loaded: '" + layerMapSource.getName() + "' (" + tAddr + ") into Layer=" + i);
						layerTiles.add(layerTile);
						if (layerTile.getTileState() == TileState.TS_LOADING)
							log.debug("'loading' tile found in ts: " + layerTile + " for " + layerMapSource.getName());
					}
					else
						log.warn("Multi layer empty: " + layerMapSource.getName() + "' (" + tAddr + ") into Layer=" + i + ", wait and retry");
					// tile.wait(20000);
				}
			}
			if (layerTiles.size() > 0)
			{
				image = new BufferedImage(MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE, BufferedImage.TYPE_3BYTE_BGR);
				g2 = image.createGraphics();
				g2.setColor(getBackgroundColor());
				g2.fillRect(0, 0, maxSize, maxSize);
				Date tMod = new Date();
				// Wie lange sollen neu erzeugte Kacheln gültig sein?
				// Date tExp = new Date(System.currentTimeMillis() + ACSettings.getInstance().getTileMaxExpirationTime());
				Date tExp = new Date(System.currentTimeMillis() + 14 * 24 * 3600 * 1000); // max. 14Tage

				for (int i = 0; i < layerTiles.size(); i++)
				{
					BufferedImage layerImage = layerTiles.get(i).getImage();
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getLayerAlpha(i)));
					g2.drawImage(layerImage, 0, 0, maxSize, maxSize, null);
					if (tMod.before(layerTiles.get(i).getMod()))
						tMod = layerTiles.get(i).getMod();
					if (tExp.after(layerTiles.get(i).getExp()))
						tExp = layerTiles.get(i).getExp();
					log.debug("Multi layer added: Alpha=" + getLayerAlpha(i) + "; Layer=" + i);
				}

				ByteArrayOutputStream buf = new ByteArrayOutputStream(32000);
				ImageIO.write(image, mTileType.getFileExt(), buf);
				log.trace("composed image written into buffer:(" + tAddr + ")");
				tile.loadImage(buf.toByteArray());
				tile.setMod(tMod);
				tile.setExp(tExp);
				tile.setTileState(TileState.TS_LOADED);
				// long timeExpires = timeLastModified + ACSettings.getTileDefaultExpirationTime();
				log.trace("put composed " + tile + " into tile store, exp=" + tExp);
				getNTileStore().putTile(tile);
				return tile;
			}
			else
			{
				log.warn(tAddr + " empty for" + this.getName());
				return null;
			}
		}
		// catch (InterruptedException e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		finally
		{
			if (g2 != null)
			{
				g2.dispose();
			}
		}
		// return null;
	}

	/**
	 * This loads the image via {@link IfMapSource.getTileImage}() for each layer in this multi layer map source.
	 * When all layer images are loaded, they were combined via Graphics2D.drawImage().
	 */
	@Override
	public Tile updateTile(Tile tile)
	{
		log.trace(OSMBStrs.RStr("START"));
		BufferedImage image = null;
		Graphics2D g2 = null;
		try
		{
			ArrayList<Tile> layerTiles = new ArrayList<Tile>(mapSources.length);
			int maxSize = MP2MapSpace.getTileSize();
			for (int i = 0; i < mapSources.length; i++)
			{
				ACMapSource layerMapSource = mapSources[i];
				// try to load the layer tiles from mtc
				Tile layerTile = null;
				if (mMTC != null)
				{
					layerTile = mMTC.getTile(layerMapSource, tile.getAddress());
					if (layerTile.getTileState() == TileState.TS_LOADING)
						log.debug("'loading' tile found in mtc: " + layerTile);
				}
				while (layerTile == null)
				{
					// try to load the tile from the online source
					layerTile = layerMapSource.updateTile(tile);
					if (layerTile != null)
					{
						log.debug("Multi layer loaded: '" + layerMapSource.getName() + "' (" + tile.getAddress() + ") into Layer=" + i);
						layerTiles.add(layerTile);
						if (layerTile.getTileState() == TileState.TS_LOADING)
							log.debug("'loading' tile found in ts: " + layerTile + " for " + layerMapSource.getName());
					}
					else
						log.warn("Multi layer empty: " + layerMapSource.getName() + "' (" + tile.getAddress() + ") into Layer=" + i + ", wait and retry");
					// tile.wait(20000);
				}
			}
			if (layerTiles.size() > 0)
			{
				image = new BufferedImage(MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE, BufferedImage.TYPE_3BYTE_BGR);
				g2 = image.createGraphics();
				g2.setColor(getBackgroundColor());
				g2.fillRect(0, 0, maxSize, maxSize);
				Date tMod = new Date();
				// Wie lange sollen neu erzeugte Kacheln gültig sein?
				// Date tExp = new Date(System.currentTimeMillis() + ACSettings.getInstance().getTileMaxExpirationTime());
				Date tExp = new Date(System.currentTimeMillis() + 14 * 24 * 3600 * 1000); // max. 14Tage

				for (int i = 0; i < layerTiles.size(); i++)
				{
					BufferedImage layerImage = layerTiles.get(i).getImage();
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getLayerAlpha(i)));
					g2.drawImage(layerImage, 0, 0, maxSize, maxSize, null);
					if (tMod.before(layerTiles.get(i).getMod()))
						tMod = layerTiles.get(i).getMod();
					if (tExp.after(layerTiles.get(i).getExp()))
						tExp = layerTiles.get(i).getExp();
					log.debug("Multi layer added: Alpha=" + getLayerAlpha(i) + "; Layer=" + i);
				}

				ByteArrayOutputStream buf = new ByteArrayOutputStream(32000);
				ImageIO.write(image, mTileType.getFileExt(), buf);
				log.trace("composed image written into buffer:(" + tile.getAddress() + ")");
				tile.loadImage(buf.toByteArray());
				tile.setMod(tMod);
				tile.setExp(tExp);
				tile.setTileState(TileState.TS_LOADED);
				// long timeExpires = timeLastModified + ACSettings.getTileDefaultExpirationTime();
				log.trace("put composed " + tile + " into tile store, exp=" + tExp);
				getNTileStore().putTile(tile);
				return tile;
			}
			else
			{
				log.warn(tile.getAddress() + " empty for" + this.getName());
				return null;
			}
		}
		// catch (InterruptedException e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if (g2 != null)
			{
				g2.dispose();
			}
		}
		// return null;
		return tile;
	}

	/**
	 * This loads the image via {@link IfMapSource.getTileImage}() for each layer in this multi layer map source.
	 * When all layer images are loaded, they were combined via Graphics2D.drawImage().
	 */
	@Override
	@Deprecated
	public BufferedImage getTileImage(int zoom, int x, int y) throws IOException, InterruptedException, TileException
	{
		log.warn(OSMBStrs.RStr("START"));
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

	// @Deprecated
	// public BufferedImage downloadTileImage(int zoom, int x, int y) throws IOException, TileException, InterruptedException
	// {
	// return getTileImage(new TileAddress(x, y, zoom));
	// }

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
