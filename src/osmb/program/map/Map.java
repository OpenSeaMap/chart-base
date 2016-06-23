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
import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import osmb.exceptions.InvalidNameException;
import osmb.mapsources.ACMapSource;
import osmb.mapsources.ACMultiLayerMapSource;
import osmb.mapsources.MP2Corner;
import osmb.mapsources.MP2MapSpace;
// W #mapSpace import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.catalog.IfCapabilityDeletable;
import osmb.program.catalog.IfCatalog;
import osmb.program.tiles.IfTileFilter;
import osmb.program.tiles.TileImageParameters;
//W #mapSpace import osmb.utilities.geo.EastNorthCoordinate;
import osmb.utilities.geo.GeoCoordinate;
//W #mapSpaceimport osmb.utilities.image.MercatorPixelCoordinate;

/**
 * The map is the implementation of a rectangular (not really, since the earth is a globe) area on earths surface, which is rendered in square tiles (of 256 x
 * 265) pixels.
 * see {@link IfMapSpace}.
 * For a description of the map space coordinates see {@link MercatorPower2MapSpace}.
 * 
 * @author humbach
 */
@XmlType(propOrder =
{ "name", "mapSource", "ULC", "LRC", "minTileCoordinate", "maxTileCoordinate", "number" })
public class Map implements IfMap, IfCapabilityDeletable, TreeNode
{
	// class/static data
	protected static Logger log = Logger.getLogger(Map.class);

	/**
	 * This comparator sorts the maps according to
	 * <ul>
	 * <li>first: their YMin tile coordinates (ascending)
	 * <li>second: their XMin tile coordinates (ascending)
	 * <li>third: their yMax tile coordinates (ascending)
	 * <li>last: their XMax tile coordinates (ascending) (sub-, supersets or 'identity')
	 */
	public static final Comparator<Map> YXMinYXMaxASC = new Comparator<Map>()
	{
		@Override
		public int compare(Map m1, Map m2)
		{
			int nRes = 0;
			if (m1.getYMin() < m2.getYMin())
				nRes = -1;
			else if (m1.getYMin() > m2.getYMin())
				nRes = 1;
			else // m1.getYMin() == m2.getYMin()
			{
				if (m1.getXMin() < m2.getXMin())
					nRes = -1;
				else if (m1.getXMin() > m2.getXMin())
					nRes = 1;
				else // m1.getXMin() == m2.getXMin()
				{
					if (m1.getYMax() < m2.getYMax())
						nRes = -1;
					else if (m1.getYMax() > m2.getYMax())
						nRes = 1;
					else // m1.getYMax() == m2.getYMax()
					{
						if (m1.getXMax() < m2.getXMax()) // m1 is subset of m2
							nRes = -1;
						else if (m1.getXMax() > m2.getXMax()) // m1 is superset of m2
							nRes = 1;
						else // m1.getXMax() == m2.getXMax() // identical areas -> deletion of one map!
							nRes = 0;
					}
				}
			}
			return nRes;
		}
	};

	/**
	 * This comparator sorts the maps according to
	 * <ul>
	 * <li>first: their {@link name names} (ascending)
	 * <li>second: their {@link #number numbers} (ascending).
	 */
	public static final Comparator<Map> NameNumberASC = new Comparator<Map>()
	{
		@Override
		public int compare(Map m1, Map m2)
		{
			int nRes = 0;
			if (m1.getName().compareTo(m2.getName()) < 0)
				nRes = -1;
			else if (m1.getName().compareTo(m2.getName()) > 0)
				nRes = 1;
			else // m1.getName() == m2.getName()
			{
				if (m1.getNumber().compareTo(m2.getNumber()) < 0)
					nRes = -1;
				else if (m1.getNumber().compareTo(m2.getNumber()) > 0)
					nRes = 1;
				else // m1.getNumber() == m2.getNumber() // names and numbers identical -> deletion of one map!
					nRes = 0;
			}
			return nRes;
		}
	};

	/**
	 * This composes an osm-internal map number in the form: Zoom-LatIdx-LonIdx-Height-Width, which is worldwide unique. It is a sufficient map area
	 * specification.
	 * 
	 * @param nZoom
	 *          The maps zoom level.
	 * @param nMinY
	 *          The upper (northerly) pixel index.
	 * @param nMaxY
	 *          The lower (southerly) pixel index.
	 * @param nMinX
	 *          The left (westerly) pixel index.
	 * @param nMaxX
	 *          The right (easterly) pixel index.
	 * @return A String containing the unique chart number.
	 * @see #number
	 */
	public static String makeMapNumber(int nZoom, int nMinY, int nMaxY, int nMinX, int nMaxX)
	{
		// 20150722 AH use fix numbers in here
		String strNum = nZoom + "-" + nMinY / (MP2MapSpace.TECH_TILESIZE) + "-" + nMinX / (MP2MapSpace.TECH_TILESIZE) + "-"
		    + ((nMaxY - nMinY + 1) / MP2MapSpace.TECH_TILESIZE) + "-" + ((nMaxX - nMinX + 1) / MP2MapSpace.TECH_TILESIZE);
		return strNum;
	}

	// instance data
	/**
	 * The osm internal name. This is "L" + the maps {@link #number}.
	 */
	protected String name;
	/**
	 * The osm internal number. It composes a worldwide unique map number, stating zoom level, location and size in tiles.<br>
	 * ZOOM-LAT-LON-HEIGHT-WIDTH, LAT and LON of the upper left (N-W) corner in tiles, HEIGHT, WIDTH in tiles.
	 */
	protected String number;
	/**
	 * the INT conformant name - if there is one; for a lot of maps this is empty.
	 */
	protected String intName = null;
	/**
	 * the INT conformant number - if there is one; for a lot of maps this is empty.
	 */
	protected String intNumber = null;
	/**
	 * the INT national name - if there is one; for a lot of maps this is empty.
	 */
	protected String natName = null;
	protected Point maxPixelCoordinate = null;
	protected Point minPixelCoordinate = null;
	@XmlAttribute
	protected ACMapSource mapSource = null;
	// protected int zoom;
	protected Layer layer;
	protected TileImageParameters parameters = null;
	protected Dimension tileDimension = null;

	protected Map()
	{
		log = Logger.getLogger(this.getClass());
	}

	protected Map(Map map)
	{
		name = map.name;
		number = map.number;
	}

	// protected Map(Layer layer, String name, IfMapSource mapSource, int zoom, Point minPixelCoordinate, Point maxPixelCoordinate, TileImageParameters
	// parameters)
	protected Map(Layer layer, ACMapSource mapSource, int zoom, Point minPixelCoordinate, Point maxPixelCoordinate, TileImageParameters parameters)
	{
		this.layer = layer;
		this.maxPixelCoordinate = maxPixelCoordinate;
		this.minPixelCoordinate = minPixelCoordinate;
		this.number = makeMapNumber(zoom, minPixelCoordinate.y, maxPixelCoordinate.y, minPixelCoordinate.x, maxPixelCoordinate.x);
		this.name = "L" + number;
		this.mapSource = mapSource;
		// this.zoom = zoom;
		this.parameters = parameters;
		calculateRuntimeValues();
		// setMapNumber(zoom, minPixelCoordinate.y, maxPixelCoordinate.y, minPixelCoordinate.x, maxPixelCoordinate.x);
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
			int tileSize = MP2MapSpace.getTileSize(); // W #mapSpace mapSource.getMapSpace().getTileSize();
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
	public ACMapSource getMapSource()
	{
		return this.mapSource;
	}

	/**
	 * @XmlJavaTypeAdapter(PointAdapter.class) annotation in package-info.java
	 */
	@Override
	@XmlAttribute
	public Point getMaxTileCoordinate()
	{
		int x = maxPixelCoordinate.x / tileDimension.width;
		int y = maxPixelCoordinate.y / tileDimension.height;
		return new Point(x, y);
	}

	/**
	 * @XmlJavaTypeAdapter(PointAdapter.class) annotation in package-info.java
	 */
	@Override
	@XmlAttribute
	public Point getMinTileCoordinate()
	{
		int x = minPixelCoordinate.x / tileDimension.width;
		int y = minPixelCoordinate.y / tileDimension.height;
		return new Point(x, y);
	}

	@Override
	public void setMaxTileCoordinate(Point MaxC)
	{
		Point pMax = new Point((MaxC.x + 1) * MP2MapSpace.TECH_TILESIZE - 1, (MaxC.y + 1) * MP2MapSpace.TECH_TILESIZE - 1);// W #mapSpace ((MaxC.x + 1) *
		                                                                                                                   // IfMapSpace.TECH_TILESIZE - 1, (MaxC.y
		                                                                                                                   // + 1) * IfMapSpace.TECH_TILESIZE - 1);
		maxPixelCoordinate = pMax;
	}

	@Override
	public void setMinTileCoordinate(Point MinC)
	{
		Point pMin = new Point(MinC.x * MP2MapSpace.TECH_TILESIZE, MinC.y * MP2MapSpace.TECH_TILESIZE);// W #mapSpace (MinC.x * IfMapSpace.TECH_TILESIZE, MinC.y *
		                                                                                               // IfMapSpace.TECH_TILESIZE);
		minPixelCoordinate = pMin;
	}

	@Override
	@XmlTransient
	public Point getMaxPixelCoordinate()
	{
		return maxPixelCoordinate;
	}

	@Override
	@XmlTransient
	public Point getMinPixelCoordinate()
	{
		return minPixelCoordinate;
	}

	@Override
	public void setMaxPixelCoordinate(Point MaxC)
	{
		maxPixelCoordinate = MaxC;
	}

	@Override
	public void setMinPixelCoordinate(Point MinC)
	{
		minPixelCoordinate = MinC;
	}

	// W #mapSpace MP2Corner.toGeoCoordinate()
	/**
	 * This creates a string containing latitude and longitude of the upper left (north east) corner of the map.<br>
	 * 
	 * @return string with double border coordinates
	 */
	@XmlAttribute
	public String getULC()
	{
		// return new EastNorthCoordinate(mapSource.getMapSpace(), getZoom(), minPixelCoordinate.x, minPixelCoordinate.y).toCatalog();
		return new MP2Corner(minPixelCoordinate.x, minPixelCoordinate.y, getZoom()).toGeoCoordinate().toCatalog();
	}

	public void setULC(String strULC)
	{
	}

	/**
	 * This creates a string containing latitude and longitude of the lower right (south-west) corner of the map.<br>
	 * 
	 * For example: the lower right corner of earth in Mercator projection is lat=-85.05112878, lon=180.00000000
	 * 
	 * @return string with double border coordinates
	 */
	@XmlAttribute
	public String getLRC()
	{
		// return new EastNorthCoordinate(mapSource.getMapSpace(), getZoom(), maxPixelCoordinate.x + 1, maxPixelCoordinate.y + 1).toCatalog();
		return new MP2Corner(maxPixelCoordinate.x + 1, maxPixelCoordinate.y + 1, getZoom()).toGeoCoordinate().toCatalog();
	}

	public void setLRC(String strLRC)
	{
	}

	/**
	 * XMin is west
	 * 
	 * @return int tile index
	 */
	@Override
	public int getXMin()
	{
		return minPixelCoordinate.x / MP2MapSpace.TECH_TILESIZE;
	}

	/**
	 * XMax is east
	 * 
	 * @return int tile index
	 */
	@Override
	public int getXMax()
	{
		return maxPixelCoordinate.x / MP2MapSpace.TECH_TILESIZE;
	}

	/**
	 * YMin is north
	 * 
	 * @return int tile index
	 */
	@Override
	public int getYMin()
	{
		return minPixelCoordinate.y / MP2MapSpace.TECH_TILESIZE;
	}

	/**
	 * YMax is south
	 * 
	 * @return int tile index
	 */
	@Override
	public int getYMax()
	{
		return maxPixelCoordinate.y / MP2MapSpace.TECH_TILESIZE;
	}

	@Override
	@XmlAttribute
	public String getName()
	{
		// test if the name is correct
		this.number = makeMapNumber(getZoom(), minPixelCoordinate.y, maxPixelCoordinate.y, minPixelCoordinate.x, maxPixelCoordinate.x);
		this.name = "L" + number;
		return name;
	}

	@Override
	public void setName(String newName) throws InvalidNameException
	{
		if (layer != null)
		{
			for (IfMap map : layer)
			{
				if ((map != this) && (newName.equals(map.getName())))
					throw new InvalidNameException("There is already a map named \"" + newName + "\" in this layer.\nMap names have to be unique.");
			}
		}
		this.name = newName;
		log.trace("map " + name);
	}

	@Override
	@XmlAttribute
	public String getNumber()
	{
		this.number = makeMapNumber(getZoom(), minPixelCoordinate.y, maxPixelCoordinate.y, minPixelCoordinate.x, maxPixelCoordinate.x);
		this.name = "L" + number;
		return number;
	}

	public void setNumber(String strNum)
	{
		number = strNum;
		this.name = "L" + strNum;
		log.trace("map " + number + ", X=" + (minPixelCoordinate.x / 256) + ", Y=" + (minPixelCoordinate.y / 256));
	}

	/**
	 * This allows to (re-)adjust the map number to the current area of the map.
	 * 
	 * @param nZoom
	 * @param nMinY
	 *          Pixel coordinate
	 * @param nMaxY
	 *          Pixel coordinate
	 * @param nMinX
	 *          Pixel coordinate
	 * @param nMaxX
	 *          Pixel coordinate
	 */
	public void setMapNumber(int nZoom, int nMinY, int nMaxY, int nMinX, int nMaxX)
	{
		// 20160315 AH This provides a spec of the map in tile indices, (Zoom-YIdx-XIdy-Height-Width). This map number is worldwide unique.
		String strNum = makeMapNumber(nZoom, nMinY, nMaxY, nMinX, nMaxX);
		log.trace("new map: '" + strNum + "'");
		this.number = strNum;
		this.name = "L" + strNum;
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
		return "Map\n name=" + name + "\n mapSource=" + mapSource + "\n zoom=" + getZoom() + "\n maxPixelCoordinate=" + maxPixelCoordinate.x + "/"
		    + maxPixelCoordinate.y + "\n minPixelCoordinate=" + minPixelCoordinate.x + "/" + minPixelCoordinate.y + "\n parameters=" + parameters;
	}

	public String getToolTip()
	{
		// W #mapSpace IfMapSpace mapSpace = mapSource.getMapSpace();//W #mapSpace MP2Corner GeoCoordinate
		@SuppressWarnings("unused") // /W #unused
		// EastNorthCoordinate tl = new EastNorthCoordinate(mapSpace, getZoom(), minPixelCoordinate.x, minPixelCoordinate.y);
		GeoCoordinate tl = new MP2Corner(minPixelCoordinate.x, minPixelCoordinate.y, getZoom()).toGeoCoordinate();
		@SuppressWarnings("unused") // /W #unused
		GeoCoordinate br = new MP2Corner(maxPixelCoordinate.x, maxPixelCoordinate.y, getZoom()).toGeoCoordinate();

		StringWriter sw = new StringWriter(1024);
		// sw.write("<html>");
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_map_title"));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_source",
		// StringEscapeUtils.escapeHtml4(mapSource.toString()),
		// StringEscapeUtils.escapeHtml4(mapSource.getName())));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_zoom_lv",
		// zoom));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_area_start",
		// tl.toString(), minPixelCoordinate.x, minPixelCoordinate.y));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_area_end",
		// br.toString(), maxPixelCoordinate.x, maxPixelCoordinate.y));
		// sw.write(OSMCBStrs.getLocalizedString("lp_bundle_info_map_size",
		// (maxPixelCoordinate.x - minPixelCoordinate.x + 1), (maxPixelCoordinate.y
		// - minPixelCoordinate.y + 1)));
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

	// W #mapSpace
	/**
	 * This calculates of the upper left (north-west) corner of the map in {@link osmb.program.map.IfMapSpace#MAX_TECH_ZOOM zoom 22}.
	 * 
	 * @return Corner coordinate (N-W) in zoom 22<br>
	 *         see {@link MercatorPixelCoordinate}
	 */
	protected MP2Corner ulCornerToMaxZoom()
	{
		MP2Corner borderCoord = new MP2Corner(minPixelCoordinate.x, minPixelCoordinate.y, getZoom());
		borderCoord = borderCoord.adaptToZoomlevel(MP2MapSpace.MAX_TECH_ZOOM);
		return borderCoord;
	}

	// W #mapSpace
	/**
	 * This calculates of the lower right (south-east) corner of the map in {@link osmb.program.map.IfMapSpace#MAX_TECH_ZOOM zoom 22}.
	 * 
	 * @return Corner coordinate (S-E) in zoom 22<br>
	 *         see {@link MercatorPixelCoordinate}
	 */
	protected MP2Corner lrCornerToMaxZoom()
	{
		MP2Corner borderCoord = new MP2Corner(maxPixelCoordinate.x + 1, maxPixelCoordinate.y + 1, getZoom());
		borderCoord = borderCoord.adaptToZoomlevel(MP2MapSpace.MAX_TECH_ZOOM);
		return borderCoord;
	}

	@Override
	public int getXBorderMin()
	{
		return ulCornerToMaxZoom().getX();
	}

	@Override
	public int getXBorderMax()
	{
		return lrCornerToMaxZoom().getX();
	}

	@Override
	public int getYBorderMin()
	{
		return ulCornerToMaxZoom().getY();
	}

	@Override
	public int getYBorderMax()
	{
		return lrCornerToMaxZoom().getY();
	}

	@Override
	public double getMinLat()
	{
		return MP2MapSpace.cYToLatLowerBorder(maxPixelCoordinate.y, getZoom());
	}

	@Override
	public double getMaxLat()
	{
		return MP2MapSpace.cYToLatUpperBorder(minPixelCoordinate.y, getZoom());
	}

	@Override
	public double getMinLon()
	{
		return MP2MapSpace.cXToLonLeftBorder(minPixelCoordinate.x, getZoom());
	}

	@Override
	public double getMaxLon()
	{
		return MP2MapSpace.cXToLonRightBorder(maxPixelCoordinate.x, getZoom());
	}

	@Override
	public void delete()
	{
		layer.deleteMap(this);
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
		long tiles = (maxPixelCoordinate.x - minPixelCoordinate.x + 1) * (maxPixelCoordinate.y - minPixelCoordinate.y + 1) // /W + 1, + 1
		    / (MP2MapSpace.getTileSize() * MP2MapSpace.getTileSize()); // #mapSpace (mapSource.getMapSpace().getTileSize() * mapSource.getMapSpace().getTileSize());
		return tiles;
	}

	/**
	 * This simply calculates all tiles included in the map. It currently (2015-08) does not take in account that tiles are shared by several maps.
	 */
	@Override
	public long calculateTilesToLoad()
	{
		long tiles = getTileCount();
		if (mapSource instanceof ACMultiLayerMapSource)
		{
			// We have a map with most probably two layers and for each layer we have to download the tiles - therefore double the tileCount
			// Adjust the factor more to reality than theory. We don't have twice the tiles in OpenSeaMap, which currently is the main second layer.
			tiles *= 15;
			tiles /= 10;
		}
		log.trace("map='" + getName() + "', tiles=" + tiles);
		return tiles;
	}

	@Override
	public boolean isInvalid()
	{
		boolean result = false;
		boolean[] checks =
		{ name == null, // 0
		    layer == null, // 1
		    maxPixelCoordinate == null, // 2
		    minPixelCoordinate == null, // 3
		    mapSource == null, // 4
		    getZoom() < 0 // 5
		};

		for (int i = 0; i < checks.length; i++)
			if (checks[i])
			{
				log.error("Problem detected with map \"" + name + "\" check: " + i);
				result = true;
			}
		// Automatically correct bad ordered min/max coordinates
		try // W #??? try-catch
		{
			if (minPixelCoordinate.x > maxPixelCoordinate.x)
			{
				int tmp = maxPixelCoordinate.x;
				maxPixelCoordinate.x = minPixelCoordinate.x;
				minPixelCoordinate.x = tmp;
			}
			if (minPixelCoordinate.y > maxPixelCoordinate.y)
			{
				int tmp = maxPixelCoordinate.y;
				maxPixelCoordinate.y = minPixelCoordinate.y;
				minPixelCoordinate.y = tmp;
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
			map.maxPixelCoordinate = (Point) maxPixelCoordinate.clone();
			map.minPixelCoordinate = (Point) minPixelCoordinate.clone();
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
