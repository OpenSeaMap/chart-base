package osmb.mapsources;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;

import osmb.program.jaxb.ColorAdapter;
import osmb.program.jaxb.MapSourceAdapter;
import osmb.program.tiles.IfTileProvider;
import osmb.program.tiles.Tile;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.program.tilestore.ACTileStore;
import osmb.program.tilestore.berkeleydb.SiBerkeleyDbTileStore;
import osmb.program.tilestore.sqlitedb.SQLiteDbTileStore;

@XmlJavaTypeAdapter(MapSourceAdapter.class)
public abstract class ACMapSource implements IfTileProvider
{
	// static/class data
	protected static Logger log = Logger.getLogger(ACMapSource.class);

	// instance data
	protected boolean initialized = false;
	protected int mID = 0;
	// for testing use both tile stores
	protected SiBerkeleyDbTileStore mTS = SiBerkeleyDbTileStore.getInstance();
	protected SQLiteDbTileStore mNTS = null;
	protected MapSourceLoaderInfo mLoaderInfo = null;

	@XmlElement(name = "name", nillable = false, defaultValue = "Custom")
	protected String mName = "";
	@XmlElement(name = "minZoom", defaultValue = "0")
	protected int mMinZoom = MP2MapSpace.MIN_TECH_ZOOM;
	@XmlElement(name = "maxZoom", required = true)
	protected int mMaxZoom = MP2MapSpace.MAX_TECH_ZOOM;
	@XmlElement(name = "tileType", defaultValue = "PNG")
	protected TileImageType mTileType;
	// @XmlElement(name = "tileUpdate", defaultValue = "NONE")
	// protected IfOnlineMapSource.TileUpdate tileUpdate;
	@XmlElement(name = "backgroundColor", defaultValue = "#FFFFFF")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	protected Color mBackgroundColor = Color.WHITE;

	/**
	 * Kernel constructor for all map sources. The map source is locally backed up by a tile store, which contains all tiles already loaded from the map sources
	 * source.<br>
	 * Usually the map source is created by JAXB unmarshalling a configuration file.
	 */
	protected ACMapSource()
	{
		// adjust logger to the actual implementation
		log = Logger.getLogger(this.getClass());
	}

	/**
	 * Thread safe initialization for the map source:<br>
	 * - tile store<br>
	 */
	public void initialize()
	{
		if (!initialized)
		{
			// Prevent multiple initializations in case of multi-threaded access
			try
			{
				synchronized (this)
				{
					if (!initialized)
					{
						if (mTS == null)
						{
							mTS.prepareTileStore(this);
						}
						initialized = true;
						log.trace("Map source has been initialized");
					}
				}
			}
			catch (Exception e)
			{
				log.error("Map source initialization failed: " + e.getMessage(), e);
				// TODO: inform user
			}
		}
	}

	/**
	 * A map source name has to be unique. This retrieves a 'user friendly' name.
	 * 
	 * @return Name of the map source as used in the UI.
	 */
	public String getName()
	{
		return mName;
	}

	public ACTileStore getTileStore()
	{
		return mTS;
	}

	public String getTileStoreName()
	{
		return mName;
	}

	/**
	 * Specifies the tile image type. For tiles rendered by Mapnik or Osmarenderer this is usually {@link TileImageType#PNG}.
	 * 
	 * @return Tile image file type.
	 */
	public TileImageType getTileImageType()
	{
		return mTileType;
	}

	/**
	 * The map source has an internally used ID to avoid string compares with the map source name.
	 * 
	 * @return ID of this map source as used internally, esp. by the tile store.
	 */
	public int getID()
	{
		return mID;
	}

	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo()
	{
		return mLoaderInfo;
	}

	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo)
	{
		if (this.mLoaderInfo != null)
			throw new RuntimeException("LoaderInfo already set");
		this.mLoaderInfo = loaderInfo;
	}

	public Color getBackgroundColor()
	{
		return mBackgroundColor;
	}

	/**
	 * Specifies the maximum zoom value. The number of zoom levels supported by this map source is [{@link #getMinZoom}.. {@link #getMaxZoom}].
	 * To use the zoom level it has to checked against {@link Bundle#getMaxZoomLevel}() and {@link MP2MapSpace#MAX_TECH_ZOOM}. <br>
	 * The first one is the maximum reasonable to be used in the bundles output format and device, the latter is the maximum zoom level supported by osmbs
	 * internal data structures.
	 * 
	 * @return maximum zoom value - currently (2015) <= 22 {@value MP2MapSpace#MAX_TECH_ZOOM}
	 */
	public int getMaxZoom()
	{
		return mMaxZoom;
	}

	/**
	 * Specifies the minimum zoom value. The number of zoom levels supported by this map source is [{@link #getMinZoom}.. {@link #getMaxZoom}].
	 * To use the zoom level it has to checked against Bundle.getMinZoomLevel() and IfMapSpace.MIN_TECH_ZOOM. The first one is the minimum reasonable to be used
	 * in the bundles output format and device, the latter is the minimum zoom level supported by osmbs internal data structures.
	 * 
	 * @return minimum zoom value - usually 0
	 */
	public int getMinZoom()
	{
		return mMinZoom;
	}

	/**
	 * Retrieves the data for the specified tile from the map sources source, i.e. either online or local, depending on the map source. If no data are available
	 * at the specified location, null is returned.<br>
	 * 
	 * @param zoom
	 * @param x
	 * @param y
	 * @return the tile data as a byte array
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	@Deprecated
	public byte[] getTileData(int zoom, int x, int y) throws IOException, TileException, InterruptedException
	{
		return null;
	}

	/**
	 * Retrieves the data for the specified tile from the map sources source, i.e. either online or local, depending on the map source. If no data are available
	 * at the specified location, null is returned.<br>
	 * 
	 * @param tAddr
	 *          The tile address.
	 * @return The tile data as a byte array.
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	@Override
	public byte[] loadTileData(TileAddress tAddr)
	{
		return null;
	}

	/**
	 * Retrieves the tile as an image from the map sources source, i.e. either online or local, depending on the map source.
	 * 
	 * @param zoom
	 * @param x
	 * @param y
	 * @return The tile as an image or null if no image available
	 * @throws IOException
	 * @throws TileException
	 * @throws InterruptedException
	 */
	@Deprecated
	public BufferedImage getTileImage(int zoom, int x, int y) throws IOException, TileException, InterruptedException
	{
		return null;
	}

	/**
	 * 
	 * @param tAddr
	 *          The tiles address.
	 * @return The tile data as an image or null if no image is currently available.
	 */
	@Override
	public BufferedImage loadTileImage(TileAddress tAddr)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Retrieves the tile from the map sources source, i.e. either online or local, depending on the map source.
	 * 
	 * @return The tile or null if no tile is currently available.
	 */
	@Override
	public Tile loadTile(TileAddress tAddr)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		boolean bOk = false;
		if (obj != null)
		{
			if (this == obj)
				bOk = true;
			if ((obj instanceof ACMapSource))
			{
				ACMapSource other = (ACMapSource) obj;
				return other.getName().equals(getName());
			}
		}
		return bOk;
	}
}
