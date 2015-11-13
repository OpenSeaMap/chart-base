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
package osmb.program;

import java.io.File;
// import java.util.Properties;

import osmb.utilities.OSMBUtilities;

/**
 * Provides a default initialization of the common directories used within OpenSeaMap ChartBundler and OpenSeaMap ChartDesigner:
 * <ul>
 * <li>current directory</li>
 * <li>program directory</li>
 * <li>user home directory</li>
 * <li>user settings directory</li>
 * <li>temporary directory</li>
 * </ul>
 * 
 * Usually this information is changed when loading settings.xml succeeded. Therefore the program does not use these values directly, but rather by accessing
 * OSMCBSettings.getInstance()...
 */
public class DirectoryManager
{
	public static File currentDir;
	public static File programDir;
	public static File userHomeDir;
	public static File tempDir;
	public static File userAppDataDir;
	public static File userSettingsDir;
	public static File mapSourcesDir;
	public static File toolsDir;
	public static File catalogsDir;
	public static File tileStoreDir;
	public static File bundlesDir;

	// private static Properties dirConfig = null; // /W #unused

	static
	{
		currentDir = new File(System.getProperty("user.dir"));
		userHomeDir = new File(System.getProperty("user.home"));
		programDir = ACApp.getProgramDir();

		userAppDataDir = getUserAppDataDir();
		tempDir = new File(System.getProperty("java.io.tmpdir"));

		toolsDir = new File(programDir, "tools");
		userSettingsDir = programDir;

		// /W standard directories
		catalogsDir = new File(userHomeDir, "catalogs");
		mapSourcesDir = new File(programDir, "../OSeaMChartBase/mapsources"); // /W #???
		tileStoreDir = new File(userHomeDir, "tilestore");
		bundlesDir = new File(userHomeDir, "bundles");
	}

	public static void initialize(File programDir)
	{

		if (currentDir == null || userAppDataDir == null || tempDir == null || programDir == null)
			throw new RuntimeException("DirectoryManager failed");
	}

	/**
	 * unused see {@link ACApp#getProgramDir()}
	 * 
	 * @return
	 *         Returns the directory from which this java program is executed
	 */
	@SuppressWarnings("unused") // /W ACApp.getProgramDir() ...
	private static File getProgramDir()
	{
		File f = null;
		try
		{
			// f = OSMCBUtilities.getClassLocation(DirectoryManager.class);
			f = OSMBUtilities.getClassLocation(DirectoryManager.class);
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
			return currentDir;
		}
		if ("bin".equals(f.getName())) // remove the bin dir -> this usually happens only in a development environment
			return f.getParentFile();
		else
			return f;
	}

	/**
	 * Returns the directory where the application saves it's settings. Should be overridden for any OS the app shall know about
	 * 
	 * Examples:
	 * <ul>
	 * <li>English Windows XP:<br>
	 * <tt>C:\Document and Settings\%username%\Application Data\%appname%</tt>
	 * <li>Vista, W7, W8:<br>
	 * <tt>C:\Users\%username%\Application Data\%appname%</tt>
	 * <li>Linux:<br>
	 * <tt>/home/$username$/.%appname%</tt></li>
	 * </ul>
	 * 
	 * @return
	 */
	private static File getUserAppDataDir()
	{
		String appData = System.getenv("APPDATA");
		if (appData != null)
		{
			File appDataDir = new File(appData);
			if (appDataDir.isDirectory())
			{
				File osmcbDataDir = new File(appData, "OSeaM ChartBase");
				if (!osmcbDataDir.exists() && !osmcbDataDir.mkdir())
					throw new RuntimeException("Unable to create directory \"" + osmcbDataDir.getAbsolutePath() + "\"");
				return osmcbDataDir;
			}
		}
		else
		{
			File userDir = new File(System.getProperty("user.home"));
			File osmcbDataDir = new File(userDir, ".osmb");
			if (!osmcbDataDir.exists() && !osmcbDataDir.mkdir())
				throw new RuntimeException("Unable to create directory \"" + osmcbDataDir.getAbsolutePath() + "\"");
			return osmcbDataDir;
		}
		return null;
	}

	/**
	 * should never be instantiated
	 */
	protected DirectoryManager()
	{
	}
}
