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

import org.apache.log4j.Logger;

import osmb.mapsources.IfHttpMapSource;
import osmb.mapsources.IfMapSourceListener;
import osmb.program.ACSettings;
import osmb.program.map.IfMapSpace;
import osmb.program.tilestore.ACSiTileStore;
import osmb.program.tilestore.IfTileStoreEntry;
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
	private static ACSettings settings = ACSettings.getInstance();

	public static String ACCEPT = "text/html, image/png, image/jpeg, image/gif, */*;q=0.1";

	static
	{
		Object defaultReadTimeout = System.getProperty("sun.net.client.defaultReadTimeout");
		if (defaultReadTimeout == null)
			System.setProperty("sun.net.client.defaultReadTimeout", "15000");
		System.setProperty("http.maxConnections", "20");
	}

	/**
	 * Tries to get the data from the tile store.
	 * If this is not successful, it loads the tile from the online map source by {@link #downloadTileAndUpdateStore}().
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
	public static byte[] getTileData(int x, int y, int zoom, IfHttpMapSource mapSource) throws IOException, InterruptedException, UnrecoverableDownloadException
	{
		IfMapSpace mapSpace = mapSource.getMapSpace();
		int maxTileIndex = mapSpace.getMaxPixels(zoom) / mapSpace.getTileSize();
		if (x > maxTileIndex)
			throw new RuntimeException("Invalid tile index x=" + x + " for zoom " + zoom + ", MAX=" + maxTileIndex);
		if (y > maxTileIndex)
			throw new RuntimeException("Invalid tile index y=" + y + " for zoom " + zoom + ", MAX=" + maxTileIndex);

		ACSiTileStore ts = ACSiTileStore.getInstance();

		// Thread.sleep(2000);

		ACSettings s = ACSettings.getInstance();

		IfTileStoreEntry tile = null;
		if (s.getTileStoreEnabled())
		{
			// Copy the file from the persistent tile store instead of downloading it from internet.
			tile = ts.getTile(x, y, zoom, mapSource);
			boolean expired = ts.isTileExpired(tile);
			if (tile != null)
			{
				if (expired)
				{
					log.warn("Expired: " + mapSource.getName() + " " + tile);
				}
				else
				{
					log.trace("Tile of map source " + mapSource.getName() + " used from tilestore");
					byte[] data = tile.getData();
					notifyCachedTileUsed(data.length);
					return data;
				}
			}
		}
		byte[] data = null;
		if (tile == null)
		{
			data = downloadTileAndUpdateStore(x, y, zoom, mapSource);
			notifyTileDownloaded(data.length);
		}
		else
		{
			byte[] updatedData = updateStoredTile(tile, mapSource);
			if (updatedData != null)
			{
				data = updatedData;
				notifyTileDownloaded(data.length);
			}
			else
			{
				data = tile.getData();
				notifyCachedTileUsed(data.length);
			}
		}
		return data;
	}

	private static void notifyTileDownloaded(int size)
	{
		if (Thread.currentThread() instanceof IfMapSourceListener)
		{
			((IfMapSourceListener) Thread.currentThread()).tileDownloaded(size);
		}
	}

	private static void notifyCachedTileUsed(int size)
	{
		if (Thread.currentThread() instanceof IfMapSourceListener)
		{
			((IfMapSourceListener) Thread.currentThread()).tileLoadedFromCache(size);
		}
	}

	/**
	 * Download the tile from the web server and updates the tile store if the tile could be successfully retrieved.
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @param mapSource
	 * @return
	 * @throws UnrecoverableDownloadException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static byte[] downloadTileAndUpdateStore(int x, int y, int zoom, IfHttpMapSource mapSource)
	    throws UnrecoverableDownloadException, IOException, InterruptedException
	{
		return downloadTileAndUpdateStore(x, y, zoom, mapSource, ACSettings.getInstance().getTileStoreEnabled());
	}

	// public static byte[] downloadTile(int x, int y, int zoom, IfHttpMapSource mapSource) throws UnrecoverableDownloadException, IOException,
	// InterruptedException
	// {
	// return downloadTileAndUpdateStore(x, y, zoom, mapSource, false);
	// }

	public static byte[] downloadTileAndUpdateStore(int x, int y, int zoom, IfHttpMapSource mapSource, boolean useTileStore)
	    throws UnrecoverableDownloadException, IOException, InterruptedException
	{
		if (zoom < 0)
			throw new UnrecoverableDownloadException("Negative zoom!");
		HttpURLConnection conn = mapSource.getTileUrlConnection(zoom, x, y);
		if (conn == null)
			throw new UnrecoverableDownloadException("Tile x=" + x + " y=" + y + " zoom=" + zoom + " is not a valid tile in map source " + mapSource);

		log.trace("Downloading " + conn.getURL());

		prepareConnection(conn);
		conn.connect();

		int code = conn.getResponseCode();
		byte[] data = loadBodyDataInBuffer(conn);

		if (code != HttpURLConnection.HTTP_OK)
			throw new DownloadFailedException(conn, code);

		checkContentType(conn, data);
		checkContentLength(conn, data);

		String eTag = conn.getHeaderField("ETag");
		long timeLastModified = conn.getLastModified();
		long timeExpires = conn.getExpiration();

		OSMBUtilities.checkForInterruption();
		TileImageType imageType = OSMBUtilities.getImageType(data);
		if (imageType == null)
			throw new UnrecoverableDownloadException("The returned image is of unknown format");
		if (useTileStore)
		{
			ACSiTileStore.getInstance().putTileData(data, x, y, zoom, mapSource, timeLastModified, timeExpires, eTag);
		}
		OSMBUtilities.checkForInterruption();
		return data;
	}

	/**
	 * This checks if the data online are modified against the data in the tile store. If so the new data are down loaded and stored in the tile store - if the
	 * tile store is enabled in settings.
	 * 
	 * @param tile
	 * @param mapSource
	 * @return The tile data as a byte array. Only if the data have been modified according to the update setting. If the data are not modified, it returns null.
	 * @throws UnrecoverableDownloadException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static byte[] updateStoredTile(IfTileStoreEntry tile, IfHttpMapSource mapSource)
	    throws UnrecoverableDownloadException, IOException, InterruptedException
	{
		final int x = tile.getX();
		final int y = tile.getY();
		final int zoom = tile.getZoom();
		final IfHttpMapSource.TileUpdate tileUpdate = mapSource.getTileUpdate();

		switch (tileUpdate)
		{
			case ETag:
			{
				boolean different = isETagDifferent(tile, mapSource);
				if (!different)
				{
					if (log.isTraceEnabled())
						log.trace("Data unchanged on server (eTag): " + mapSource + " " + tile);
					return null;
				}
				break;
			}
			case LastModified:
			{
				boolean isNewer = isTileNewer(tile, mapSource);
				if (!isNewer)
				{
					if (log.isTraceEnabled())
						log.trace("Data unchanged on server (LastModified): " + mapSource + " " + tile);
					return null;
				}
				break;
			}
			case None:
			{
				break;
			}
			case IfModifiedSince:
			case IfNoneMatch:
			default:
				break;
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(zoom, x, y);
		if (conn == null)
			throw new UnrecoverableDownloadException("Tile x=" + x + " y=" + y + " zoom=" + zoom + " is not a valid tile in map source " + mapSource);

		if (log.isTraceEnabled())
			log.trace(String.format("Checking %s %s", mapSource.getName(), tile));

		prepareConnection(conn);

		boolean conditionalRequest = false;

		// prepare to ask the server if the data have changed
		switch (tileUpdate)
		{
			case IfNoneMatch:
			{
				if (tile.getETag() != null)
				{
					conn.setRequestProperty("If-None-Match", tile.getETag());
					conditionalRequest = true;
				}
				break;
			}
			case IfModifiedSince:
			{
				if (tile.getTimeLastModified() > 0)
				{
					conn.setIfModifiedSince(tile.getTimeLastModified());
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

		if (conditionalRequest && code == HttpURLConnection.HTTP_NOT_MODIFIED)
		{
			// server responded: data not modified
			if (s.getTileStoreEnabled())
			{
				// update expiration date in tile store
				tile.update(conn.getExpiration());
				ACSiTileStore.getInstance().putTile(tile, mapSource);
			}
			if (log.isTraceEnabled())
				log.trace("Server responded: Data not modified: " + mapSource + " " + tile);
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

			OSMBUtilities.checkForInterruption();
			TileImageType imageType = OSMBUtilities.getImageType(data);
			if (imageType == null)
				throw new UnrecoverableDownloadException("The returned image is of unknown format");
			if (s.getTileStoreEnabled())
			{
				ACSiTileStore.getInstance().putTileData(data, x, y, zoom, mapSource, timeLastModified, timeExpires, eTag);
			}
			OSMBUtilities.checkForInterruption();
			return data;
		}
	}

	/**
	 * @deprecated This is moved into the {@link ACSiTileStore}
	 */
	@Deprecated
	public static boolean isTileExpired(IfTileStoreEntry tileStoreEntry)
	{
		if (tileStoreEntry == null)
			return true;
		return ACSiTileStore.getInstance().isTileExpired(tileStoreEntry);
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
		InputStream input = null;
		byte[] data = null;
		try
		{
			input = conn.getInputStream();
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
			OSMBUtilities.closeStream(input);
		}
		log.trace("Retrieved " + data.length + " bytes for a HTTP " + conn.getResponseCode());
		if (data.length == 0)
			return null;
		return data;
	}

	/**
	 * Returns true if the tile online is newer than the one referenced by the tile store entry.
	 * Performs a <code>HEAD</code> request for retrieving the <code>LastModified</code> header value.
	 */
	protected static boolean isTileNewer(IfTileStoreEntry tile, IfHttpMapSource mapSource) throws IOException
	{
		long oldLastModified = tile.getTimeLastModified();
		if (oldLastModified <= 0)
		{
			log.warn("Tile age comparison not possible: " + "tile in tilestore does not contain lastModified attribute");
			return true;
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(tile.getZoom(), tile.getX(), tile.getY());
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
	protected static boolean isETagDifferent(IfTileStoreEntry tile, IfHttpMapSource mapSource) throws IOException
	{
		String eTag = tile.getETag();
		if (eTag == null || eTag.length() == 0)
		{
			log.warn("ETag check not possible: " + "tile in tilestore does not contain ETag attribute");
			return true;
		}
		HttpURLConnection conn = mapSource.getTileUrlConnection(tile.getZoom(), tile.getX(), tile.getY());
		conn.setRequestMethod("HEAD");
		conn.setRequestProperty("Accept", ACCEPT);
		String onlineETag = conn.getHeaderField("ETag");
		if (onlineETag == null || onlineETag.length() == 0)
			return true;
		return (!onlineETag.equals(eTag));
	}

	protected static void prepareConnection(HttpURLConnection conn) throws ProtocolException
	{
		conn.setRequestMethod("GET");

		ACSettings s = ACSettings.getInstance();
		conn.setConnectTimeout(1000 * s.getHttpConnectionTimeout());
		conn.setReadTimeout(1000 * s.getHttpReadTimeout());
		if (conn.getRequestProperty("User-agent") == null)
			conn.setRequestProperty("User-agent", s.getUserAgent());
		conn.setRequestProperty("Accept", ACCEPT);
	}

	protected static void checkContentType(HttpURLConnection conn, byte[] data) throws UnrecoverableDownloadException
	{
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
		int len = conn.getContentLength();
		if (len < 0)
			return;
		if (data.length != len)
			throw new UnrecoverableDownloadException(
			    "Content length is not as declared by the server: retrieved=" + data.length + " bytes, expected-content-length=" + len + " bytes");
	}
}
