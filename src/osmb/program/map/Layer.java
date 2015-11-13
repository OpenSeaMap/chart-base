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

import org.apache.log4j.Logger;

import osmb.exceptions.InvalidNameException;
import osmb.mapsources.IfFileBasedMapSource;
import osmb.mapsources.IfMapSource;
import osmb.program.catalog.Catalog;
import osmb.program.catalog.IfCapabilityDeletable;
import osmb.program.catalog.IfCatalog;
import osmb.program.tiles.TileImageParameters;
import osmb.utilities.geo.EastNorthCoordinate;

/**
 * A layer holding one or multiple maps of the same map source and the same zoom level.
 * The number of maps depends on the size of the covered area - if it is smaller than
 * the specified <code>maxMapSize</code> then there will be only one map.
 * 
 * 20140128 Ah zoom level introduced as property of layer
 * 
 */
// /W ? @XmlRootElement
public class Layer implements IfLayer, IfCapabilityDeletable
{
	// static/class data
	private static Logger log = Logger.getLogger(Layer.class);

	/**
	 * A description of a map containing the coordinates of the upper-left and bottom-right pixels,
	 * the map source, the zoom level and the base to build a map name.
	 */
	private class MapDescription
	{
		private String name;
		private int nZoomLvl;
		Point minPixelC;
		Point maxPixelC;
		IfMapSource mapSource;
	}

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
	private IfCatalog mCatalog;

	private String name;
	private int nZoomLvl;

	@XmlElements(
	{ @XmlElement(name = "PolygonMap", type = MapPolygon.class), @XmlElement(name = "Map", type = Map.class) })
	private LinkedList<IfMap> maps = new LinkedList<IfMap>();

	/**
	 * @return the maps list
	 */
	@Override
	public LinkedList<IfMap> getMaps()
	{
		return maps;
	}

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
		addMapsAutocut(mapNameBase, ms, minCoordinate.toPixelCoordinate(mapSpace, zoom), maxCoordinate.toPixelCoordinate(mapSpace, zoom), zoom, parameters,
		    maxMapSize, 0);
	}

	/**
	 * addMapsAutocut() checks if the new map is already completely covered in
	 * another map or other way round
	 */
	@Override
	public void addMapsAutocut(String mapNameBase, IfMapSource mapSource, Point minPixelCoordinate, Point maxPixelCoordinate, int zoom,
	    TileImageParameters parameters, int maxMapSize, int overlapTiles) throws InvalidNameException
	{
		log.trace("Adding new map(s): \"" + mapNameBase + "\" " + mapSource + " zoom=" + zoom + " min=" + minPixelCoordinate.x + "/" + minPixelCoordinate.y
		    + " max=" + maxPixelCoordinate.x + "/" + maxPixelCoordinate.y);

		MapDescription mD = new MapDescription();

		mD.name = mapNameBase;
		mD.nZoomLvl = zoom;
		mD.maxPixelC = maxPixelCoordinate;
		mD.minPixelC = minPixelCoordinate;
		mD.mapSource = mapSource;

		// if no zoom level yet, set it, else check if it is correct
		if (nZoomLvl == -1)
			nZoomLvl = zoom;
		if (zoom == nZoomLvl)
		{
			int tileSize = mapSource.getMapSpace().getTileSize();
			int nXSize = (maxPixelCoordinate.x - minPixelCoordinate.x) / tileSize + 1;
			int nYSize = (maxPixelCoordinate.y - minPixelCoordinate.y) / tileSize + 1;
			int nXExp = 1, nYExp = 1;

			log.trace("addMapsAutocut(): tile=" + tileSize + ", XSize=" + nXSize + ", YSize=" + nYSize + ", XExp=" + nXExp);

			// get requested size in 2^n grid
			while ((nXSize >>= 1) > 1)
				++nXExp;
			while ((nYSize >>= 1) > 1)
				++nYExp;

			// fit into encouraged map grid widths (8, 16, 32, 64, 128 tiles)
			nXExp = Math.min(zoom, Math.max(3, Math.min(7, nXExp)));
			int nXGridSize = tileSize << nXExp;
			nYExp = Math.min(zoom, Math.max(3, Math.min(7, nYExp)));
			int nYGridSize = tileSize << nYExp;

			log.trace("addMapsAutocut(): nXExp=" + nXExp + ", nXGridSize=" + nXGridSize + ", nYExp=" + nYExp + ", nYGridSize=" + nYGridSize);

			// align left/top with map grid
			int nXOff = 0, nYOff = 0;
			nXOff = minPixelCoordinate.x % (nXGridSize);
			// minPixelCoordinate.x -= (nXOff > (nXGridSize / 4) ? nXOff : nXOff + nXGridSize / 2);
			minPixelCoordinate.x -= nXOff;
			nYOff = minPixelCoordinate.y % (nYGridSize);
			// minPixelCoordinate.y -= (nYOff > (nYGridSize / 4) ? nYOff : nYOff + nYGridSize / 2);
			minPixelCoordinate.y -= nYOff;

			log.trace("addMapsAutocut(): nXOff=" + nXOff + ", mtc.x=" + minPixelCoordinate.x + ", nYOff=" + nYOff + ", mtc.y=" + minPixelCoordinate.y);

			// align right/bottom with map grid
			nXOff = nXGridSize + tileSize * overlapTiles - maxPixelCoordinate.x % (nXGridSize) - 1; // /W - 1 instead of + 1
			// maxPixelCoordinate.x += (nXOff > (nXGridSize / 4) ? nXOff : nXOff + nXGridSize / 2);
			maxPixelCoordinate.x += nXOff;
			nYOff = nYGridSize + tileSize * overlapTiles - maxPixelCoordinate.y % (nYGridSize) - 1; // /W - 1 instead of + 1
			// maxPixelCoordinate.y += (nYOff > (nYGridSize / 4) ? nYOff : nYOff + nYGridSize / 2);
			maxPixelCoordinate.y += nYOff;

			log.trace("addMapsAutocut(): nXOff=" + nXOff + ", xtc.x=" + maxPixelCoordinate.x + ", nYOff=" + nYOff + ", xtc.y=" + maxPixelCoordinate.y);

			// we only use fixed size tiles (256 x 256)
			Dimension tileDimension = new Dimension(tileSize, tileSize);
			// if the user set parameters we use them
			// Dimension tileDimension;
			// if (parameters == null)
			// tileDimension = new Dimension(tileSize, tileSize);
			// else
			// tileDimension = parameters.getDimension();

			// We adapt the max map size to the tile size so that we do not get
			// ugly cut/incomplete tiles at the borders
			Dimension maxMapDimension = new Dimension(maxMapSize, maxMapSize);
			maxMapDimension.width -= maxMapSize % tileDimension.width;
			maxMapDimension.height -= maxMapSize % tileDimension.height;

			log.trace("Adding new map(s) after alignment: \"" + mapNameBase + "\" " + mapSource + " zoom=" + zoom + " min=" + minPixelCoordinate.x + "/"
			    + minPixelCoordinate.y + " max=" + maxPixelCoordinate.x + "/" + maxPixelCoordinate.y);

			// is the new map enclosed in an already existing map -> nothing to do at all
			if (checkMapIsSubset(mD))
				return;
			// check if the new map is a superset of already existing maps -> delete old maps
			checkMapSuperset(mD);
			// is the new map an extension of an already existing map
			mD = checkMapIsExtension(mD);

			// does the map fit the allowed size or has it be cut into several maps
			int mapWidth = maxPixelCoordinate.x - minPixelCoordinate.x + 1;
			int mapHeight = maxPixelCoordinate.y - minPixelCoordinate.y + 1;
			if ((mapWidth <= maxMapDimension.width) && (mapHeight <= maxMapDimension.height))
			{
				// String mapName = String.format(mapNameFormat, new Object[]
				// {mapNameBase, mapCounter++});
				String mapName = MakeValidMapName(mD.name, "0000");
				Map s = new Map(this, mapName, mD.mapSource, mD.nZoomLvl, mD.minPixelC, mD.maxPixelC, parameters);
				maps.add(s);

			}
			else
			{
				log.warn("map not added due to size");
			}
			// {
			// Dimension nextMapStep = new Dimension(maxMapDimension.width -
			// (tileDimension.width * overlapTiles), maxMapDimension.height
			// - (tileDimension.height * overlapTiles));
			//
			// for (int mapX = minPixelCoordinate.x; mapX < maxPixelCoordinate.x;
			// mapX += nextMapStep.width)
			// {
			// for (int mapY = minPixelCoordinate.y; mapY < maxPixelCoordinate.y;
			// mapY += nextMapStep.height)
			// {
			// int maxX = Math.min(mapX + maxMapDimension.width,
			// maxPixelCoordinate.x);
			// int maxY = Math.min(mapY + maxMapDimension.height,
			// maxPixelCoordinate.y);
			// Point min = new Point(mapX, mapY);
			// Point max = new Point(maxX - 1, maxY - 1);
			// // check if this map is not a sub/superset of another already
			// existing map
			// if (!CheckMapIsExtension(min, max))
			// {
			// if (CheckMapArea(min, max))
			// {
			// // String mapName = String.format(mapNameFormat, new Object[]
			// {mapNameBase, mapCounter++});
			// String mapName = MakeValidMapName(mapNameBase, "0000");
			// Map s = new Map(this, mapName, mapSource, zoom, min, max,
			// parameters);
			// maps.add(s);
			// }
			// }
			// }
			// }
			// }
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
				log.error("newMapName=" + newMapName + "; c=" + c); // #??? double mapNums
				mapNr = 0;
				continue;
			}
		}
		return newMapName;
	}

	/**
	 * This checks if the new map is totally covered by an already existing map
	 * 
	 * @param mD
	 *          a {@link MapDescription} of the new map
	 * 
	 * @return true if mD is a subset of an existing map
	 */
	protected boolean checkMapIsSubset(MapDescription mD)
	{
		for (int mapNr = 0; mapNr < getMapCount(); ++mapNr)
		{
			IfMap map = getMap(mapNr);
			log.trace("checking against map: \"" + map.getName() + "\" " + map.getMapSource().getName() + " zoom=" + map.getZoom() + " min="
			    + map.getMinPixelCoordinate().x + "/" + map.getMinPixelCoordinate().y + " max=" + map.getMaxPixelCoordinate().x + "/" + map.getMaxPixelCoordinate().y);
			if ((map.getMinPixelCoordinate().x <= mD.minPixelC.x) && (map.getMinPixelCoordinate().y <= mD.minPixelC.y))
			{
				if ((map.getMaxPixelCoordinate().x >= mD.maxPixelC.x) && (map.getMaxPixelCoordinate().y >= mD.maxPixelC.y))
				{
					log.trace("match found (new is smaller): " + " min=" + mD.minPixelC.x + "/" + mD.minPixelC.y +
					    " max=" + mD.maxPixelC.x + "/" + mD.maxPixelC.y);
					mD.minPixelC.x = map.getMinPixelCoordinate().x;
					mD.minPixelC.y = map.getMinPixelCoordinate().y;
					mD.maxPixelC.x = map.getMaxPixelCoordinate().x;
					mD.maxPixelC.y = map.getMaxPixelCoordinate().y;
					log.trace("match found (old superset): " + " min=" + mD.minPixelC.x + "/" + mD.minPixelC.y +
					    " max=" + mD.maxPixelC.x + "/" + mD.maxPixelC.y);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This deletes one (or more) existing map(s) if the new map is a superset of this map(s)
	 * 
	 * @param mD
	 *          a {@link MapDescription} of the new map
	 */
	protected void checkMapSuperset(MapDescription mD)
	{
		for (int mapNr = 0; mapNr < getMapCount(); ++mapNr)
		{
			IfMap map = getMap(mapNr);
			log.trace("checking against map: \"" + map.getName() + "\" " + map.getMapSource().getName() + " zoom=" + map.getZoom() + " min="
			    + map.getMinPixelCoordinate().x + "/" + map.getMinPixelCoordinate().y + " max=" + map.getMaxPixelCoordinate().x + "/" + map.getMaxPixelCoordinate().y);
			if ((map.getMinPixelCoordinate().x >= mD.minPixelC.x) && (map.getMinPixelCoordinate().y >= mD.minPixelC.y))
			{
				if ((map.getMaxPixelCoordinate().x <= mD.maxPixelC.x) && (map.getMaxPixelCoordinate().y <= mD.maxPixelC.y))
				{
					map.delete();
					--mapNr;
					log.trace("deleted old, new superset: " + " min=" + mD.minPixelC.x + "/" + mD.minPixelC.y + " max=" + mD.maxPixelC.x + "/" + mD.maxPixelC.y);
				}
			}
		}
	}

	/**
	 * checks if the new map is an extension of an already existing map. If it
	 * is, the existing map will be changed to new coordinates which include the
	 * new map. 20140511 case new map lies between two already existing maps is
	 * not covered yet. The new map will be extending both
	 * 
	 * @param mD
	 *          a {@link MapDescription} of the new map
	 * @return
	 *         the possibly changed {@link MapDescription} of the new map
	 */
	public MapDescription checkMapIsExtension(MapDescription mD)
	{
		for (int mapNr = 0; mapNr < getMapCount(); ++mapNr)
		{
			IfMap map = getMap(mapNr);
			if ((map.getMinPixelCoordinate().y == mD.minPixelC.y) && (map.getMaxPixelCoordinate().y == mD.maxPixelC.y))
			{
				if ((map.getMinPixelCoordinate().x >= mD.minPixelC.x) && (map.getMinPixelCoordinate().x <= mD.maxPixelC.x + 1)
				    && (map.getMaxPixelCoordinate().x > mD.maxPixelC.x))
				{
					mD.maxPixelC.x = map.getMaxPixelCoordinate().x;
					map.delete();
					--mapNr;
				}
				else if ((map.getMaxPixelCoordinate().x <= mD.maxPixelC.x) && (map.getMaxPixelCoordinate().x + 1 >= mD.minPixelC.x)
				    && (map.getMinPixelCoordinate().x < mD.minPixelC.x))
				{
					mD.minPixelC.x = map.getMinPixelCoordinate().x;
					map.delete();
					--mapNr;
				}
			}
			else 
			if ((map.getMinPixelCoordinate().x == mD.minPixelC.x) && (map.getMaxPixelCoordinate().x == mD.maxPixelC.x))
			{
				if ((map.getMinPixelCoordinate().y >= mD.minPixelC.y) && (map.getMinPixelCoordinate().y <= mD.maxPixelC.y + 1)
				    && (map.getMaxPixelCoordinate().y > mD.maxPixelC.y))
				{
					mD.maxPixelC.y = map.getMaxPixelCoordinate().y;
					map.delete();
					--mapNr;
				}
				else if ((map.getMaxPixelCoordinate().y <= mD.maxPixelC.y) && (map.getMaxPixelCoordinate().y + 1 >= mD.minPixelC.y)
				    && (map.getMinPixelCoordinate().y < mD.minPixelC.y))
				{
					mD.minPixelC.y = map.getMinPixelCoordinate().y;
					map.delete();
					--mapNr;
				}
			}
		}
		return mD;
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
					throw new InvalidNameException(
					    "There is already a layer named \"" + newName + "\" in this catalog.\nLayer names have to be unique within a catalog.");
			}
		}
		this.name = newName;
	}

	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * This skips offline mapsources
	 */
	@Override
	public long calculateTilesToDownload()
	{
		long tiles = 0;
		for (IfMap map : maps)
		{
			if (!(map.getMapSource() instanceof IfFileBasedMapSource))
				tiles += map.calculateTilesToDownload();
		}
		log.trace("layer=" + getName() + ", tiles=" + tiles);
		return tiles;
	}

	/////////////////////////////////////////////////////////////
	@Override
	public int getXBorderMin()
	{
		int xMin = Integer.MAX_VALUE;
		for (IfMap m : maps)
		{
			xMin = Math.min(xMin, m.getXBorderMin());
		}
		return xMin;
	}

	@Override
	public int getXBorderMax()
	{
		int xMax = Integer.MIN_VALUE;
		for (IfMap m : maps)
		{
			xMax = Math.max(xMax, m.getXBorderMax());
		}
		return xMax;
	}

	@Override
	public int getYBorderMin()
	{
		int yMin = Integer.MAX_VALUE;
		for (IfMap m : maps)
		{
			yMin = Math.min(yMin, m.getYBorderMin());
		}
		return yMin;
	}

	@Override
	public int getYBorderMax()
	{
		int yMax = Integer.MIN_VALUE;
		for (IfMap m : maps)
		{
			yMax = Math.max(yMax, m.getYBorderMax());
		}
		return yMax;
	}
	/////////////////////////////////////////////////////////////

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
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_layer_map_count",
		// maps.size()));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_max_tile",
		// calculateTilesToDownload()));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_area_start",
		// OSMCBUtilities.prettyPrintLatLon(getMaxLat(), true),
		// OSMCBUtilities.prettyPrintLatLon(getMinLon(), false)));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_area_end",
		// OSMCBUtilities.prettyPrintLatLon(getMinLat(), true),
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
	@XmlAttribute
	// /W
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
