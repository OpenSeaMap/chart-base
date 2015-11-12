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
package osmb.program.map;

import java.awt.Dimension;
import java.awt.Point;
import java.io.StringWriter;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import osmb.exceptions.InvalidNameException;
import osmb.mapsources.IfMapSource;
import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.catalog.IfCapabilityDeletable;
import osmb.program.catalog.IfCatalog;
import osmb.program.tiles.IfTileFilter;
import osmb.program.tiles.TileImageParameters;
import osmb.utilities.geo.EastNorthCoordinate;
import osmb.utilities.image.MercatorPixelCoordinate;

/**
 * The map is the implementation of a rectangular (not really, since the earth is a globe) area on earths surface, which is rendered in square tiles (of 256 x
 * 265) pixels.
 * see {@link IfMapSpace}.
 * For a description of the map space coordinates see {@link MercatorPower2MapSpace}.
 * 
 * @author humbach *
 */
@XmlType(propOrder =
{ "name", "mapSource", "ULC", "LRC", "minTileCoordinate", "maxTileCoordinate", "number" })
public class Map implements IfMap, IfCapabilityDeletable, TreeNode
{
	// class/static data
	protected static Logger log = Logger.getLogger(Map.class);

	// instance data
	/**
	 * the osm internal name
	 */
	protected String name;
	/**
	 * the osm internal number, the numbering scheme is still to be defined
	 * 20150722 AH proposal: ZL-LON-LAT-WID-HEI, LON and LAT in tiles/8 since this is our map alignment grid, WID, HEI in tiles.
	 */
	protected String number;
	/**
	 * the INT conformant name - if there is one; for a lot of maps this is empty
	 */
	protected String intName = null;
	/**
	 * the INT conformant number - if there is one; for a lot of maps this is empty
	 */
	protected String intNumber = null;
	/**
	 * the INT national name - if there is one; for a lot of maps this is empty
	 */
	protected String natName = null;
	protected Point maxTileCoordinate = null;
	protected Point minTileCoordinate = null;
	@XmlAttribute
	protected IfMapSource mapSource = null;
	// protected int zoom;
	protected Layer layer;
	protected TileImageParameters parameters = null;
	protected Dimension tileDimension = null;

	protected Map()
	{
	}

	protected Map(Map map)
	{
		name = map.name;
	}

	protected Map(Layer layer, String name, IfMapSource mapSource, int zoom, Point minTileCoordinate, Point maxTileCoordinate, TileImageParameters parameters)
	{
		this.layer = layer;
		this.maxTileCoordinate = maxTileCoordinate;
		this.minTileCoordinate = minTileCoordinate;
		this.name = name;
		this.mapSource = mapSource;
		// this.zoom = zoom;
		this.parameters = parameters;
		calculateRuntimeValues();
		// 20150722 AH fixed numbers in here
		String mapNumber = zoom + "-" + minTileCoordinate.y / (tileDimension.height * 8) + "-" + minTileCoordinate.x / (tileDimension.width * 8) + "-"
		    + ((maxTileCoordinate.y - minTileCoordinate.y + 1) / tileDimension.height) + "-"
		    + ((maxTileCoordinate.x - minTileCoordinate.x + 1) / tileDimension.width);
		log.trace("new map: '" + mapNumber + "'");
		this.number = mapNumber;
	}

	/**
	 * This actually get only the value for {@link #tileDimension}, which currently (2015) is fixed at 256 by 256 pixels
	 */
	protected void calculateRuntimeValues()
	{
		if (mapSource == null)
			throw new RuntimeException("The map source of map '" + name + "' is unknown to OSMCB");
		if (parameters == null)
		{
			int tileSize = mapSource.getMapSpace().getTileSize();
			tileDimension = new Dimension(tileSize, tileSize);
		}
		else
			tileDimension = parameters.getDimension();
	}

	@Override
	@XmlTransient
	public IfLayer getLayer()
	{
		return layer;
	}

	@Override
	public void setLayer(IfLayer layer)
	{
		this.layer = (Layer) layer;
	}

	@Override
	public IfMapSource getMapSource()
	{
		return this.mapSource;
	}

	@Override
	/**
	 * @XmlJavaTypeAdapter(PointAdapter.class) annotation in package-info.java
	 */
	@XmlAttribute
	public Point getMaxTileCoordinate()
	{
		return this.maxTileCoordinate;
	}

	@Override
	/**
	 * The tile coordinate value is stored in map space coordinates
	 * 
	 * @XmlJavaTypeAdapter(PointAdapter.class) annotation in package-info.java
	 */
	@XmlAttribute
	public Point getMinTileCoordinate()
	{
		return this.minTileCoordinate;
	}

	/**
	 * The tile coordinate value is stored in map space coordinates (pixel coordiantes)
	 */
	@Override
	public void setMaxTileCoordinate(Point MaxC)
	{
		this.maxTileCoordinate = MaxC;
	}

	/**
	 * The tile coordinate value is stored in map space coordinates (pixel coordiantes)
	 */
	@Override
	public void setMinTileCoordinate(Point MinC)
	{
		this.minTileCoordinate = MinC;
	}

	@XmlAttribute
	public String getULC()
	{
		return new EastNorthCoordinate(mapSource.getMapSpace(), getZoom(), minTileCoordinate.x, minTileCoordinate.y).toCatalog();// /W max->min
	}

	public void setULC(String strULC)
	{
		;
	}

	@XmlAttribute
	public String getLRC()
	{
		return new EastNorthCoordinate(mapSource.getMapSpace(), getZoom(), maxTileCoordinate.x + 1, maxTileCoordinate.y + 1).toCatalog();// /W min->max
	}

	public void setLRC(String strLRC)
	{
	}

	/**
	 * XMin is west
	 */
	@Override
	public int getXMin()
	{
		return minTileCoordinate.x / IfMapSpace.TECH_TILESIZE;
	}

	/**
	 * XMax is east
	 */
	@Override
	public int getXMax()
	{
		return maxTileCoordinate.x / IfMapSpace.TECH_TILESIZE;
	}

	/**
	 * YMin is north
	 */
	@Override
	public int getYMin()
	{
		return minTileCoordinate.y / IfMapSpace.TECH_TILESIZE;
	}

	/**
	 * YMax is south
	 */
	@Override
	public int getYMax()
	{
		return maxTileCoordinate.y / IfMapSpace.TECH_TILESIZE;
	}

	@Override
	@XmlAttribute
	public String getName()
	{
		return name;
	}

	@Override
	@XmlAttribute
	public String getNumber()
	{
		return number;
	}

	public void setNumber(String strNum)
	{
		number = strNum;
	}

	@Override
	public int getZoom()
	{
		return layer.getZoomLvl();
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	@XmlTransient
	// /W #??? nur nicht initialisiert! soll aber rausfliegen!
	public TileImageParameters getParameters()
	{
		return parameters;
	}

	@Override
	public void setParameters(TileImageParameters parameters)
	{
		this.parameters = parameters;
	}

	@Override
	public String getInfoText()
	{
		return "Map\n name=" + name + "\n mapSource=" + mapSource + "\n zoom=" + getZoom() + "\n maxTileCoordinate=" + maxTileCoordinate.x + "/"
		    + maxTileCoordinate.y + "\n minTileCoordinate=" + minTileCoordinate.x + "/" + minTileCoordinate.y + "\n parameters=" + parameters;
	}

	public String getToolTip()
	{
		IfMapSpace mapSpace = mapSource.getMapSpace();
		@SuppressWarnings("unused") // /W #unused
		EastNorthCoordinate tl = new EastNorthCoordinate(mapSpace, getZoom(), minTileCoordinate.x, minTileCoordinate.y);
		@SuppressWarnings("unused") // /W #unused
		EastNorthCoordinate br = new EastNorthCoordinate(mapSpace, getZoom(), maxTileCoordinate.x, maxTileCoordinate.y);

		StringWriter sw = new StringWriter(1024);
		// sw.write("<html>");
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_map_title"));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_source",
		// StringEscapeUtils.escapeHtml4(mapSource.toString()),
		// StringEscapeUtils.escapeHtml4(mapSource.getName())));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_zoom_lv",
		// zoom));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_area_start",
		// tl.toString(), minTileCoordinate.x, minTileCoordinate.y));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_area_end",
		// br.toString(), maxTileCoordinate.x, maxTileCoordinate.y));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_size",
		// (maxTileCoordinate.x - minTileCoordinate.x + 1), (maxTileCoordinate.y
		// - minTileCoordinate.y + 1)));
		// if (parameters != null)
		// {
		// sw.write(String.format(OSMCBStrs.RStr("lp_bundle_info_tile_size"),
		// parameters.getWidth(), parameters.getHeight()));
		// sw.write(String.format(OSMCBStrs.RStr("lp_bundle_info_tile_format"),
		// parameters.getFormat().toString()));
		// }
		// else
		// {
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_tile_format_origin"));
		// }
		//
		// sw.write(String.format(OSMCBStrs.RStr("lp_bundle_info_max_tile"),
		// calculateTilesToDownload()));
		// sw.write("</html>");
		return sw.toString();
	}

	@Override
	public Dimension getTileSize()
	{
		return tileDimension;
	}

	/////////////////////////////////////////////////////////////////////////////////
	// /W mapSource.getMaxZoom() or JMapViewer.MAX_ZOOM = 22 ???????

	protected MercatorPixelCoordinate ulBordersToMaxZoom()
	{
		MercatorPixelCoordinate borderCoord = new MercatorPixelCoordinate(mapSource.getMapSpace(), minTileCoordinate.x, minTileCoordinate.y, getZoom());
		borderCoord = borderCoord.adaptToZoomlevel(mapSource.getMaxZoom());
		return borderCoord;
	}

	protected MercatorPixelCoordinate lrBordersToMaxZoom()
	{
		MercatorPixelCoordinate borderCoord = new MercatorPixelCoordinate(mapSource.getMapSpace(), maxTileCoordinate.x + 1, maxTileCoordinate.y + 1, getZoom());
		borderCoord = borderCoord.adaptToZoomlevel(mapSource.getMaxZoom());
		return borderCoord;
	}

	@Override
	public int getXBorderMin()
	{
		return ulBordersToMaxZoom().getX();
	}

	@Override
	public int getXBorderMax()
	{
		// return minTileCoordinate.y;
		return lrBordersToMaxZoom().getX();
	}

	@Override
	public int getYBorderMin()
	{
		// return maxTileCoordinate.x;
		return ulBordersToMaxZoom().getY();
	}

	@Override
	public int getYBorderMax()
	{
		return lrBordersToMaxZoom().getY();
	}
	/////////////////////////////////////////////////////////////////////////////////

	@Override
	public double getMinLat()
	{
		return mapSource.getMapSpace().cYToLat(maxTileCoordinate.y, getZoom());
	}

	@Override
	public double getMaxLat()
	{
		return mapSource.getMapSpace().cYToLat(minTileCoordinate.y, getZoom());
	}

	@Override
	public double getMinLon()
	{
		return mapSource.getMapSpace().cXToLon(minTileCoordinate.x, getZoom());
	}

	@Override
	public double getMaxLon()
	{
		return mapSource.getMapSpace().cXToLon(maxTileCoordinate.x, getZoom());
	}

	@Override
	public void delete()
	{
		layer.deleteMap(this);
	}

	@Override
	public void setName(String newName) throws InvalidNameException
	{
		if (layer != null)
		{
			for (IfMap map : layer)
			{
				if ((map != this) && (newName.equals(map.getName())))
					throw new InvalidNameException("There is already a map named \"" + newName + "\" in this layer.\nMap names have to be unique within a layer.");
			}
		}
		this.name = newName;
	}

	@Override
	public Enumeration<?> children()
	{
		return null;
	}

	@Override
	public boolean getAllowsChildren()
	{
		return false;
	}

	@Override
	public TreeNode getChildAt(int childIndex)
	{
		return null;
	}

	@Override
	public int getChildCount()
	{
		return 0;
	}

	@Override
	public int getIndex(TreeNode node)
	{
		return 0;
	}

	@Override
	public TreeNode getParent()
	{
		return layer;
	}

	@Override
	public boolean isLeaf()
	{
		return true;
	}

	/**
	 * This simply calculates all tiles included in the map.
	 */
	@Override
	public long getTileCount()
	{
		long tiles = (maxTileCoordinate.x - minTileCoordinate.x + 1) * (maxTileCoordinate.y - minTileCoordinate.y + 1) // /W + 1, + 1
		    / (mapSource.getMapSpace().getTileSize() * mapSource.getMapSpace().getTileSize());
		return tiles;
	}

	/**
	 * This simply calculates all tiles included in the map. It currently (2015-08) does not take in account that tiles are shared by several maps.
	 */
	@Override
	public long calculateTilesToDownload()
	{
		// long tiles = (maxTileCoordinate.x - minTileCoordinate.x) * (maxTileCoordinate.y - minTileCoordinate.y)
		// / (mapSource.getMapSpace().getTileSize() * mapSource.getMapSpace().getTileSize());
		long tiles = getTileCount();
		// TODO correct tile count in case of multi-layer maps
		// if (mapSource instanceof MultiLayerMapSource) {
		// // We have a map with two layers and for each layer we have to
		// // download the tiles - therefore double the tileCount
		// tileCount *= 2;
		// }
		log.trace("map=" + getName() + ", tiles=" + tiles);
		return tiles;
	}

	@Override
	public boolean isInvalid()
	{
		boolean result = false;
		boolean[] checks =
		{ name == null, // 0
		    layer == null, // 1
		    maxTileCoordinate == null, // 2
		    minTileCoordinate == null, // 3
		    mapSource == null, // 4
		    getZoom() < 0 // 5
		};

		for (int i = 0; i < checks.length; i++)
			if (checks[i])
			{
				log.error("Problem detectected with map \"" + name + "\" check: " + i);
				result = true;
			}
		// Automatically correct bad ordered min/max coordinates
		try
		{
			if (minTileCoordinate.x > maxTileCoordinate.x)
			{
				int tmp = maxTileCoordinate.x;
				maxTileCoordinate.x = minTileCoordinate.x;
				minTileCoordinate.x = tmp;
			}
			if (minTileCoordinate.y > maxTileCoordinate.y)
			{
				int tmp = maxTileCoordinate.y;
				maxTileCoordinate.y = minTileCoordinate.y;
				minTileCoordinate.y = tmp;
			}
		}
		catch (Exception e)
		{
		}
		return result;
	}

	@Override
	public IfMap deepClone(IfLayer newLayer)
	{
		try
		{
			Map map = this.getClass().newInstance();
			map.layer = (Layer) newLayer;
			map.mapSource = mapSource;
			map.maxTileCoordinate = (Point) maxTileCoordinate.clone();
			map.minTileCoordinate = (Point) minTileCoordinate.clone();
			map.name = name;
			if (parameters != null)
				map.parameters = (TileImageParameters) parameters.clone();
			else
				map.parameters = null;
			map.tileDimension = (Dimension) tileDimension.clone();
			return map;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public void afterUnmarshal(Unmarshaller u, Object parent)
	{
		this.layer = (Layer) parent;
		calculateRuntimeValues();
	}

	@Override
	public IfTileFilter getTileFilter()
	{
		// return new DummyTileFilter();
		return null;
	}

	@Override
	public IfCatalog getCatalog()
	{
		return this.layer.getCatalog();
	}

	// @Override
	// public Enumeration<Job> getDownloadJobs(TarIndexedArchive tileArchive,
	// IfDownloadJobListener listener)
	// {
	// return new DownloadJobEnumerator(this, mapSource, tileArchive, listener);
	// }
}
