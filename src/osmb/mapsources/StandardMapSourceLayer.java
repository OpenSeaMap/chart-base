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

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import osmb.program.tiles.TileImageType;
import osmb.program.tilestore.ACTileStore;

@XmlRootElement
/**
 * Wraps an already existing map source so that it can be loaded by name in a custom multi-layer map source
 */
public class StandardMapSourceLayer extends ACMapSource
{
	protected ACMapSource mapSource = null;

	@XmlElement(name = "name")
	protected String mapSourceName;

	public ACMapSource getMapSource()
	{
		return mapSource;
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent)
	{
		mapSource = SiACMapSourcesManager.getInstance().getSourceByName(mapSourceName);
		if (mapSource == null)
			throw new RuntimeException("Invalid map source name used: " + mapSourceName);
	}

	@Override
	public int getMaxZoom()
	{
		return mapSource.getMaxZoom();
	}

	@Override
	public int getMinZoom()
	{
		return mapSource.getMinZoom();
	}

	@Override
	public String getName()
	{
		return mapSource.getName();
	}

	@Override
	public byte[] loadTileData(TileAddress tAddr)
	{
		return mapSource.loadTileData(tAddr);
	}

	@Override
	public BufferedImage loadTileImage(TileAddress tAddr)
	{
		return mapSource.loadTileImage(tAddr);
	}

	@Override
	public TileImageType getTileImageType()
	{
		return mapSource.getTileImageType();
	}

	@Override
	public Color getBackgroundColor()
	{
		return mapSource.getBackgroundColor();
	}

	@Override
	public String toString()
	{
		return mapSource.toString();
	}

	@Override
	public int hashCode()
	{
		return mapSource.hashCode();
	}

	@Override
	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo()
	{
		return mapSource.getLoaderInfo();
	}

	@Override
	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo)
	{
		mapSource.setLoaderInfo(loaderInfo);
	}

	@Override
	public boolean equals(Object obj)
	{
		return mapSource.equals(obj);
	}

	@Override
	public void initialize()
	{
		// TODO Auto-generated method stub
	}

	@Override
	public ACTileStore getTileStore()
	{
		return null;
	}

}
