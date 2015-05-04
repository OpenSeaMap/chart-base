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

public abstract class ACMapSourcesManager
{
	protected static ACMapSourcesManager INSTANCE = null;

	public static ACMapSourcesManager getInstance()
	{
		return INSTANCE;
	}

	public abstract void addMapSource(IfMapSource mapSource);

	public abstract Vector<IfMapSource> getAllMapSources();

	/**
	 * Returns all {@link IfMapSource} used implementations that represent a map layer (have a visible result). Meta-map-sources like multi-layer map sources are
	 * ignored. The result does contain each {@link IfMapSource} only once (no duplicates).
	 * 
	 * @return
	 */
	public abstract Vector<IfMapSource> getAllLayerMapSources();

	public abstract Vector<IfMapSource> getEnabledOrderedMapSources();

	public abstract IfMapSource getDefaultMapSource();

	public abstract IfMapSource getSourceByName(String name);

	public abstract Vector<IfMapSource> getDisabledMapSources();

	/**
	 * All means all visible map sources to the user plus all layers of multi-layer map sources
	 * 
	 * @return
	 */
	public abstract Vector<IfMapSource> getAllAvailableMapSources();

}
