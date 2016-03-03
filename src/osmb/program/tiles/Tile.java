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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.imageio.ImageIO;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.MP2MapSpace;
import osmb.mapsources.TileAddress;
import osmb.utilities.OSMBUtilities;

/**
 * Holds one map tile. Additionally the code for loading the tile image and painting it is also included in this class.
 * This contains code originally from Jan Peter Stotz.
 * 
 * @author humbach
 */
public class Tile
{
	// some static tiles
	public static final int ERROR_TILE_ID = 0; // red border and cross tile
	public static final int LOADING_TILE_ID = 1; // hourglass tile
	public static final int EXPIRED_TILE_ID = 2; // marker for expired tile
	public static final int EMPTY_TILE_ID = 10; // all transparent tile
	public static final int WHITE_TILE_ID = 11; // all white tile
	public static final int BLACK_TILE_ID = 12; // all black tile
	public static final int SEA_TILE_ID = 50; // plain sea, all (181,208,208)
	public static final int CCSALOGO_TILE_ID = 98; // ccsa logo for map corner
	public static final int OSMLOGO_TILE_ID = 99; // osm logo for map corner
	public static final int LAST_SPECIAL_TILE_ID = 99;

	public static BufferedImage LOADING_IMAGE;
	public static BufferedImage ERROR_IMAGE;

	static
	{
		try
		{
			LOADING_IMAGE = ImageIO.read(OSMBUtilities.getResourceImageUrl("hourglass.png"));
			ERROR_IMAGE = ImageIO.read(OSMBUtilities.getResourceImageUrl("error.png"));
		}
		catch (Exception e1)
		{
			LOADING_IMAGE = null;
			ERROR_IMAGE = null;
		}
	}

	/**
	 * @author humbach
	 */
	public enum TileState
	{
		TS_NEW, TS_LOADING, TS_LOADED, TS_ERROR, TS_EXPIRED
	};

	protected ACMapSource mMapSource = null;
	protected int mXTIdx = 0;
	protected int mYTIdx = 0;
	protected int mZoom = 0;
	protected TileAddress mTA = null;
	protected BufferedImage mImage = LOADING_IMAGE;
	protected TileState mTileState = TileState.TS_NEW;
	protected Date mod = null;
	protected Date exp = null;
	protected String etag = "";
	protected String mKey = "";

	/**
	 * Creates a tile with a 'loading' image and TS_NEW state.
	 * 
	 * @param mapSource
	 * @param xTIdx
	 *          tile x-index
	 * @param yTIdx
	 *          tile y-index
	 * @param zoom
	 */
	public Tile(ACMapSource mapSource, int xTIdx, int yTIdx, int zoom)
	{
		super();
		this.mMapSource = mapSource;
		this.mXTIdx = xTIdx;
		this.mYTIdx = yTIdx;
		this.mZoom = zoom;
		this.mKey = getTileKey(mapSource, xTIdx, yTIdx, zoom);
	}

	public Tile(ACMapSource source, int xTIdx, int yTIdx, int zoom, BufferedImage image)
	{
		this(source, xTIdx, yTIdx, zoom);
		this.mImage = image;
	}

	public Tile(ACMapSource mapSource, TileAddress tAddr)
	{
		this(mapSource, tAddr.getX(), tAddr.getY(), tAddr.getZoom());
	}

	/**
	 * Tries to get tiles of a lower or higher zoom level (one or two level difference) from cache and use it as a
	 * placeholder until the tile has been loaded.
	 */
	// This does not belong into here. It has to be moved into the MemoryTileCache. The Tile should not know anything about the TileStore/Cache
	public void loadPlaceholderFromCache(MemoryTileCache cache)
	{
		int tileSize = MP2MapSpace.getTileSize();
		BufferedImage tmpImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) tmpImage.getGraphics();
		// g.drawImage(image, 0, 0, null);
		for (int zoomDiff = 1; zoomDiff < 5; zoomDiff++)
		{
			// first we check if there are already the 2^x tiles
			// of a higher detail level
			int zoom_high = mZoom + zoomDiff;
			if (zoomDiff < 3 && zoom_high <= mMapSource.getMaxZoom())
			{
				int factor = 1 << zoomDiff;
				int xtile_high = mXTIdx << zoomDiff;
				int ytile_high = mYTIdx << zoomDiff;
				double scale = 1.0 / factor;
				g.setTransform(AffineTransform.getScaleInstance(scale, scale));
				int paintedTileCount = 0;
				for (int x = 0; x < factor; x++)
				{
					for (int y = 0; y < factor; y++)
					{
						Tile tile = cache.getTile(mMapSource, xtile_high + x, ytile_high + y, zoom_high);
						if (tile != null && tile.mTileState == TileState.TS_LOADED)
						{
							paintedTileCount++;
							tile.paint(g, x * tileSize, y * tileSize);
						}
					}
				}
				if (paintedTileCount == factor * factor)
				{
					mImage = tmpImage;
					return;
				}
			}

			int zoom_low = mZoom - zoomDiff;
			if (zoom_low >= mMapSource.getMinZoom())
			{
				int xtile_low = mXTIdx >> zoomDiff;
				int ytile_low = mYTIdx >> zoomDiff;
				int factor = (1 << zoomDiff);
				double scale = factor;
				AffineTransform at = new AffineTransform();
				int translate_x = (mXTIdx % factor) * tileSize;
				int translate_y = (mYTIdx % factor) * tileSize;
				at.setTransform(scale, 0, 0, scale, -translate_x, -translate_y);
				g.setTransform(at);
				Tile tile = cache.getTile(mMapSource, xtile_low, ytile_low, zoom_low);
				if (tile != null && tile.mTileState == TileState.TS_LOADED)
				{
					tile.paint(g, 0, 0);
					mImage = tmpImage;
					return;
				}
			}
		}
	}

	/**
	 * @return The map source of this tile.
	 */
	public ACMapSource getSource()
	{
		return mMapSource;
	}

	/**
	 * @return The tile number on the x axis of this tile.
	 */
	public int getXtile()
	{
		return mXTIdx;
	}

	/**
	 * @return The tile number on the y axis of this tile.
	 */
	public int getYtile()
	{
		return mYTIdx;
	}

	/**
	 * @return The zoom level of this tile.
	 */
	public int getZoom()
	{
		return mZoom;
	}

	/**
	 * @return the modification date
	 */
	public Date getMod()
	{
		return mod;
	}

	/**
	 * @param mod
	 *          the mod to set
	 */
	public void setMod(Date mod)
	{
		this.mod = mod;
	}

	/**
	 * @return the expiration date
	 */
	public Date getExp()
	{
		return exp;
	}

	/**
	 * @param exp
	 *          the exp to set
	 */
	public void setExp(Date exp)
	{
		this.exp = exp;
	}

	/**
	 * @return the etag entry
	 */
	public String getEtag()
	{
		return etag;
	}

	/**
	 * @param etag
	 *          the etag to set
	 */
	public void setEtag(String etag)
	{
		this.etag = etag;
	}

	/**
	 * @return The image for this tile.
	 */
	public BufferedImage getImage()
	{
		return mImage;
	}

	/**
	 * @return The image for this tile.
	 */
	public Byte[] getImageData()
	{
		return null;
	}

	/**
	 * Associates a specified image with this tile.
	 * 
	 * @param image
	 *          The image to be used.
	 */
	public void setImage(BufferedImage image)
	{
		this.mImage = image;
	}

	/**
	 * Uses predefined image as an error indicator.
	 */
	public void setErrorImage()
	{
		mImage = ERROR_IMAGE;
		mTileState = TileState.TS_ERROR;
	}

	/**
	 * Uses predefined image as a loading indicator.
	 */
	public void setLoadingImage()
	{
		mImage = LOADING_IMAGE;
		mTileState = TileState.TS_LOADING;
	}

	public void loadImage(InputStream input) throws IOException
	{
		mImage = ImageIO.read(input);
	}

	public void loadImage(byte[] data) throws IOException
	{
		loadImage(new ByteArrayInputStream(data));
	}

	/**
	 * @return key that identifies a tile
	 */
	public String getKey()
	{
		return mKey;
	}

	public boolean isErrorTile()
	{
		return (ERROR_IMAGE.equals(mImage));
	}

	public TileState getTileState()
	{
		return mTileState;
	}

	public void setTileState(TileState tileState)
	{
		this.mTileState = tileState;
	}

	/**
	 * Paints the tile-image on the {@link Graphics} <code>g</code> at the position <code>x</code>/<code>y</code>.
	 * 
	 * @param g
	 * @param x
	 *          x-coordinate in <code>g</code>
	 * @param y
	 *          y-coordinate in <code>g</code>
	 */
	public void paint(Graphics g, int x, int y)
	{
		if (mImage == null)
			return;

		int tileSize = MP2MapSpace.getTileSize();
		// Google Scale = 2, retina support
		g.drawImage(mImage, x, y, tileSize, tileSize, Color.WHITE, null);
		// g.drawImage(image, x, y, Color.WHITE);
	}

	public void paintTransparent(Graphics g, int x, int y)
	{
		if (mImage == null)
			return;

		int tileSize = MP2MapSpace.getTileSize();
		// Google Scale = 2, retina support
		g.drawImage(mImage, x, y, tileSize, tileSize, null);
		// g.drawImage(image, x, y, null);
	}

	// @Override
	// public String toString()
	// {
	// return "tile " + key;
	// }

	@Override
	public String toString()
	{
		return "Tile (" + mZoom + "|" + mXTIdx + "|" + mYTIdx + ") from '" + mMapSource + "'";
	}

	/**
	 * Why is the source not regarded here? AH 2015-10-01
	 * Who does use this?
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Tile))
			return false;
		Tile tile = (Tile) obj;
		return (mXTIdx == tile.mXTIdx) && (mYTIdx == tile.mYTIdx) && (mZoom == tile.mZoom);
	}

	@Override
	public int hashCode()
	{
		assert false : "hashCode not designed";
		return -1;
	}

	/**
	 * The key identifies the tile. Be aware that the map sources name is part of the key. Probably we should have something like the 'key' in the map source
	 * too...
	 * 
	 * @param source
	 * @param xtile
	 * @param ytile
	 * @param zoom
	 * @return The key as a concatenation of zoom, xtile, ytile and source *
	 */
	public static String getTileKey(ACMapSource source, int xtile, int ytile, int zoom)
	{
		return zoom + "/" + xtile + "/" + ytile + "@" + source.getName();
	}
}
