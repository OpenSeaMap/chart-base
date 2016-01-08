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
package osmb.program.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import osmb.mapsources.ACMapSourcesManager;
import osmb.mapsources.IfMapSource;

public class MapSourceAdapter extends XmlAdapter<String, IfMapSource>
{
	@Override
	public String marshal(IfMapSource mapSource) throws Exception
	{
		return mapSource.getName();
	}

	@Override
	public IfMapSource unmarshal(String name) throws Exception
	{
		return ACMapSourcesManager.getInstance().getSourceByName(name);
	}
}
