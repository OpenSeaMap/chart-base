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
import java.io.IOException;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

//W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;

@XmlRootElement
/**
 * Wraps an already existing map source so that it can be loaded by name in a custom multi-layer map source
 */
public class StandardMapSourceLayer implements IfMapSource
{
	protected IfMapSource mapSource = null;

	@XmlElement(name = "name")
	protected String mapSourceName;

	public IfMapSource getMapSource()
	{
		return mapSource;
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent)
	{
		mapSource = ACMapSourcesManager.getInstance().getSourceByName(mapSourceName);
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
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException
	{
		return mapSource.getTileData(zoom, x, y, loadMethod);
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException
	{
		return mapSource.getTileImage(zoom, x, y, loadMethod);
	}

	@Override
	public BufferedImage downloadTileImage(int zoom, int x, int y) throws IOException, TileException, InterruptedException
	{
		return mapSource.downloadTileImage(zoom, x, y);
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
}
