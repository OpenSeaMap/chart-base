package osmb.program;

import java.awt.Dimension;
import java.io.File;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;

import osmb.program.jaxb.DimensionAdapter;
import osmb.program.tiles.TileImageFormat;
import osmb.utilities.UnitSystem;
import osmb.utilities.geo.CoordinateStringFormat;

/**
 * Abstract base for all OpenSeaMap Chart Apps settings. It provides reasonable defaults for common elements in settings.
 * 
 * @author humbach
 *
 */
public abstract class ACSettings implements IfSettings
{
	// class data / statics
	private static final File FILE = new File(ACApp.getProgramDir(), "settings.xml");
	protected static ACSettings instance = null;
	protected static Logger log = initLogger();

	protected static Logger initLogger()
	{
		return log = Logger.getLogger(ACSettings.class);
	}

	// W #??? getter/setter <-> @XmlElement(name = "directories") protected Directories cfgDirectories = new Directories();
	public static class Directories
	{
		// standard directories applied in DirectoryManager.static
		// user changes in SettingsGUI.addDirectoriesPanel()
		@XmlElement
		protected String bundleOutputDirectory = null;
		@XmlElement
		protected String tileStoreDirectory = null;
		@XmlElement
		protected String catalogsDirectory = null;
		// @XmlElement
		// protected String mapSourcesDirectory = null; // wird immer noch gebraucht, oder wo kommt der Wert her?
	}

	protected static long SETTINGS_LAST_MODIFIED = 0;
	/**
	 * Expiration time (in milliseconds) of a tile if the server does not provide an expiration time
	 */
	private static long cfgTileDefaultExpirationTime = TimeUnit.DAYS.toMillis(28);

	public static ACSettings getInstance()
	{
		return instance;
	}

	public static File getFile()
	{
		return FILE;
	}

	public static ACSettings load() throws JAXBException
	{
		return instance;
	}

	public static void save() throws JAXBException
	{
	}

	public static boolean checkSettingsFileModified()
	{
		if (SETTINGS_LAST_MODIFIED == 0)
			return false;
		// Check if the settings.xml has been modified
		// since it has been loaded
		long lastModified = getFile().lastModified();
		return (SETTINGS_LAST_MODIFIED != lastModified);
	}

	// W #??? xml-Element <-> static
	public static long getTileDefaultExpirationTime()
	{
		return cfgTileDefaultExpirationTime;
	}

	public static void setTileDefaultExpirationTime(long tileDefaultExpirationTime)
	{
		cfgTileDefaultExpirationTime = tileDefaultExpirationTime;
	}

	// instance data, usually all protected
	// esp. this classes instances are load from a xml-file by loadOrQuit()

	/**
	 * Version of this settings file
	 */
	// W #??? @XmlElement(defaultValue = "") cfgVersion <-> version?
	protected String cfgVersion;
	/**
	 * user agent used for connections to tile servers
	 */
	protected String cfgUserAgent = null;
	/**
	 * List with directories
	 */
	@XmlElement(name = "directories")
	protected Directories cfgDirectories = new Directories();
	/**
	 * Use tilestore or not
	 */
	protected boolean cfgTileStoreEnabled = true;
	protected int cfgDownloadThreadCount = 2;
	protected int cfgDownloadRetryCount = 1;
	protected CoordinateStringFormat cfgCoordinateNumberFormat = CoordinateStringFormat.DEG_LOCAL;
	/**
	 * settings for the lat/lon grid
	 */
	@XmlElement(name = "wgsGrid")
	protected final WgsGridSettings cfgWgsGrid = new WgsGridSettings();
	protected transient UnitSystem cfgUnitSystem = UnitSystem.Metric; // W ? transient <-> XmlElement ? s.u.
	protected String cfgLocaleLanguage = Locale.getDefault().getLanguage();
	protected String cfgLocaleCountry = Locale.getDefault().getCountry();
	/**
	 * which tile size is to be used
	 */
	protected Dimension cfgTileSize = new Dimension(256, 256);
	/**
	 * the file format for the downloaded tiles
	 */
	protected TileImageFormat cfgTileImageFormat = TileImageFormat.PNG;
	/**
	 * Connection timeout in seconds (default 10 seconds)
	 */
	protected int cfgHttpConnectionTimeout = 10;

	/**
	 * Read timeout in seconds (default 10 seconds)
	 */
	protected int cfgHttpReadTimeout = 10;

	/**
	 * Maximum expiration (in milliseconds) acceptable. If a server sets an expiration time larger than this value it is truncated to this value on next download.
	 */
	protected long cfgTileMaxExpirationTime = TimeUnit.DAYS.toMillis(365);

	/**
	 * Minimum expiration (in milliseconds) acceptable. If a server sets an expiration time smaller than this value it is truncated to this value on next
	 * download.
	 */
	protected long cfgTileMinExpirationTime = TimeUnit.DAYS.toMillis(5);

	@XmlElementWrapper(name = "mapSourcesDisabled")
	@XmlElement(name = "mapSource")
	public Vector<String> mapSourcesDisabled = new Vector<String>();

	@XmlElementWrapper(name = "mapSourcesEnabled")
	@XmlElement(name = "mapSource")
	public Vector<String> mapSourcesEnabled = new Vector<String>();

	protected ACSettings()
	{
	}

	@Override
	public String getUserAgent()
	{
		if (cfgUserAgent != null)
			return cfgUserAgent;
		else
			return "OSMB-unknown";
	}

	public void setUserAgent(String userAgent)
	{
		if (userAgent != null)
		{
			userAgent = userAgent.trim();
			if (userAgent.length() == 0)
				userAgent = null;
		}
		this.cfgUserAgent = userAgent;
	}

	@Override
	public String getVersion()
	{
		return cfgVersion;
	}

	public void setVersion(String cfgVersion)
	{
		this.cfgVersion = cfgVersion;
	}

	@Override
	@XmlElement
	// W #??? transient <-> XmlElement ? s.o.
	public UnitSystem getUnitSystem()
	{
		return cfgUnitSystem;
	}

	public void setUnitSystem(UnitSystem unitSystem)
	{
		if (unitSystem == null)
			unitSystem = UnitSystem.Metric;
		this.cfgUnitSystem = unitSystem;
	}

	@XmlJavaTypeAdapter(DimensionAdapter.class)
	public Dimension getTileSize()
	{
		return cfgTileSize;
	}

	public void setTileSize(Dimension tileSize)
	{
		this.cfgTileSize = tileSize;
	}

	// W #??? @XmlTransient // W #---
	public TileImageFormat getTileImageFormat()
	{
		return cfgTileImageFormat;
	}

	public void setTileImageFormat(TileImageFormat tileImageFormat)
	{
		this.cfgTileImageFormat = tileImageFormat;
	}

	// W see => @XmlElement(name = "directories") at: protected Directories cfgDirectories = new Directories();
	// // W unused
	// public Directories getDirectories()
	// {
	// return cfgDirectories;
	// }
	//
	// // W unused (how to?)
	// public void setDirectories(Directories cfgDirectories)
	// {
	// this.cfgDirectories = cfgDirectories;
	// }

	// W not a directory adjustable by user -> is determined in DirectoryManager
	public File getMapSourcesDirectory()
	{
		File mapSourcesDir;
		mapSourcesDir = DirectoryManager.mapSourcesDir;
		return mapSourcesDir;
	}

	@XmlTransient
	public File getTileStoreDirectory()
	{
		String tileStoreDirSet = cfgDirectories.tileStoreDirectory;
		File tileStoreDir;
		if (tileStoreDirSet == null || tileStoreDirSet.trim().length() == 0)
			tileStoreDir = DirectoryManager.tileStoreDir;
		else
			tileStoreDir = new File(tileStoreDirSet);
		return tileStoreDir;
	}

	public void setTileStoreDirectory(File tileStoreDir)
	{
		cfgDirectories.tileStoreDirectory = tileStoreDir.toString();
	}

	@XmlTransient
	public File getCatalogsDirectory()
	{
		String dirSetting = cfgDirectories.catalogsDirectory;
		File catalogsDir;
		if (dirSetting == null || dirSetting.trim().length() == 0)
			catalogsDir = DirectoryManager.catalogsDir;
		else
			catalogsDir = new File(dirSetting);
		return catalogsDir;
	}

	public void setCatalogsDirectory(File catalogsDir)
	{
		cfgDirectories.catalogsDirectory = catalogsDir.toString();
	}

	@XmlTransient
	public File getChartBundleOutputDirectory()
	{
		String dirSetting = cfgDirectories.bundleOutputDirectory;
		File bundlesDir;
		if (dirSetting == null || dirSetting.trim().length() == 0)
			bundlesDir = DirectoryManager.bundlesDir;
		else
			bundlesDir = new File(dirSetting);
		return bundlesDir;
	}

	public void setChartBundleOutputDirectory(File bundlesDir)
	{
		cfgDirectories.bundleOutputDirectory = bundlesDir.toString();
	}

	public WgsGridSettings getWgsGrid()
	{
		return cfgWgsGrid;
	}

	public String getLocaleLanguage()
	{
		return cfgLocaleLanguage;
	}

	public void setLocaleLanguage(String localeLanguage)
	{
		this.cfgLocaleLanguage = localeLanguage;
	}

	public String getLocaleCountry()
	{
		return cfgLocaleCountry;
	}

	public void setLocaleCountry(String localeCountry)
	{
		this.cfgLocaleCountry = localeCountry;
	}

	public boolean getTileStoreEnabled()
	{
		return cfgTileStoreEnabled;
	}

	public void setTileStoreEnabled(boolean tileStoreEnabled)
	{
		this.cfgTileStoreEnabled = tileStoreEnabled;
	}

	public int getDownloadThreadCount()
	{
		return cfgDownloadThreadCount;
	}

	public void setDownloadThreadCount(int downloadThreadCount)
	{
		this.cfgDownloadThreadCount = downloadThreadCount;
	}

	public long getTileMaxExpirationTime()
	{
		return cfgTileMaxExpirationTime;
	}

	public void setTileMaxExpirationTime(long tileMaxExpirationTime)
	{
		this.cfgTileMaxExpirationTime = tileMaxExpirationTime;
	}

	public long getTileMinExpirationTime()
	{
		return cfgTileMinExpirationTime;
	}

	public void setTileMinExpirationTime(long tileMinExpirationTime)
	{
		this.cfgTileMinExpirationTime = tileMinExpirationTime;
	}

	public CoordinateStringFormat getCoordinateNumberFormat()
	{
		return cfgCoordinateNumberFormat;
	}

	public void setCoordinateNumberFormat(CoordinateStringFormat coordinateNumberFormat)
	{
		this.cfgCoordinateNumberFormat = coordinateNumberFormat;
	}

	public int getDownloadRetryCount()
	{
		return cfgDownloadRetryCount;
	}

	public void setDownloadRetryCount(int downloadRetryCount)
	{
		this.cfgDownloadRetryCount = downloadRetryCount;
	}

	public int getHttpConnectionTimeout()
	{
		return cfgHttpConnectionTimeout;
	}

	public void setHttpConnectionTimeout(int httpConnectionTimeout)
	{
		this.cfgHttpConnectionTimeout = httpConnectionTimeout;
	}

	public int getHttpReadTimeout()
	{
		return cfgHttpReadTimeout;
	}

	public void setHttpReadTimeout(int httpReadTimeout)
	{
		this.cfgHttpReadTimeout = httpReadTimeout;
	}
}
