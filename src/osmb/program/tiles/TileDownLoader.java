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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.util.Date;

import org.apache.log4j.Logger;

import osmb.mapsources.ACOnlineMapSource;
import osmb.mapsources.IfMapSourceListener;
import osmb.mapsources.IfOnlineMapSource;
import osmb.mapsources.MP2MapSpace;
import osmb.mapsources.TileAddress;
import osmb.program.ACSettings;
import osmb.program.tiles.Tile.TileState;
//W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tilestore.ACTileStore;
import osmb.program.tilestore.IfStoredTile;
import osmb.utilities.OSMBStrs;
import osmb.utilities.OSMBUtilities;

/**
 * The TileDonwLoader is the low-level class, which is actually downloading the image data. This is the one which is establishing the online connection and
 * using it to download the tile data.
 * It does perform the check if the tile online is actually different from the one
 * 
 * - getTileData()
 * - downloadTileAndUpdateStore()
 * - updateStoredTile()
 * 
 * 
 * It only consists of static methods.
 * There is no constructor available.
 */
public class TileDownLoader
{
	private static Logger log = Logger.getLogger(TileDownLoader.class);
	// private static ACSettings settings = ACSettings.getInstance();

	public static String ACCEPT = "text/html, image/png, image/jpeg, image/gif, */*;q=0.1";

	static
	{
		// Object defaultReadTimeout = System.getProperty("sun.net.client.defaultReadTimeout");
		// if (defaultReadTimeout == null)
		System.setProperty("sun.net.client.defaultReadTimeout", "30000");
		System.setProperty("http.maxConnections", "20");
	}

	/**
	 * This loads the tile from the online map source by {@link #downloadTileAndUpdateStore}().
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws UnrecoverableDownloadException
	 */
	public static byte[] loadTileData(TileAddress tAddr, ACOnlineMapSource mapSource) throws IOException, InterruptedException, UnrecoverableDownloadException
	{
		log.trace(OSMBStrs.RStr("START"));
		int maxTileIndex = MP2MapSpace.getSizeInPixel(tAddr.getZoom()) / MP2MapSpace.TECH_TILESIZE;
		if (tAddr.getX() > maxTileIndex)
			throw new RuntimeException("Invalid tile index x=" + tAddr.getX() + " for zoom " + tAddr.getZoom() + ", MAX=" + maxTileIndex);
		if (tAddr.getY() > maxTileIndex)
			throw new RuntimeException("Invalid tile index y=" + tAddr.getY() + " for zoom " + tAddr.getZoom() + ", MAX=" + maxTileIndex);

		byte[] data = null;
		{
			data = downloadTileAndUpdateStore(tAddr, mapSource);
			notifyTileDownloaded(data.length);
		}
		return data;
	}

	private static void notifyTileDownloaded(int size)
	{
		// if (Thread.currentThread() instanceof IfMapSourceListener)
		if (IfMapSourceListener.class.isAssignableFrom(Thread.currentThread().getClass()))
		{
			((IfMapSourceListener) Thread.currentThread()).tileDownloaded(size);
		}
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @param useTileStore
	 * @return The tile image as byte[] or null if no image available.
	 * @throws UnrecoverableDownloadException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static byte[] downloadTileAndUpdateStore(TileAddress tAddr, ACOnlineMapSource mapSource)
	{
		log.trace(OSMBStrs.RStr("START"));
		byte[] data = null;
		try
		{
			if (tAddr.getZoom() < 0)
				throw new UnrecoverableDownloadException("Negative zoom!");
			HttpURLConnection conn = mapSource.getTileUrlConnection(tAddr);
			if (conn == null)
				throw new UnrecoverableDownloadException(
				    "Tile x=" + tAddr.getX() + " y=" + tAddr.getY() + " zoom=" + tAddr.getZoom() + " is not a valid tile in map source " + mapSource);

			log.trace("Downloading " + conn.getURL());

			prepareConnection(conn);
			conn.connect();

			int code = conn.getResponseCode();
			data = loadBodyDataInBuffer(conn);

			if (code != HttpURLConnection.HTTP_OK)
			{
				log.error("loadBodyDataInBuffer() failed:" + code);
				throw new DownloadFailedException(conn, code);
			}

			checkContentType(conn, data);
			checkContentLength(conn, data);
			notifyTileDownloaded(data.length);

			String eTag = conn.getHeaderField("ETag");
			long timeLastModified = conn.getLastModified();
			long timeExpires = conn.getExpiration();

			OSMBUtilities.checkForInterruption();
			TileImageType imageType = OSMBUtilities.getImageType(data);
			if (imageType == null)
				throw new UnrecoverableDownloadException("The returned image is of unknown format");
			// ACTileStore.getInstance().putTileData(data, x, y, zoom, mapSource, timeLastModified, timeExpires, eTag);
			mapSource.getTileStore().putTileData(data, tAddr.getX(), tAddr.getY(), tAddr.getZoom(), mapSource, timeLastModified, timeExpires, eTag);
			OSMBUtilities.checkForInterruption();
		}
		catch (Exception e)
		{

		}
		return data;
	}

	/**
	 * @param tAddr
	 * @param mapSource
	 * @return The downloaded tile or null if problems have occurred.
	 */
	public static Tile downloadTile(TileAddress tAddr, ACOnlineMapSource mapSource)
	{
		log.trace(OSMBStrs.RStr("START"));
		Tile tile = null;
		try
		{
			tile = new Tile(mapSource, tAddr);
			byte[] data = null;

			HttpURLConnection conn = mapSource.getTileUrlConnection(tAddr);
			if (conn == null)
				throw new UnrecoverableDownloadException(tile + " is not a valid tile in map source '" + mapSource + "'");

			log.debug("Downloading " + conn.getURL() + " by " + Thread.currentThread().getClass());

			prepareConnection(conn);
			conn.connect();

			int code = conn.getResponseCode();
			data = loadBodyDataInBuffer(conn);

			if (code != HttpURLConnection.HTTP_OK)
			{
				log.error("loadBodyDataInBuffer() failed:" + code);
				throw new UnrecoverableDownloadException(conn.toString() + code);
			}

			checkContentType(conn, data);
			checkContentLength(conn, data);

			if (OSMBUtilities.getImageType(data) != null)
			{
				tile.loadImage(data);
				tile.setTileState(TileState.TS_LOADED);
				tile.setETag(conn.getHeaderField("ETag"));
				tile.setMod(new Date(Math.max(conn.getLastModified(), System.currentTimeMillis())));
				tile.setExp(new Date(Math.max(conn.getExpiration(), System.currentTimeMillis() + ACSettings.getInstance().getTileMaxExpirationTime())));
				notifyTileDownloaded(data.length);
			}
			else
				throw new UnrecoverableDownloadException("The returned image is of unknown format");
		}
		catch (SocketTimeoutException e)
		{
			log.error("Downloading of " + tAddr + " from " + mapSource.getName() + " failed due to timeout -> retry");
			tile = null;
		}
		catch (Exception e)
		{
			log.error("Downloading of " + tAddr + " failed", e);
			tile = null;
		}
		return tile;
	}

	/**
	 * This checks if the data online are modified against the data in the tile store. If so the new data are down loaded and stored in the tile store - if the
	 * tile store is enabled in settings.
	 * 
	 * @param tTile
	 * @param mapSource
	 * @return The tile data as a byte array. Only if the data have been modified according to the update setting. If the data are not modified, it returns null.
	 * @throws UnrecoverableDownloadException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Tile updateTile(Tile tTile, ACOnlineMapSource mapSource) throws UnrecoverableDownloadException, IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		final int x = tTile.getXtile();
		final int y = tTile.getYtile();
		final int zoom = tTile.getZoom();
		final IfOnlineMapSource.TileUpdate tileUpdate = mapSource.getTileUpdate();

		boolean different = isETagDifferent(tTile, mapSource);
		if (!different)
		{
			if (log.isTraceEnabled())
				log.debug("Data unchanged on server (eTag): " + mapSource + " " + tTile);
			return tTile;
		}
		boolean isNewer = isTileNewer(tTile, mapSource);
		if (!isNewer)
		{
			if (log.isTraceEnabled())
				log.trace("Data unchanged on server (LastModified): " + mapSource + " " + tTile);
			return tTile;
		}

		HttpURLConnection conn = mapSource.getTileUrlConnection(tTile.getAddress());
		if (conn == null)
			throw new UnrecoverableDownloadException("Tile x=" + x + " y=" + y + " zoom=" + zoom + " is not a valid tile in map source " + mapSource);

		if (log.isTraceEnabled())
			log.trace(String.format("Checking %s %s", mapSource.getName(), tTile));

		prepareConnection(conn);

		boolean conditionalRequest = false;

		// prepare to ask the server if the data have changed
		switch (tileUpdate)
		{
			case IfNoneMatch:
			{
				if (tTile.getETag() != null)
				{
					conn.setRequestProperty("If-None-Match", tTile.getETag());
					conditionalRequest = true;
				}
				break;
			}
			case IfModifiedSince:
			{
				if (tTile.getMod().getTime() > 0)
				{
					conn.setIfModifiedSince(tTile.getMod().getTime());
					conditionalRequest = true;
				}
				break;
			}
			case ETag:
			case LastModified:
			case None:
			default:
				break;
		}

		conn.connect();

		ACSettings s = ACSettings.getInstance();

		int code = conn.getResponseCode();

		if (conditionalRequest && (code == HttpURLConnection.HTTP_NOT_MODIFIED))
		{
			// server responded: data not modified
			if (s.getTileStoreEnabled())
			{
				// update expiration date in tile store
				tTile.setExp(new Date(conn.getExpiration()));
				mapSource.getTileStore().putTile(tTile, mapSource);
			}
			if (log.isTraceEnabled())
				log.trace("Server responded: Data not modified: " + mapSource + " " + tTile);
			return null;
		}
		else
		{
			// only now load the data
			byte[] data = loadBodyDataInBuffer(conn);

			if (code != HttpURLConnection.HTTP_OK)
				throw new DownloadFailedException(conn, code);

			checkContentType(conn, data);
			checkContentLength(conn, data);

			String eTag = conn.getHeaderField("ETag");
			long timeLastModified = conn.getLastModified();
			long timeExpires = conn.getExpiration();

			TileImageType imageType = OSMBUtilities.getImageType(data);
			if (imageType == null)
				throw new UnrecoverableDownloadException("The returned image is of unknown format");
			if (s.getTileStoreEnabled())
			{
				mapSource.getTileStore().putTileData(data, x, y, zoom, mapSource, timeLastModified, timeExpires, eTag);
			}
			return tTile;
		}
	}

	/**
	 * @deprecated This is moved into the {@link ACTileStore}
	 */
	@Deprecated
	public static boolean isTileExpired(IfStoredTile tileStoreEntry)
	{
		if (tileStoreEntry == null)
			return true;
		return ACTileStore.getInstance().isTileExpired(tileStoreEntry);
	}

	/**
	 * Reads all available data from the input stream of <code>conn</code> and returns it as byte array. If no input data is available the method returns
	 * <code>null</code>.
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	protected static byte[] loadBodyDataInBuffer(HttpURLConnection conn) throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		InputStream input = conn.getInputStream();
		byte[] data = null;
		try
		{
			if (Thread.currentThread() instanceof IfMapSourceListener)
			{
				// // We only throttle bundle downloads, not downloads for the preview map
				// long bandwidthLimit = ACSettings.getInstance().getBandwidthLimit();
				// if (bandwidthLimit > 0)
				// {
				// input = new ThrottledInputStream(input);
				// }
			}
			data = OSMBUtilities.getInputBytes(input);
		}
		catch (IOException e)
		{
			InputStream errorIn = conn.getErrorStream();
			try
			{
				byte[] errData = OSMBUtilities.getInputBytes(errorIn);
				log.trace("Retrieved " + errData.length + " error bytes for a HTTP " + conn.getResponseCode());
			}
			catch (Exception ee)
			{
				log.error("Error retrieving error stream content: " + e);
			}
			finally
			{
				OSMBUtilities.closeStream(errorIn);
			}
			throw e;
		}
		finally
		{
			if (input != null)
				OSMBUtilities.closeStream(input);
		}
		if ((data == null) || (data.length == 0))
			return null;
		log.trace("Retrieved " + data.length + " bytes for a HTTP " + conn.getResponseCode());
		return data;
	}

	/**
	 * Returns true if the tile online is newer than the one referenced by the tile store entry.
	 * Performs a <code>HEAD</code> request for retrieving the <code>LastModified</code> header value.
	 */
	protected static boolean isTileNewer(Tile tTile, IfOnlineMapSource mapSource) throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		long oldLastModified = tTile.getMod().getTime();
		if (oldLastModified <= 0)
		{
			log.warn("Tile age comparison not possible: " + "tile in tilestore does not contain lastModified attribute");
			return true;
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(tTile.getAddress());
		conn.setRequestMethod("HEAD");
		conn.setRequestProperty("Accept", ACCEPT);
		long newLastModified = conn.getLastModified();
		if (newLastModified == 0)
			return true;
		return (newLastModified > oldLastModified);
	}

	/**
	 * Returns true if the tile online has an eTag different from the one referenced by the tile store entry.
	 * Performs a <code>HEAD</code> request for retrieving the <code>ETag</code> header value.
	 */
	protected static boolean isETagDifferent(Tile tTile, IfOnlineMapSource mapSource) throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		String eTag = tTile.getETag();
		if (eTag == null || eTag.length() == 0)
		{
			log.warn("ETag check not possible: " + "tile in tilestore does not contain ETag attribute");
			return true;
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(tTile.getAddress());
		conn.setRequestMethod("HEAD");
		conn.setRequestProperty("Accept", ACCEPT);
		String onlineETag = conn.getHeaderField("ETag");
		if (onlineETag == null || onlineETag.length() == 0)
			return true;
		return (!onlineETag.equals(eTag));
	}

	protected static void prepareConnection(HttpURLConnection conn) throws ProtocolException
	{
		log.trace(OSMBStrs.RStr("START"));
		conn.setRequestMethod("GET");

		ACSettings s = ACSettings.getInstance();
		conn.setConnectTimeout(1000 * s.getHttpConnectionTimeout());
		conn.setReadTimeout(1000 * s.getHttpReadTimeout());
		// conn.setConnectTimeout(1000 * 15);
		// conn.setReadTimeout(1000 * 20);
		if (conn.getReadTimeout() < 10000)
			log.warn("timeout=" + conn.getReadTimeout());
		if (conn.getRequestProperty("User-agent") == null)
			conn.setRequestProperty("User-agent", s.getUserAgent());
		conn.setRequestProperty("Accept", ACCEPT);
	}

	protected static void checkContentType(HttpURLConnection conn, byte[] data) throws UnrecoverableDownloadException
	{
		log.trace(OSMBStrs.RStr("START"));
		String contentType = conn.getContentType();
		if (contentType != null)
		{
			contentType = contentType.toLowerCase();
			if (!contentType.startsWith("image/"))
			{
				if (log.isTraceEnabled() && contentType.startsWith("text/"))
				{
					log.trace("Content (" + contentType + "): " + new String(data));
				}
				throw new UnrecoverableDownloadException("Content type of the loaded image is unknown: " + contentType,
				    UnrecoverableDownloadException.ERROR_CODE_CONTENT_TYPE);
			}
		}
	}

	/**
	 * Check if the retrieved data length is equal to the header value Content-Length
	 * 
	 * @param conn
	 * @param data
	 * @throws UnrecoverableDownloadException
	 */
	protected static void checkContentLength(HttpURLConnection conn, byte[] data) throws UnrecoverableDownloadException
	{
		log.trace(OSMBStrs.RStr("START"));
		int len = conn.getContentLength();
		if (len < 0)
			return;
		if (data.length != len)
			throw new UnrecoverableDownloadException(
			    "Content length is not as declared by the server: retrieved=" + data.length + " bytes, expected-content-length=" + len + " bytes");
	}
}
