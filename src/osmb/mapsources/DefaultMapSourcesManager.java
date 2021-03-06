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
package osmb.mapsources;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JOptionPane;

import osmb.mapsources.loader.CustomMapSourceLoader;
import osmb.program.ACSettings;
import osmb.utilities.OSMBStrs;

public class DefaultMapSourcesManager extends SiACMapSourcesManager
{

	/**
	 * All map sources visible to the user, independent of it is enabled or disabled
	 */
	private LinkedHashMap<String, ACMapSource> allMapSources = new LinkedHashMap<String, ACMapSource>(50);

	/**
	 * All means all map sources visible to the user plus all layers of multi-layer map sources
	 */
	private HashMap<String, ACMapSource> allAvailableMapSources = new HashMap<String, ACMapSource>(50);

	public DefaultMapSourcesManager()
	{
		// Check for user specific configuration of mapsources directory
		log.trace("constructor called");
	}

	/**
	 * Loads the map sources according to the contents of settings.xml
	 */
	protected void loadMapSources()
	{
		try
		{
			File mapSourcesDir = ACSettings.getInstance().getMapSourcesDirectory();
			if (mapSourcesDir == null)
				throw new RuntimeException("Map sources directory is unset");
			if (!mapSourcesDir.isDirectory())
			{
				JOptionPane.showMessageDialog(null, String.format(OSMBStrs.RStr("msg_environment_mapsrc_dir_not_exist"), mapSourcesDir.getAbsolutePath()),
				    OSMBStrs.RStr("Error"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			try
			{
				// MapPackManager mpm = new MapPackManager(mapSourcesDir);
				// if (!devMode || !loadMapPacksEclipseMode())
				// {
				// mpm.loadMapPacks(this);
				// }
			}
			catch (Exception e)
			{
				throw new RuntimeException("Failed to load map packs: " + e.getMessage(), e);
			}
			// BeanShellMapSourceLoader bsmsl = new BeanShellMapSourceLoader(this, mapSourcesDir);
			// bsmsl.loadBeanShellMapSources();

			CustomMapSourceLoader cmsl = new CustomMapSourceLoader(this, mapSourcesDir);
			cmsl.loadCustomMapSources();
		}
		finally
		{
			// // If no map sources are available load the simple map source which shows the informative message
			// if (allMapSources.size() == 0)
			// addMapSource(new SimpleMapSource());
		}
	}

	// private boolean loadMapPacksEclipseMode()
	// {
	// EclipseMapPackLoader empl;
	// try
	// {
	// empl = new EclipseMapPackLoader(this);
	// if (!empl.loadMapPacks())
	// return false;
	// return true;
	// }
	// catch (IOException e)
	// {
	// log.error("Failed to load map packs directly from classpath");
	// }
	// return false;
	// }

	@Override
	public void addMapSource(ACMapSource mapSource)
	{
		if (mapSource instanceof StandardMapSourceLayer)
			mapSource = ((StandardMapSourceLayer) mapSource).getMapSource();
		allAvailableMapSources.put(mapSource.getName(), mapSource);
		if (mapSource instanceof ACMultiLayerMapSource)
		{
			for (ACMapSource lms : ((ACMultiLayerMapSource) mapSource))
			{
				if (lms instanceof StandardMapSourceLayer)
					lms = ((StandardMapSourceLayer) lms).getMapSource();
				ACMapSource old = allAvailableMapSources.put(lms.getName(), lms);
				if (old != null)
				{
					allAvailableMapSources.put(old.getName(), old);
					if (mapSource.equals(old))
						JOptionPane.showMessageDialog(null, "Error: Duplicate map source name found: " + mapSource.getName(), "Duplicate name", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		allMapSources.put(mapSource.getName(), mapSource);
	}

	public static void initialize()
	{
		INSTANCE = new DefaultMapSourcesManager();
		((DefaultMapSourcesManager) INSTANCE).loadMapSources();
	}

	@Override
	public Vector<ACMapSource> getAllAvailableMapSources()
	{
		return new Vector<ACMapSource>(allMapSources.values());
	}

	@Override
	public Vector<ACMapSource> getAllMapSources()
	{
		return new Vector<ACMapSource>(allMapSources.values());
	}

	@Override
	public Vector<ACMapSource> getAllLayerMapSources()
	{
		Vector<ACMapSource> all = getAllMapSources();
		TreeSet<ACMapSource> uniqueSources = new TreeSet<ACMapSource>(new Comparator<ACMapSource>()
		{

			@Override
			public int compare(ACMapSource o1, ACMapSource o2)
			{
				return o1.getName().compareTo(o2.getName());
			}

		});
		for (ACMapSource ms : all)
		{
			if (ms instanceof ACMultiLayerMapSource)
			{
				for (ACMapSource lms : ((ACMultiLayerMapSource) ms))
				{
					uniqueSources.add(lms);
				}
			}
			else
				uniqueSources.add(ms);
		}
		Vector<ACMapSource> result = new Vector<ACMapSource>(uniqueSources);
		return result;
	}

	@Override
	public Vector<ACMapSource> getEnabledOrderedMapSources()
	{
		Vector<ACMapSource> mapSources = new Vector<ACMapSource>(allMapSources.size());

		Vector<String> enabledMapSources = ACSettings.getInstance().mapSourcesEnabled;
		TreeSet<String> notEnabledMapSources = new TreeSet<String>(allMapSources.keySet());
		notEnabledMapSources.removeAll(enabledMapSources);
		for (String mapSourceName : enabledMapSources)
		{
			ACMapSource ms = getSourceByName(mapSourceName);
			if (ms != null)
			{
				mapSources.add(ms);
			}
		}
		// remove all disabled map sources so we get those that are neither enabled nor disabled
		notEnabledMapSources.removeAll(ACSettings.getInstance().mapSourcesDisabled);
		for (String mapSourceName : notEnabledMapSources)
		{
			ACMapSource ms = getSourceByName(mapSourceName);
			if (ms != null)
			{
				mapSources.add(ms);
			}
		}
		// if (mapSources.size() == 0)
		// mapSources.add(new SimpleMapSource());
		return mapSources;
	}

	@Override
	public Vector<ACMapSource> getDisabledMapSources()
	{
		Vector<String> disabledMapSources = ACSettings.getInstance().mapSourcesDisabled;
		Vector<ACMapSource> mapSources = new Vector<ACMapSource>(0);
		for (String mapSourceName : disabledMapSources)
		{
			ACMapSource ms = getSourceByName(mapSourceName);
			if (ms != null)
			{
				mapSources.add(ms);
			}
		}
		return mapSources;
	}

	@Override
	public ACMapSource getDefaultMapSource()
	{
		ACMapSource ms = getSourceByName("AH OpenSeaMap - Mapnik"); // W #??? DEFAULT;
		if (ms != null)
			return ms;
		// Fallback: return first
		return allMapSources.values().iterator().next();
	}

	@Override
	public ACMapSource getSourceByName(String name)
	{
		return allAvailableMapSources.get(name);
	}
}
