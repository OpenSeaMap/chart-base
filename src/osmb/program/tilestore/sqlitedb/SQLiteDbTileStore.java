package osmb.program.tilestore.sqlitedb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.TileAddress;
import osmb.program.ACSettings;
import osmb.program.tiles.SQLiteLoader;
import osmb.program.tiles.Tile;
import osmb.program.tiles.Tile.TileState;
import osmb.program.tilestore.ACNTileStore;
import osmb.program.tilestore.TileStoreException;
import osmb.program.tilestore.TileStoreInfo;
import osmb.utilities.OSMBStrs;
import osmb.utilities.OSMBUtilities;

/**
 * This class is intended to replace the 'old' berkelydb tilestore. It is not a singleton any longer.
 * It is neccessary to have several tile stores open at the same time.
 * This is valid for one app, as well as for different apps.
 * <p>
 * Tiles to be hold in the SQLite db, maps stored in file system and paths in the map store db, see: https://www.sqlite.org/intern-v-extern-blob.html
 * This says approximately > 200kB file system is faster; < 20kB SQLite is faster
 * <p>
 * The class holds an enumeration of all instances.
 * Each map sources tile store is placed in a separate file.
 * 
 * @author humbach
 */
public class SQLiteDbTileStore extends ACNTileStore
{
	// class/static data
	protected static Logger log = Logger.getLogger(SQLiteDbTileStore.class);
	// common tile store database
	private static final int MAX_BATCH_SIZE = 1000;
	private static final String TS_COMMONDB = "TSCommon";
	private static final String EXT_COMMONDB = "sqlitedb";
	private static final String BEGIN_TA = "begin transaction";
	private static final String COMMIT_TA = "commit transaction";
	private static final String ROLLBACK_TA = "rollback transaction";
	// tile store info table
	private static final String CREATE_TSINFO = "create table if not exists TS_INFO (SID int, TS_NAME text, TS_SNAME text, primary key (SID))";
	private static final String INSERT_TSINFO = "insert or replace into TS_INFO (SID, TS_NAME, TS_SNAME) values (?,?,?)";
	private static final String MAXID_TSINFO = "select distinct SID from TS_INFO order by SID desc limit 1";
	private static final String SNAME_TSINFO = "select TS_SNAME from TS_INFO where (SID=?)";
	private static final String IDS_TSINFO = "select SID from TS_INFO where (TS_NAME like ?)";
	// tiles table
	private static final String CREATE_TILES = "create table if not exists TILES (Z int, X int, Y int, MOD int, EXP int, ETAG text, FK_IID int, primary key (Z,X,Y))";
	private static final String INDEXTILE_TILES = "create index if not exists IX_TILE on TILES (Z,X,Y)";
	private static final String INDEXMOD_TILES = "create index if not exists IX_MOD on TILES (MOD)";
	private static final String INDEXEXP_TILES = "create index if not exists IX_EXP on TILES (EXP)";
	private static final String INDEXETAG_TILES = "create index if not exists IX_ETAG on TILES (ETAG)";
	private static final String INSERT_TILES = "insert or replace into TILES (Z,X,Y,MOD,EXP,ETAG,FK_IID) values (?,?,?,?,?,?,?)";
	private static final String CLEAR_TILES = "delete from TILES";
	private static final String IID_TILES = "select FK_IID from TILES where (Z=?) and (X=?) and (Y=?)";
	private static final String EXP_TILES = "select EXP from TILES where (Z=?) and (X=?) and (Y=?)";
	private static final String MOD_TILES = "select MOD from TILES where (Z=?) and (X=?) and (Y=?)";
	private static final String ETAG_TILES = "select ETAG from TILES where (Z=?) and (X=?) and (Y=?)";
	private static final String NIID_TILES = "select max(FK_IID) from TILES";
	// images table
	private static final String CREATE_IMAGES = "create table if not exists IMAGES (IID int, IMAGE blob, primary key (IID))";
	private static final String INSERT_IMAGES = "insert or replace into IMAGES (IID, IMAGE) values (?,?)";
	private static final String CLEAR_IMAGES = "delete from IMAGES";
	private static final String IMAGE_IMAGES = "select IMAGE from IMAGES where (IID=?)";
	private static final String NIID_IMAGES = "select max(IID) from IMAGES";
	// 'magic' tiles ids come from Tile

	// private static final String TCNT_TILES = "SELECT DISTINCT cnt(id_s) FROM tiles ORDER BY id_s DESC LIMIT 1;";

	// SQLite access samples:
	// stat.executeUpdate("INSERT INTO android_metadata VALUES ('" + locale + "')");
	//
	// prepStmt = conn.prepareStatement("INSERT or REPLACE INTO tiles (x,y,z,s,image) VALUES (?,?,?,0,?)");
	// prepStmt.setInt(1, x);
	// prepStmt.setInt(2, y);
	// prepStmt.setInt(3, 17 - z);
	// prepStmt.setBytes(4, tileData);
	// prepStmt.addBatch();
	// prepStmt.executeBatch();
	// prepStmt.clearBatch();
	// conn.commit();

	// common tile store data
	protected static Path sTileStoreBase = ACSettings.getInstance().getTileStoreDirectory().toPath();
	/**
	 * Connection to the common tile store database
	 */
	protected static Connection sTSConn = null;

	/**
	 * Factory method. The class maintains a hash map with all instances.
	 * 
	 * @param mapSource
	 * @throws TileStoreException
	 */
	public static SQLiteDbTileStore prepareTileStore(ACMapSource mapSource) throws TileStoreException
	{
		log.trace(OSMBStrs.RStr("START"));
		try
		{
			if (!sInitialized)
				initializeCommonDB();
			if (!sInitialized)
			{
				log.error(OSMBStrs.RStr("TileStore.NotInit"));
				throw new TileStoreException(OSMBStrs.RStr("TileStore.NotInit"));
			}

			SQLiteDbTileStore tStore = (SQLiteDbTileStore) sHM.get(mapSource);
			if (tStore == null)
			{
				tStore = new SQLiteDbTileStore();
				sHM.put(mapSource, tStore);
			}
			if (!tStore.isInitialized())
			{
				tStore.mMapSource = mapSource;
				tStore.initializeDB();
			}
			return tStore;
		}
		catch (SQLException e)
		{
			throw new TileStoreException("Error creating SQL database for '" + mapSource.getName() + "': " + e.getMessage(), e);
		}
	}

	/**
	 * @return A String array with all tile stores loaded at the moment.
	 */
	public static String[] getStoresList()
	{
		log.trace(OSMBStrs.RStr("START"));
		String[] strList = null;
		// TODO Auto-generated method stub
		return strList;
	}

	/**
	 * @param mapSource
	 * @return TRUE if the tile store for the specified map source is already loaded and ok.
	 */
	public static boolean isStoreValid(ACMapSource mapSource)
	{
		log.trace(OSMBStrs.RStr("START"));
		boolean bOk = false;
		// TODO Auto-generated method stub
		return bOk;
	}

	/**
	 * Returns <code>true</code> if the tile store of the specified {@link ACMapSource} exists.
	 * 
	 * @param mapSource
	 * @return
	 */
	public static boolean storeExists(ACMapSource mapSource)
	{
		log.trace(OSMBStrs.RStr("START"));
		return (sHM.get(mapSource) != null);
	}

	/**
	 * Runs over all map source stores in all tile store instances.
	 */
	public static void closeAllStores()
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @param pDB
	 *          The Path of the database to connect to.
	 * @return The successfully opened connection to the SQLite db.
	 * @throws SQLException
	 */
	protected static Connection openConnection(Path pDB) throws SQLException
	{
		log.trace(OSMBStrs.RStr("START"));
		Connection conn = null;
		{
			String url = "jdbc:sqlite:/" + pDB.toAbsolutePath();
			conn = DriverManager.getConnection(url);
		}
		return conn;
	}

	protected static Connection reopenConnection(Path pDB, Connection conn) throws SQLException
	{
		log.trace(OSMBStrs.RStr("START"));
		if ((conn == null) || (conn.isClosed()))
		{
			conn = openConnection(pDB);
		}
		else
			log.debug("connection already open: '" + conn.toString() + "'");
		return conn;
	}

	public static void initializeCommonDB() throws SQLException
	{
		log.trace(OSMBStrs.RStr("START"));
		try
		{
			if (!SQLiteLoader.isLoaded())
				SQLiteLoader.loadSQLite();
			if (sTSConn == null)
			{
				if (!sTileStoreBase.toFile().isDirectory())
					OSMBUtilities.mkDirs(sTileStoreBase);
				Path pDB = sTileStoreBase.resolve(TS_COMMONDB + "." + EXT_COMMONDB);
				sTSConn = openConnection(pDB);
			}
			synchronized (sTSConn)
			{
				if ((sTSConn != null) && (!sInitialized))
				{
					Statement stmt = sTSConn.createStatement();
					stmt.executeUpdate(BEGIN_TA);
					stmt.executeUpdate(CREATE_TSINFO);
					stmt.executeUpdate(COMMIT_TA);
					sInitialized = true;
				}
				if (sTSConn == null)
					log.error("no common SQLiteDB connection");
			}
		}
		catch (SQLException | IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Instance data
	protected Connection mConn = null;
	protected PreparedStatement mPrepStmt = null;
	protected Path mTileStoreDB = null;
	protected ACMapSource mMapSource = null;
	protected int mMS_ID = 0;
	protected boolean mInitialized = false;

	/**
	 * 
	 */
	private SQLiteDbTileStore()
	{
		try
		{
			if (!sInitialized)
				SQLiteDbTileStore.initializeCommonDB();
		}
		catch (SQLException e)
		{
			log.error("exception in SQLiteDbTileStore() constructor");
			e.printStackTrace();
		}
	}

	public synchronized void initialize()
	{
		log.trace(OSMBStrs.RStr("START"));
		// testing of SQLite tile store
		try
		{
			SQLiteDbTileStore.initializeCommonDB();
			if (mConn == null)
			{
				mTileStoreDB = sTileStoreBase.resolve(mMapSource.getName() + "." + EXT_COMMONDB);
				mConn = openConnection(mTileStoreDB);
				initializeDB();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * This clears the whole tile store for this map source.
	 * 
	 * @throws TileStoreException
	 */
	@Override
	public void clearStore() throws TileStoreException
	{
		log.trace(OSMBStrs.RStr("START"));
		Statement stmt;
		try
		{
			if (mConn == null)
			{
				prepareTileStore(mMapSource);
			}
			if (mConn != null)
			{
				stmt = mConn.createStatement();
				stmt.executeUpdate(BEGIN_TA);
				stmt.executeUpdate(CLEAR_TILES);
				stmt.executeUpdate(CLEAR_IMAGES);
				stmt.executeUpdate(COMMIT_TA);
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Writes the tiles info into TILES table
	 */
	protected void writeTileInfo(int x, int y, int z, long mod, long exp, String eTag, long nImg_ID) throws SQLException
	{
		// write the tile info
		mPrepStmt = mConn.prepareStatement(INSERT_TILES);
		mPrepStmt.setInt(1, z);
		mPrepStmt.setInt(2, x);
		mPrepStmt.setInt(3, y);
		mPrepStmt.setLong(4, mod);
		mPrepStmt.setLong(5, exp);
		mPrepStmt.setString(6, eTag);
		mPrepStmt.setLong(7, nImg_ID);
		mPrepStmt.addBatch();
		int[] upd = mPrepStmt.executeBatch();
	}

	/**
	 * Writes the tiles image into IMAGES table
	 */
	protected void writeTileData(long nImg_ID, byte[] tileData) throws SQLException
	{
		mPrepStmt = mConn.prepareStatement(INSERT_IMAGES);
		mPrepStmt.setLong(1, nImg_ID);
		mPrepStmt.setBytes(2, tileData);
		mPrepStmt.addBatch();
		mPrepStmt.executeBatch();
	}

	@Override
	public void putTile(Tile tile)
	{
		log.trace(OSMBStrs.RStr("START"));
		try
		{
			synchronized (mConn)
			{
				mConn.prepareStatement(BEGIN_TA).executeUpdate();
				switch (tile.getTileState())
				{
					case TS_LOADED:
					case TS_EXPIRED:
						// find next free image id
						long nNextImg_ID = 100;
						// ResultSet rs = mConn.prepareStatement(NIID_IMAGES).executeQuery();
						mPrepStmt = mConn.prepareStatement(IID_TILES);
						mPrepStmt.setInt(1, tile.getZoom());
						mPrepStmt.setInt(2, tile.getXtile());
						mPrepStmt.setInt(3, tile.getYtile());
						ResultSet rs = mPrepStmt.executeQuery();
						if (rs.next())
						{
							nNextImg_ID = rs.getLong(1);
							log.debug("found image id=" + nNextImg_ID);
							rs.close();
						}
						else
						{
							rs.close();
							rs = mConn.prepareStatement(NIID_TILES).executeQuery();
							if (rs.next())
								nNextImg_ID = Math.max(nNextImg_ID, rs.getLong(1) + 1);
						}
						writeTileData(nNextImg_ID, tile.getImageData());
						writeTileInfo(tile.getXtile(), tile.getYtile(), tile.getZoom(), tile.getMod().getTime(), tile.getExp().getTime(), tile.getETag(), nNextImg_ID);
						log.debug("image " + nNextImg_ID + " written for " + tile);
						break;
					case TS_ERROR:
						writeTileInfo(tile.getXtile(), tile.getYtile(), tile.getZoom(), tile.getMod().getTime(), tile.getExp().getTime(), tile.getETag(),
						    Tile.ERROR_TILE_ID);
						log.warn("error image written for " + tile);
						break;
					default:
						// TS_LOADING tiles will not be written into the tile store
						// TS_NEW tiles will not be written into the tile store
						// TS_ZOOMED tiles will not be written into the tile store
						log.debug("no image written for " + tile);
						break;
				}
				mConn.prepareStatement(COMMIT_TA).executeUpdate();
			}
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			try
			{
				mConn.prepareStatement(ROLLBACK_TA).executeUpdate();
			}
			catch (SQLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void putTileData(byte[] tileData, TileAddress tAddr) throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		Tile tile = new Tile(mMapSource, tAddr);
		tile.loadImage(tileData);
		putTile(tile);
	}

	public boolean contains(int x, int y, int zoom)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * This initialized a SQLiteDB tile store for the specified map source.
	 * The tiles table is created if it does not already exist.
	 * It checks in the common db if this map source already is registered, i.e. it has an id_s entry.
	 * 
	 * @throws SQLException
	 */
	@Override
	protected void initializeDB() throws SQLException
	{
		log.trace(OSMBStrs.RStr("START"));
		if (!mInitialized)
			initialize();
		synchronized (mConn)
		{
			Statement stmt = mConn.createStatement();
			stmt.executeUpdate(BEGIN_TA);
			stmt.executeUpdate(CREATE_TILES);
			stmt.executeUpdate(INDEXTILE_TILES);
			stmt.executeUpdate(INDEXMOD_TILES);
			stmt.executeUpdate(INDEXEXP_TILES);
			stmt.executeUpdate(INDEXETAG_TILES);
			stmt.executeUpdate(CREATE_IMAGES);
			stmt.executeUpdate(COMMIT_TA);
		}
		synchronized (sTSConn)
		{
			mPrepStmt = sTSConn.prepareStatement(IDS_TSINFO);
			mPrepStmt.setString(1, mMapSource.getName());
			ResultSet rs = mPrepStmt.executeQuery();
			if (rs.next())
				mMS_ID = rs.getInt(1);
			else
			{
				rs.close();
				mPrepStmt = sTSConn.prepareStatement(MAXID_TSINFO);
				rs = mPrepStmt.executeQuery();
				if (rs.next())
					mMS_ID = rs.getInt(1) + 1;
				else
					mMS_ID = 1;
				rs.close();
				mPrepStmt = sTSConn.prepareStatement(INSERT_TSINFO);
				mPrepStmt.setInt(1, mMS_ID);
				mPrepStmt.setString(2, mMapSource.getName());
				mPrepStmt.setString(3, mMapSource.getName());
				mPrepStmt.execute();
			}
		}
		mInitialized = true;
	}

	@Override
	public TileStoreInfo getStoreInfo() throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public BufferedImage getCacheCoverage(int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Member method exists for compatibility only. Use class method {@link #getStoresList()}
	 */
	@Override
	public String[] getAllStoreNames()
	{
		return getStoresList();
	}

	public void close()
	{
		log.trace(OSMBStrs.RStr("START"));
		if (mConn != null)
		{
			try
			{
				sHM.remove(mMapSource);
				mConn.commit();
				mConn.close();
				mConn = null;
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public Tile getTile(TileAddress tAddr)
	{
		log.trace(OSMBStrs.RStr("START"));
		Tile tile = null;
		try
		{
			log.debug("search for " + tAddr);
			long nIId = 0;
			byte[] data = null;
			tile = new Tile(mMapSource, tAddr);
			// get image id from tiles
			synchronized (mConn)
			{
				mPrepStmt = mConn.prepareStatement(IID_TILES);
				mPrepStmt.setInt(1, tAddr.getZoom());
				mPrepStmt.setInt(2, tAddr.getX());
				mPrepStmt.setInt(3, tAddr.getY());
				ResultSet rs = mPrepStmt.executeQuery();
				if (rs.next())
				{
					nIId = rs.getLong(1);
					log.debug("found image id=" + nIId);
					rs.close();
					if (nIId == Tile.ERROR_TILE_ID)
					{
						tile.setErrorImage();
					}
					else
					{
						mPrepStmt = mConn.prepareStatement(IMAGE_IMAGES);
						mPrepStmt.setLong(1, nIId);
						rs = mPrepStmt.executeQuery();
						if (rs.next())
						{
							data = rs.getBytes(1);
							tile.loadImage(rs.getBytes(1));
							tile.setExp(new Date(getExp(tAddr)));
							tile.setMod(new Date(getMod(tAddr)));
							tile.setETag(getETag(tAddr));
							tile.setTileState(TileState.TS_LOADED);
							log.debug("bytes=" + data.length);
						}
						rs.close();
					}
				}
				else
				{
					rs.close();
					// log.debug("no image found for " + tAddr + ". Stmt='" + mPrepStmt + "'");
					log.debug("no image found for " + tAddr);
				}
			}
		}
		catch (SQLException | IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tile;
	}

	protected long getMod(TileAddress tAddr) throws SQLException
	{
		long tMod = System.currentTimeMillis();
		mPrepStmt = mConn.prepareStatement(MOD_TILES);
		mPrepStmt.setInt(1, tAddr.getZoom());
		mPrepStmt.setInt(2, tAddr.getX());
		mPrepStmt.setInt(3, tAddr.getY());
		ResultSet rs = mPrepStmt.executeQuery();
		if (rs.next())
		{
			tMod = rs.getLong(1);
			log.debug("modified=" + new Date(tMod));
		}
		rs.close();
		return tMod;
	}

	protected long getExp(TileAddress tAddr) throws SQLException
	{
		long tExp = System.currentTimeMillis();
		mPrepStmt = mConn.prepareStatement(EXP_TILES);
		mPrepStmt.setInt(1, tAddr.getZoom());
		mPrepStmt.setInt(2, tAddr.getX());
		mPrepStmt.setInt(3, tAddr.getY());
		ResultSet rs = mPrepStmt.executeQuery();
		if (rs.next())
		{
			tExp = rs.getLong(1);
			log.debug("expires=" + new Date(tExp));
		}
		rs.close();
		return tExp;
	}

	protected String getETag(TileAddress tAddr) throws SQLException
	{
		String eTag = "-";
		mPrepStmt = mConn.prepareStatement(ETAG_TILES);
		mPrepStmt.setInt(1, tAddr.getZoom());
		mPrepStmt.setInt(2, tAddr.getX());
		mPrepStmt.setInt(3, tAddr.getY());
		ResultSet rs = mPrepStmt.executeQuery();
		if (rs.next())
		{
			eTag = rs.getString(1);
			log.debug("eTag=" + eTag);
		}
		rs.close();
		return eTag;
	}

	@Override
	public BufferedImage getCacheCoverage(ACMapSource mapSource, int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInitialized()
	{
		return mInitialized;
	}

	@Override
	public void putTileData(byte[] tileData, TileAddress tAddr, long timeLastModified, long timeExpires, String eTag) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean containsTile(TileAddress tAddr)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTileExpired(TileAddress tAddr)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
