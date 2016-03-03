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
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import osmb.mapsources.ACMapSource;
import osmb.mapsources.TileAddress;
import osmb.program.ACSettings;
import osmb.program.tiles.SQLiteLoader;
import osmb.program.tiles.Tile;
import osmb.program.tilestore.ACTileStore;
import osmb.program.tilestore.IfStoredTile;
import osmb.program.tilestore.TileStoreException;
import osmb.program.tilestore.TileStoreInfo;
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
public class SQLiteDbTileStore extends ACTileStore
{
	// class/static data
	protected static Logger log = Logger.getLogger(SQLiteDbTileStore.class);
	// common tile store database
	private static final int MAX_BATCH_SIZE = 1000;
	private static final String TS_COMMONDB = "TSCommon";
	private static final String EXT_COMMONDB = "sqlitedb";
	private static final String BEGIN_TA = "BEGIN TRANSACTION";
	private static final String COMMIT_TA = "COMMIT TRANSACTION";
	private static final String ROLLBACK_TA = "ROLLBACK TRANSACTION";

	private static final String CREATE_TSINFO = "CREATE TABLE IF NOT EXISTS ts_info (id_s int, ts_name char, ts_sname char, PRIMARY KEY (id_s))";
	private static final String INSERT_TSINFO = "INSERT or REPLACE INTO ts_info (id_s, ts_name, ts_sname) VALUES (?,?,?)";
	private static final String MAXID_TSINFO = "SELECT DISTINCT id_s FROM ts_info ORDER BY id_s DESC LIMIT 1";
	private static final String SNAME_TSINFO = "SELECT ts_sname FROM ts_info WHERE (id_s=?)";
	private static final String IDS_TSINFO = "SELECT id_s FROM ts_info WHERE (ts_name LIKE ?)";
	// tiles table
	private static final String CREATE_TILES = "CREATE TABLE IF NOT EXISTS tiles (z int, x int, y int, mod datetime, exp datetime, etag char, fk_iid int, PRIMARY KEY (z,x,y))";
	private static final String INDEXTILE_TILES = "CREATE INDEX IF NOT EXISTS IND on tiles (z,x,y)";
	private static final String INDEXMD_TILES = "CREATE INDEX IF NOT EXISTS IND on tiles (mod)";
	private static final String INDEXEXP_TILES = "CREATE INDEX IF NOT EXISTS IND on tiles (exp)";
	private static final String INDEXETAG_TILES = "CREATE INDEX IF NOT EXISTS IND on tiles (etag)";
	private static final String INSERT_TILES = "INSERT or REPLACE INTO tiles (z,x,y,mod,exp,etag,fk_iid) VALUES (?,?,?,?,?,?,?)";
	private static final String CLEAR_TILES = "DELETE * FROM tiles;";
	private static final String IID_TILES = "SELECT fk_iid FROM tiles WHERE (z=?) AND (x=?) AND (y=?)";
	private static final String EXP_TILES = "SELECT exp FROM tiles WHERE (z=?) AND (x=?) AND (y=?)";
	private static final String MOD_TILES = "SELECT mod FROM tiles WHERE (z=?) AND (x=?) AND (y=?)";
	private static final String ETAG_TILES = "SELECT etag FROM tiles WHERE (z=?) AND (x=?) AND (y=?)";
	// images table
	private static final String CREATE_IMAGES = "CREATE TABLE IF NOT EXISTS images (i_id int, image blob, PRIMARY KEY (i_id))";
	private static final String INSERT_IMAGES = "INSERT or REPLACE INTO images (i_id,image) VALUES (?,?)";
	private static final String CLEAR_IMAGES = "DELETE * FROM images;";
	private static final String IMAGE_IMAGES = "SELECT image FROM images WHERE (i_id=?)";
	private static final String NIID_IMAGES = "SELECT max(i_id) FROM images";
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

	public static synchronized void initialize()
	{
		// testing of SQLite tile store
		try
		{
			initializeCommonDB();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @return A String array with all tile stores loaded at the moment.
	 */
	public static String[] getStoresList()
	{
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
		// TODO Auto-generated method stub
		return false;
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
		Connection conn = null;
		{
			String url = "jdbc:sqlite:/" + pDB.toAbsolutePath();
			conn = DriverManager.getConnection(url);
		}
		return conn;
	}

	protected static Connection reopenConnection(Path pDB, Connection conn) throws SQLException
	{
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
			if (sTSConn != null)
			{
				Statement stmt = sTSConn.createStatement();
				stmt.executeUpdate(CREATE_TSINFO);
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

	/**
	 * 
	 */
	public SQLiteDbTileStore()
	{
	}

	/**
	 * This clears the whole tile store for this map source.
	 */
	@Override
	public void clearStore(String storeName)
	{
		Statement stmt;
		try
		{
			stmt = mConn.createStatement();
			stmt.executeUpdate(CLEAR_TILES);
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void putTileData(byte[] tileData, int x, int y, int zoom) throws IOException
	{
		long md = System.currentTimeMillis();
		long exp = md + ACSettings.getTileDefaultExpirationTime();
		putTileData(tileData, x, y, zoom, md, exp, "");
	}

	/**
	 * 
	 */
	// public void putTileData(byte[] tileData, int x, int y, int z, IfMapSource mapSource, long timeLastModified, long timeExpires, String eTag)
	// throws IOException
	public void putTileData(byte[] tileData, int x, int y, int z, long md, long exp, String eTag) throws IOException
	{
		try
		{
			if (mConn == null)
			{
				prepareTileStore(mMapSource);
			}
			if (mConn != null)
			{
				mConn.prepareStatement(BEGIN_TA).executeUpdate();
				// find next free image id
				int nNextImg_ID = 100;
				ResultSet rs = mConn.prepareStatement(NIID_IMAGES).executeQuery();
				if (rs.next())
					nNextImg_ID = rs.getInt(1) + 1;
				writeTileData(nNextImg_ID, tileData);
				writeTileInfo(x, y, z, md, exp, eTag, nNextImg_ID);
				mConn.prepareStatement(COMMIT_TA).executeUpdate();
			}
		}
		catch (SQLException | TileStoreException e)
		{
			log.error("Exception while inserting image data");
			// mConn.prepareStatement(ROLLBACK_TA).executeUpdate();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void writeTileInfo(int x, int y, int z, long md, long exp, String eTag, int nNextImg_ID) throws SQLException
	{
		// write the tile info
		mPrepStmt = mConn.prepareStatement(INSERT_TILES);
		mPrepStmt.setInt(1, z);
		mPrepStmt.setInt(2, x);
		mPrepStmt.setInt(3, y);
		mPrepStmt.setTimestamp(4, new Timestamp(md));
		mPrepStmt.setTimestamp(5, new Timestamp(exp));
		mPrepStmt.setString(6, eTag);
		mPrepStmt.setInt(7, nNextImg_ID);
		mPrepStmt.addBatch();
		mPrepStmt.executeBatch();
	}

	protected void writeTileData(int nNextImg_ID, byte[] tileData) throws SQLException
	{
		mPrepStmt = mConn.prepareStatement(INSERT_IMAGES);
		mPrepStmt.setInt(1, nNextImg_ID);
		mPrepStmt.setBytes(2, tileData);
		mPrepStmt.executeBatch();
	}

	public void putTile(Tile tile)
	{
		try
		{
			mConn.prepareStatement(BEGIN_TA).executeUpdate();
			switch (tile.getTileState())
			{
				case TS_LOADED:
					// find next free image id
					int nNextImg_ID = 100;
					ResultSet rs = mConn.prepareStatement(NIID_IMAGES).executeQuery();
					if (rs.next())
						nNextImg_ID = rs.getInt(1) + 1;
					// writeTileData(nNextImg_ID, tile.getImage().);
					writeTileInfo(tile.getXtile(), tile.getYtile(), tile.getZoom(), tile.getMod().getTime(), tile.getExp().getTime(), tile.getEtag(), nNextImg_ID);
					break;
				case TS_ERROR:
					writeTileInfo(tile.getXtile(), tile.getYtile(), tile.getZoom(), tile.getMod().getTime(), tile.getExp().getTime(), tile.getEtag(), Tile.ERROR_TILE_ID);
					break;
				default:
					break;
			}
			mConn.prepareStatement(COMMIT_TA).executeUpdate();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public IfStoredTile getTileEntry(int x, int y, int zoom)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean contains(int x, int y, int zoom)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void prepareTileStore(ACMapSource mapSource) throws TileStoreException
	{
		try
		{
			if (!SQLiteLoader.isLoaded() || !(sTSConn == null))
				initializeCommonDB();
			mTileStoreDB = sTileStoreBase.resolve(mapSource.getName() + "." + EXT_COMMONDB);
			mConn = openConnection(mTileStoreDB);
			mMapSource = mapSource;
			initializeDB();
		}
		catch (SQLException e)
		{
			throw new TileStoreException("Error creating SQL database \"" + mTileStoreDB.toAbsolutePath() + "\": " + e.getMessage(), e);
		}
	}

	/**
	 * This initialized a SQLiteDB tile store for the specified map source.
	 * The tiles table is created if it does not already exist.
	 * It checks in the common db is this map source already is registered, i.e. it has an id_s entry.
	 * 
	 * @throws SQLException
	 */
	protected void initializeDB() throws SQLException
	{
		Statement stmt = mConn.createStatement();
		stmt.executeUpdate(CREATE_TILES);
		stmt.executeUpdate(INDEXTILE_TILES);
		stmt.executeUpdate(INDEXMD_TILES);
		stmt.executeUpdate(INDEXEXP_TILES);
		stmt.executeUpdate(INDEXETAG_TILES);
		stmt.executeUpdate(CREATE_IMAGES);

		mPrepStmt = sTSConn.prepareStatement(IDS_TSINFO);
		mPrepStmt.setString(1, mMapSource.getName());
		ResultSet rs = mPrepStmt.executeQuery();
		if (rs.next())
			mMS_ID = rs.getInt(1);
		else
		{
			rs.close();
			rs = stmt.executeQuery(MAXID_TSINFO);
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

	@Override
	public TileStoreInfo getStoreInfo(String mapSourceName) throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public BufferedImage getCacheCoverage(int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IfStoredTile createNewEntry(int x, int y, int zoom, byte[] data, long timeLastModified, long timeExpires, String eTag)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IfStoredTile createNewEmptyEntry(int x, int y, int zoom)
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

	/**
	 * Member method exists for compatibility only. Use class method {@link #closeAllStores()}
	 */
	@Override
	public void closeAll()
	{
		closeAllStores();
		close();
	}

	public void close()
	{
		if (mConn != null)
		{
			try
			{
				mConn.commit();
				mConn.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void putTileData(byte[] tileData, int x, int y, int zoom, ACMapSource mapSource) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void putTileData(byte[] tileData, TileAddress tAddr, ACMapSource mapSource) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void putTileData(byte[] tileData, int x, int y, int zoom, ACMapSource mapSource, long timeLastModified, long timeExpires, String eTag)
	    throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void putTile(IfStoredTile tile, ACMapSource mapSource)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public IfStoredTile getTileEntry(int x, int y, int zoom, ACMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tile getTile(TileAddress tAddr, ACMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(int x, int y, int zoom, ACMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(TileAddress tAddr, ACMapSource mapSource)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public BufferedImage getCacheCoverage(ACMapSource mapSource, int zoom, Point tileNumMin, Point tileNumMax) throws InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}

}
