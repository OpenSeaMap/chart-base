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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.tree.TreeNode;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
///W ? import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;

import osmb.exceptions.InvalidNameException;
import osmb.mapsources.IfMapSource;
import osmb.program.catalog.Catalog;
import osmb.program.catalog.IfCapabilityDeletable;
import osmb.program.catalog.IfCatalog;
import osmb.program.tiles.TileImageParameters;
import osmb.utilities.geo.EastNorthCoordinate;

/**
 * A layer holding one or multiple maps of the same map source and the same zoom level. The number of maps depends on the size of the covered area - if it is
 * smaller than the specified <code>maxMapSize</code> then there will be only one map.
 * 
 * 20140128 Ah zoom level introduced as property of layer
 * 
 */
// /W ? @XmlRootElement
public class Layer implements IfLayer, IfCapabilityDeletable
{
	// static/class data
	private static Logger log = Logger.getLogger(Layer.class);

	static public Layer GetLayerByZoom(IfCatalog catalog, int zoom)
	{
		if (catalog != null)
		{
			for (IfLayer layer : catalog)
			{
				if (layer.getZoomLvl() == zoom)
					return (Layer) layer;
			}
		}
		return null;
	}

	// instance data
	@XmlTransient
	private IfCatalog mCatalog;

	private String name;
	private int nZoomLvl;

	@XmlElements(
	{ @XmlElement(name = "PolygonMap", type = MapPolygon.class), @XmlElement(name = "Map", type = Map.class) })
	private LinkedList<IfMap> maps = new LinkedList<IfMap>();

	protected Layer()
	{
	}

	public Layer(IfCatalog catalog, String name, int zoom) throws InvalidNameException
	{
		this.mCatalog = catalog;
		setName(name);
		nZoomLvl = zoom;
	}

	@Override
	public void addMapsAutocut(String mapNameBase, IfMapSource ms, EastNorthCoordinate minCoordinate, EastNorthCoordinate maxCoordinate, int zoom,
			TileImageParameters parameters, int maxMapSize) throws InvalidNameException
	{
		IfMapSpace mapSpace = ms.getMapSpace();
		addMapsAutocut(mapNameBase, ms, minCoordinate.toTileCoordinate(mapSpace, zoom), maxCoordinate.toTileCoordinate(mapSpace, zoom), zoom, parameters,
				maxMapSize, 0);
	}

	/**
	 * addMapsAutocut() checks if the new map is already completely covered in another map or other way round
	 */
	@Override
	public void addMapsAutocut(String mapNameBase, IfMapSource mapSource, Point minTileCoordinate, Point maxTileCoordinate, int zoom,
			TileImageParameters parameters, int maxMapSize, int overlapTiles) throws InvalidNameException
	{
		log.trace("Adding new map(s): \"" + mapNameBase + "\" " + mapSource + " zoom=" + zoom + " min=" + minTileCoordinate.x + "/" + minTileCoordinate.y + " max="
				+ maxTileCoordinate.x + "/" + maxTileCoordinate.y);

		// if no zoom level yet, set it, else check if it is correct
		if (nZoomLvl == -1)
			nZoomLvl = zoom;
		if (zoom == nZoomLvl)
		{
			int tileSize = mapSource.getMapSpace().getTileSize();
			int nXSize = (maxTileCoordinate.x - minTileCoordinate.x) / tileSize + 1;
			int nYSize = (maxTileCoordinate.y - minTileCoordinate.y) / tileSize + 1;
			int nXExp = 1, nYExp = 1;

			// get requested size in 2^n grid
			while ((nXSize >>= 1) >= 1)
				++nXExp;
			while ((nYSize >>= 1) >= 1)
				++nYExp;

			// fit into encouraged map grid widths (8, 16, 32, 64, 128 tiles)
			int nXGridSize = 0, nYGridSize = 0;
			nXExp = Math.max(3, Math.min(7, nXExp));// /W == 3 ? min <-> max vertauscht
			nXExp = Math.min(zoom, nXExp);// /W sonst Problem bei zoom 0, 1, 2
			nXGridSize = tileSize << nXExp;
			nYExp = Math.max(3, Math.min(7, nYExp));// /W dto
			nYExp = Math.min(zoom, nYExp);// /W dto
			nYGridSize = tileSize << nYExp;

			// align left/top with map grid
			int nXOff = 0, nYOff = 0;
			nXOff = minTileCoordinate.x % (nXGridSize / 2);
			minTileCoordinate.x -= (nXOff > (nXGridSize / 4) ? nXOff : nXOff + nXGridSize / 2);
			nYOff = minTileCoordinate.y % (nYGridSize / 2);
			minTileCoordinate.y -= (nYOff > (nYGridSize / 4) ? nYOff : nYOff + nYGridSize / 2);

			// align right/bottom with map grid
			nXOff = nXGridSize / 2 + tileSize * overlapTiles - maxTileCoordinate.x % (nXGridSize / 2) - 1;// /W -1 statt + 1 -> Werte in catalog ok
			maxTileCoordinate.x += (nXOff > (nXGridSize / 4) ? nXOff : nXOff + nXGridSize / 2);
			nYOff = nYGridSize / 2 + tileSize * overlapTiles - maxTileCoordinate.y % (nYGridSize / 2) - 1;// /W dto
			maxTileCoordinate.y += (nYOff > (nYGridSize / 4) ? nYOff : nYOff + nYGridSize / 2);

			// if the user set parameters we use them
			Dimension tileDimension;
			if (parameters == null)
				tileDimension = new Dimension(tileSize, tileSize);
			else
				tileDimension = parameters.getDimension();

			// We adapt the max map size to the tile size so that we do not get ugly cut/incomplete tiles at the borders
			Dimension maxMapDimension = new Dimension(maxMapSize, maxMapSize);
			maxMapDimension.width -= maxMapSize % tileDimension.width;
			maxMapDimension.height -= maxMapSize % tileDimension.height;

			log.trace("Adding new map(s) after alignment: \"" + mapNameBase + "\" " + mapSource + " zoom=" + zoom + " min=" + minTileCoordinate.x + "/"
					+ minTileCoordinate.y + " max=" + maxTileCoordinate.x + "/" + maxTileCoordinate.y);
			// is the new map an extension of an already existing map

			// does the map fit the allowed size or has it be cut into several maps
			int mapWidth = maxTileCoordinate.x - minTileCoordinate.x;
			int mapHeight = maxTileCoordinate.y - minTileCoordinate.y;
			if ((mapWidth < maxMapDimension.width) && (mapHeight < maxMapDimension.height))
			{
				// check if this map is not a sub/superset of another already existing map
				if (!CheckMapIsExtension(minTileCoordinate, maxTileCoordinate))
				{
					if (CheckMapArea(minTileCoordinate, maxTileCoordinate))
					{
						// String mapName = String.format(mapNameFormat, new Object[] {mapNameBase, mapCounter++});
						String mapName = MakeValidMapName(mapNameBase, "0000");
						Map s = new Map(this, mapName, mapSource, zoom, minTileCoordinate, maxTileCoordinate, parameters);
						maps.add(s);
					}
				}
			}
			else
			{
				Dimension nextMapStep = new Dimension(maxMapDimension.width - (tileDimension.width * overlapTiles), maxMapDimension.height
						- (tileDimension.height * overlapTiles));

				for (int mapX = minTileCoordinate.x; mapX < maxTileCoordinate.x; mapX += nextMapStep.width)
				{
					for (int mapY = minTileCoordinate.y; mapY < maxTileCoordinate.y; mapY += nextMapStep.height)
					{
						int maxX = Math.min(mapX + maxMapDimension.width, maxTileCoordinate.x);
						int maxY = Math.min(mapY + maxMapDimension.height, maxTileCoordinate.y);
						Point min = new Point(mapX, mapY);
						Point max = new Point(maxX - 1, maxY - 1);
						// check if this map is not a sub/superset of another already existing map
						if (!CheckMapIsExtension(min, max))
						{
							if (CheckMapArea(min, max))
							{
								// String mapName = String.format(mapNameFormat, new Object[] {mapNameBase, mapCounter++});
								String mapName = MakeValidMapName(mapNameBase, "0000");
								Map s = new Map(this, mapName, mapSource, zoom, min, max, parameters);
								maps.add(s);
							}
						}
					}
				}
			}
		}
	}

	public String MakeValidMapName(String mapName, String mapNum)
	{
		String newMapName = mapName + "-" + mapNum;
		int c = 1;
		for (int mapNr = 0; mapNr < getMapCount(); ++mapNr)
		{
			if (newMapName.compareTo(getMap(mapNr).getName()) == 0)
			{
				newMapName = String.format("%s-%04d", mapName, c++);
				mapNr = 0;
				continue;
			}
		}
		return newMapName;
	}

	/**
	 * checks if the new map from MinC to MaxC is not covered by an already existing map
	 * 
	 * @param MinC
	 *          minimum coordinate (upper left corner, NW-C)
	 * @param MaxC
	 *          maximum coordinate (lower right corner, SE-C)
	 * @return true if it truly is new map
	 */
	public boolean CheckMapArea(Point MinC, Point MaxC)
	{
		boolean bSub = true;
		for (int mapNr = 0; mapNr < getMapCount(); ++mapNr)
		{
			IfMap map = getMap(mapNr);
			if ((map.getMinTileCoordinate().x <= MinC.getX()) && (map.getMinTileCoordinate().y <= MinC.getY()))
			{
				if ((map.getMaxTileCoordinate().x >= MaxC.getX()) && (map.getMaxTileCoordinate().y >= MaxC.getY()))
				{
					bSub = false;
					break;
				}
			}
			if ((map.getMinTileCoordinate().x >= MinC.getX()) && (map.getMinTileCoordinate().y >= MinC.getY()))
			{
				if ((map.getMaxTileCoordinate().x <= MaxC.getX()) && (map.getMaxTileCoordinate().y <= MaxC.getY()))
				{
					map.delete();
					--mapNr;
				}
			}

		}
		return bSub;
	}

	/**
	 * checks if the new map is an extension of an already existing map. If it is, the existing map will be changed to new coordinates which include the new
	 * map. 20140511 case new map lies between two already existing maps is not covered yet. The new map will be extending both
	 * 
	 * @param MinC
	 *          minimum coordinate (upper left corner, NW-C)
	 * @param MaxC
	 *          maximum coordinate (lower right corner, SE-C)
	 * @return true if the new map is an extension to an existing map
	 */
	public boolean CheckMapIsExtension(Point MinC, Point MaxC)
	{
		boolean bIsExt = false;
		for (int mapNr = 0; mapNr < getMapCount(); ++mapNr)
		{
			IfMap map = getMap(mapNr);
			if ((map.getMinTileCoordinate().y == MinC.getY()) && (map.getMaxTileCoordinate().y == MaxC.getY()))
			{
				if ((map.getMinTileCoordinate().x >= MinC.getX()) && (map.getMinTileCoordinate().x <= MaxC.getX()))
				{
					map.setMinTileCoordinate(MinC);
					bIsExt = true;
				}
				if ((map.getMaxTileCoordinate().x <= MaxC.getX()) && (map.getMaxTileCoordinate().x >= MinC.getX()))
				{
					map.setMaxTileCoordinate(MaxC);
					bIsExt = true;
				}
			}
			if ((map.getMinTileCoordinate().x == MinC.getX()) && (map.getMaxTileCoordinate().x == MaxC.getX()))
			{
				if ((map.getMinTileCoordinate().y >= MinC.getY()) && (map.getMinTileCoordinate().y <= MaxC.getY()))
				{
					map.setMinTileCoordinate(MinC);
					bIsExt = true;
				}
				if ((map.getMaxTileCoordinate().y <= MaxC.getY()) && (map.getMaxTileCoordinate().y >= MinC.getY()))
				{
					map.setMaxTileCoordinate(MaxC);
					bIsExt = true;
				}
			}
		}
		return bIsExt;
	}

	@Override
	public void delete()
	{
		maps.clear();
		mCatalog.deleteLayer(this);
	}

	@Override
	public IfCatalog getCatalog()
	{
		return mCatalog;
	}

	@Override
	public void addMap(IfMap map)
	{
		// TODO: Add name collision check
		maps.add(map);
		map.setLayer(this);
	}

	@Override
	public IfMap getMap(int index)
	{
		return maps.get(index);
	}

	@Override
	public int getMapCount()
	{
		return maps.size();
	}

	@Override
	@XmlAttribute
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String newName) throws InvalidNameException
	{
		if (mCatalog != null)
		{
			for (IfLayer layer : mCatalog)
			{
				if ((layer != this) && newName.equals(layer.getName()))
					throw new InvalidNameException("There is already a layer named \"" + newName + "\" in this catalog.\nLayer names have to be unique within a catalog.");
			}
		}
		this.name = newName;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public long calculateTilesToDownload()
	{
		long result = 0;
		for (IfMap map : maps)
			result += map.calculateTilesToDownload();
		return result;
	}

	@Override
	public double getMinLat()
	{
		double lat = 90d;
		for (IfMap m : maps)
		{
			lat = Math.min(lat, m.getMinLat());
		}
		return lat;
	}

	@Override
	public double getMaxLat()
	{
		double lat = -90d;
		for (IfMap m : maps)
		{
			lat = Math.max(lat, m.getMaxLat());
		}
		return lat;
	}

	@Override
	public double getMinLon()
	{
		double lon = 180d;
		for (IfMap m : maps)
		{
			lon = Math.min(lon, m.getMinLon());
		}
		return lon;
	}

	@Override
	public double getMaxLon()
	{
		double lon = -180d;
		for (IfMap m : maps)
		{
			lon = Math.max(lon, m.getMaxLon());
		}
		return lon;
	}

	public String getToolTip()
	{
		StringWriter sw = new StringWriter(1024);
		// sw.write("<html>");
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_layer_title"));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_layer_map_count", maps.size()));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_max_tile", calculateTilesToDownload()));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_area_start", OSMCBUtilities.prettyPrintLatLon(getMaxLat(), true),
		// OSMCBUtilities.prettyPrintLatLon(getMinLon(), false)));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_area_end", OSMCBUtilities.prettyPrintLatLon(getMinLat(), true),
		// OSMCBUtilities.prettyPrintLatLon(getMaxLon(), false)));
		// sw.write("</html>");
		return sw.toString();
	}

	@Override
	public Iterator<IfMap> iterator()
	{
		return maps.iterator();
	}

	@Override
	public Enumeration<?> children()
	{
		return Collections.enumeration(maps);
	}

	@Override
	public boolean getAllowsChildren()
	{
		return true;
	}

	@Override
	public TreeNode getChildAt(int childIndex)
	{
		return (TreeNode) maps.get(childIndex);
	}

	@Override
	public int getChildCount()
	{
		return maps.size();
	}

	@Override
	public int getIndex(TreeNode node)
	{
		return maps.indexOf(node);
	}

	@Override
	public TreeNode getParent()
	{
		return (TreeNode) mCatalog;
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	public void afterUnmarshal(Unmarshaller u, Object parent)
	{
		this.mCatalog = (Catalog) parent;
	}

	@Override
	public boolean isInvalid()
	{
		if (mCatalog == null)
			return true;
		if (name == null)
			return true;
		// Check for duplicate map names
		HashSet<String> names = new HashSet<String>(maps.size());
		for (IfMap map : maps)
			names.add(map.getName());
		if (names.size() < maps.size())
			return true; // at least one duplicate name found
		return false;
	}

	public void deleteMap(Map map)
	{
		maps.remove(map);
	}

	@Override
	public IfLayer deepClone(IfCatalog catalog)
	{
		Layer layer = new Layer();
		layer.mCatalog = catalog;
		layer.name = name;
		layer.setZoomLvl(getZoomLvl());
		for (IfMap map : maps)
			layer.maps.add(map.deepClone(layer));
		return layer;
	}

	@Override
	public int getZoomLvl()
	{
		return nZoomLvl;
	}

	@Override
	public void setZoomLvl(int nZoom)
	{
		nZoomLvl = nZoom;
	}

}
