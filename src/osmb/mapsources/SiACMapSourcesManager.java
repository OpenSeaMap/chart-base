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

import java.util.Vector;

import org.apache.log4j.Logger;

public abstract class SiACMapSourcesManager
{
	protected static Logger log = Logger.getLogger(DefaultMapSourcesManager.class);
	protected static SiACMapSourcesManager INSTANCE = null;

	public static SiACMapSourcesManager getInstance()
	{
		return INSTANCE;
	}

	protected SiACMapSourcesManager()
	{
		// adjust logger to the actual implementation
		log = Logger.getLogger(this.getClass());
	}

	public abstract void addMapSource(ACMapSource mapSource);

	public abstract Vector<ACMapSource> getAllMapSources();

	/**
	 * Returns all {@link IfMapSource} used implementations that represent a map layer (have a visible result). Meta-map-sources like multi-layer map sources are
	 * ignored. The result does contain each {@link ACMapSource} only once (no duplicates).
	 * 
	 * @return
	 */
	public abstract Vector<ACMapSource> getAllLayerMapSources();

	public abstract Vector<ACMapSource> getEnabledOrderedMapSources();

	public abstract ACMapSource getDefaultMapSource();

	public abstract ACMapSource getSourceByName(String name);

	public abstract Vector<ACMapSource> getDisabledMapSources();

	/**
	 * All means in this case: All map sources to the user visible plus all layers of all multi-layer map sources
	 * 
	 * @return List of map sources.
	 */
	public abstract Vector<ACMapSource> getAllAvailableMapSources();
}
