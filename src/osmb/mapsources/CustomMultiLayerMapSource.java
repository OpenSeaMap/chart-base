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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import osmb.program.tiles.TileImageType;

/**
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlSeeAlso(
{ CustomOnlineMapSource.class })
public class CustomMultiLayerMapSource extends ACMultiLayerMapSource
{
	@XmlElementWrapper(name = "layers")
	// @XmlElements(
	// { @XmlElement(name = "customOnlineMapSource", type = CustomOnlineMapSource.class), @XmlElement(name = "mapSource", type = StandardMapSourceLayer.class),
	// @XmlElement(name = "localTileSQLite", type = CustomLocalTileSQliteMapSource.class),
	// @XmlElement(name = "localTileFiles", type = CustomLocalTileFilesMapSource.class),
	// @XmlElement(name = "localTileZip", type = CustomLocalTileZipMapSource.class),
	// @XmlElement(name = "localImageFile", type = CustomLocalImageFileMapSource.class) })
	@XmlElements(
	{ @XmlElement(name = "customMapSource", type = CustomOnlineMapSource.class) })
	protected List<CustomOnlineMapSource> layers = new ArrayList<CustomOnlineMapSource>();

	@XmlList()
	protected List<Float> layersAlpha = new ArrayList<Float>();

	public CustomMultiLayerMapSource()
	{
		super();
		mapSources = new ACMapSource[0];
	}

	public TileImageType getTileType()
	{
		return mTileType;
	}

	public void setTileType(TileImageType tileType)
	{
		this.mTileType = tileType;
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent)
	{
		mapSources = new ACMapSource[layers.size()];
		layers.toArray(mapSources);
		// initializeValues();
		initialize();
	}

	@XmlElement(name = "name")
	public String getMLName()
	{
		return mName;
	}

	public void setMLName(String name)
	{
		this.mName = name;
	}

	@Override
	protected float getLayerAlpha(int layerIndex)
	{
		if (layersAlpha.size() <= layerIndex)
			return 1.0f;
		else
			return layersAlpha.get(layerIndex);
	}
}
