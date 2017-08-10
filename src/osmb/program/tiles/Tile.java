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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.MP2MapSpace;
import osmb.mapsources.TileAddress;
import osmb.program.ACSettings;
import osmb.utilities.OSMBStrs;
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
	 * This describes the state the tile is in. It can be <br>
	 * TS_NEW - Meaning a newly created tile.<br>
	 * TS_LOADING - There is a load scheduled, but it has not yet finished.<br>
	 * TS_LOADED - The tile has been successfully loaded from the specified source.<br>
	 * TS_ERROR - There has bee some error and no image is available. The image shown is a standard error image.<br>
	 * TS_EXPIRED - The image shown is expired and should be replaced by a new version from the source.
	 * TS_ZOOMED - The image is composed from another zoom level and not actually loaded.
	 * 
	 * @author humbach
	 */
	public enum TileState
	{
		TS_NEW, TS_LOADING, TS_LOADED, TS_ERROR, TS_EXPIRED, TS_ZOOMED
	};

	// static/class data
	protected static Logger log = Logger.getLogger(ACMapSource.class);

	/**
	 * The key identifies the tile. Be aware that the map sources name is part of the key. Probably we should have something like the 'key' in the map source
	 * too...<br>
	 * It is primarily used by the mtc to identify tiles.
	 * 
	 * @param source
	 * @param tAddr
	 *          The tile address: zoom, x-idx, y-idx.
	 * @return The key as a concatenation of zoom, x, y and map source name.
	 */
	public static String getTileKey(ACMapSource source, TileAddress tAddr)
	{
		return tAddr.getZoom() + "/" + tAddr.getX() + "/" + tAddr.getY() + "@" + source.getName();
	}

	// instance data
	protected ACMapSource mMapSource = null;
	// protected int mXTIdx = 0;
	// protected int mYTIdx = 0;
	// protected int mZoom = 0;
	protected TileAddress mTA = null;
	protected BufferedImage mImage = LOADING_IMAGE;
	protected TileState mTileState = TileState.TS_NEW;
	protected Date mMod = new Date();
	protected Date mExp = new Date();
	protected String mETag = "-";
	protected String mKey = "";

	/**
	 * Creates a tile with a 'loading' image and TS_NEW state.
	 */
	public Tile(ACMapSource mapSource, int xTIdx, int yTIdx, int zoom)
	{
		this(mapSource, new TileAddress(xTIdx, yTIdx, zoom));
	}

	/**
	 * Creates a tile with specified image and TS_NEW state.<br>
	 * The {@link TileState} should be set to a reasonable value after this way of construction.
	 */
	public Tile(ACMapSource mapSource, int xTIdx, int yTIdx, int zoom, BufferedImage image)
	{
		this(mapSource, new TileAddress(xTIdx, yTIdx, zoom));
		this.mImage = image;
	}

	/**
	 * Creates a tile with a 'loading' image and TS_NEW state.
	 */
	public Tile(ACMapSource mapSource, TileAddress tAddr)
	{
		log = Logger.getLogger(this.getClass());
		this.mMapSource = mapSource;
		this.mTA = tAddr;
		this.mKey = getTileKey(mapSource, tAddr);
	}

	/**
	 * @return The map source of this tile.
	 */
	public ACMapSource getSource()
	{
		return mMapSource;
	}

	/**
	 * @return The tile index on the x axis of this tile.
	 */
	public int getXtile()
	{
		return mTA.getX();
	}

	/**
	 * @return The tile index on the y axis of this tile.
	 */
	public int getYtile()
	{
		return mTA.getY();
	}

	/**
	 * @return The zoom level of this tile.
	 */
	public int getZoom()
	{
		return mTA.getZoom();
	}

	/**
	 * @return The tiles address.
	 */
	public TileAddress getAddress()
	{
		return mTA;
	}

	/**
	 * @return the modification date
	 */
	public Date getMod()
	{
		if (mMod == null)
			mMod = new Date();
		return mMod;
	}

	/**
	 * @param mod
	 *          the mod to set
	 */
	public void setMod(Date mod)
	{
		this.mMod = mod;
	}

	/**
	 * @return the expiration date
	 */
	public Date getExp()
	{
		if (mExp == null)
			mExp = new Date();
		return mExp;
	}

	/**
	 * @param exp
	 *          the exp to set
	 */
	public void setExp(Date exp)
	{
		this.mExp = exp;
	}

	public boolean isExpired()
	{
		log.trace(OSMBStrs.RStr("START"));
		boolean bExp = false;
		ACSettings settings = ACSettings.getInstance();
		long maxExpirationTime = settings.getTileMaxExpirationTime();
		long minExpirationTime = settings.getTileMinExpirationTime();
		// long maxExpirationTime = 18000000;
		// long minExpirationTime = 3000000;
		long modMillis = getMod().getTime();
		long now = System.currentTimeMillis();
		// if (modDate + maxExpirationTime) has expired. then the tile has expired, regardless of the servers expiration date.
		if ((modMillis + maxExpirationTime) < now)
		{
			bExp = true;
			log.info(this + " has expired due to max=" + (maxExpirationTime / 3600000) + "h, mod=" + (modMillis / 1000));
		}
		// only if (modDate + minExpirationTime) has expired. then ...
		else if ((modMillis + minExpirationTime) < now)
		{
			Date expiryDate = getExp();
			if (expiryDate != null)
			{
				// server had set an expiration time, use that.
				bExp = (expiryDate.getTime() < now);
			}
			else
			{
				// server had not set an expiration time, the tile has expired.
				bExp = true;
				log.warn(this + " has no expiration time set");
			}
		}
		return bExp;
	}

	/**
	 * @return the etag entry
	 */
	public String getETag()
	{
		return mETag;
	}

	/**
	 * @param etag
	 *          the etag to set
	 */
	public void setETag(String etag)
	{
		this.mETag = etag;
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
	 * @throws IOException
	 */
	public byte[] getImageData() throws IOException
	{
		ByteArrayOutputStream buf = null;
		buf = new ByteArrayOutputStream(32000);
		ImageIO.write(mImage, mMapSource.getTileImageType().getFileExt(), buf);
		return buf.toByteArray();
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

	public TileState getTileState()
	{
		return mTileState;
	}

	public void setTileState(TileState tileState)
	{
		mTileState = tileState;
	}

	/**
	 * Paints the tile-image on the {@link Graphics} context <code>gC</code> at the position <code>xTgt</code>/<code>yTgt</code>.<br>
	 * It simply and silently fails to do anything if the image is null.
	 * 
	 * @param gC
	 *          The graphics context to paint into.
	 * @param xTgt
	 *          x-coordinate in <code>gC</code>
	 * @param yTgt
	 *          y-coordinate in <code>gC</code>
	 */
	public void paint(Graphics gC, int xTgt, int yTgt)
	{
		if (mImage != null)
			// gC.drawImage(mImage, xTgt, yTgt, MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE, Color.WHITE, null);
			gC.drawImage(mImage, xTgt, yTgt, MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE, mMapSource.getBackgroundColor(), null);
	}

	/**
	 * Does the same as {@link #paint(Graphics, int, int)}, but ignores the background color specified by the map source.
	 * 
	 * @param gC
	 * @param xTgt
	 * @param yTgt
	 */
	public void paintTransparent(Graphics gC, int xTgt, int yTgt)
	{
		if (mImage != null)
			gC.drawImage(mImage, xTgt, yTgt, MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE, null);
	}

	@Override
	public String toString()
	{
		return "Tile " + mTA + ":" + this.mTileState + " from '" + mMapSource + "'";
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
		return (mTA == tile.getAddress());
	}

	@Override
	public int hashCode()
	{
		assert false : "hashCode not designed";
		return -1;
	}

}
