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

/**
 * ACStarter class for starting java apps with some checks.
 * 
 * This class performs the Java Runtime version check and if the correct version is installed it creates a new instance of the class specified by
 * {@link #MAIN_CLASS}. The class to be instantiated is specified by it's name intentionally as this allows to compile this class without any further class
 * dependencies.
 * 
 */
abstract public class ACStarter
{
	/**
	 * Name of the main class, to be superimposed by the actual starter class
	 */
	static final String MAIN_CLASS = "";
	static protected ACApp theApp;

	public static void setLookAndFeel()
	{
		try
		{
			if (System.getProperty("swing.defaultlaf") != null)
				return;
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * not currently maintained, heritage of MobAC
	 */
	protected static void checkJavaVersion()
	{
		String ver = System.getProperty("java.specification.version");
		if (ver == null)
			ver = "Unknown";
		String[] v = ver.split("\\.");
		int major = 0;
		int minor = 0;
		try
		{
			major = Integer.parseInt(v[0]);
			minor = Integer.parseInt(v[1]);
		}
		catch (Exception e)
		{
		}
		int version = (major * 1000) + minor;
		// 1.5 -> 1005; 1.6 -> 1006; 1.7 -> 1007
		if (version < 1006)
		{
			System.exit(1);
		}
	}
}
