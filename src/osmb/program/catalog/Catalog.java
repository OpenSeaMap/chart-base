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
package osmb.program.catalog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.TreeNode;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;

import osmb.program.ACSettings;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.program.map.Layer;
import osmb.utilities.OSMBUtilities;

@XmlRootElement
public class Catalog implements IfCatalogProfile, IfCatalog, TreeNode, Comparable<IfCatalogProfile>
{
	// standard data
	public static final int CURRENT_CATALOG_VERSION = 2;
	public static final int MIN_CATALOG_ZOOMLEVEL = 4; // /W #zoom0-3 used to disable zoomlevels 0 t0 3 in MainFrame
	protected static Logger log = Logger.getLogger(Catalog.class);

	// class/static data
	public static final String CATALOG_NAME_REGEX = "[\\w _-]+";
	public static final String CATALOG_FILENAME_PREFIX = "osmcb-catalog-";
	public static final Pattern CATALOG_FILENAME_PATTERN = Pattern.compile(CATALOG_FILENAME_PREFIX + "(" + CATALOG_NAME_REGEX + ").xml");
	// public static final Catalog DEFAULT = new Catalog();
	@Deprecated // /W #deprecated
	protected static Vector<Catalog> catalogs = new Vector<Catalog>();

	/**
	 * Builds a (hopefully) valid filename for a given catalog name
	 * 
	 * @param catalogName
	 * @return valid filename or null!
	 */
	public static String getCatalogFileName(String catalogName)
	{
		// do a check, if the name is valid
		if (catalogName == null)
			return null; // otherwise returns "osmcb-catalog-null.xml"!
		else
			return CATALOG_FILENAME_PREFIX + catalogName + ".xml";
	}
	
	/**
	 * Builds a (hopefully) valid filename from a given catalog name for the overview '.png'
	 * 
	 * @param catalogName
	 * @return valid filename or null!
	 */
	public static String getCatalogOverviewFileName(String catalogName)
	{
		// do a check, if the name is valid
		if (catalogName == null)
			return null; // otherwise returns "osmcb-catalog-null.xml"!
		else
			return CATALOG_FILENAME_PREFIX + catalogName + ".png";
	}

	/**
	 * This creates a new default instance of a catalog: an empty catalog with name, current version
	 * and a (from name derived empty) file member != null (to enable Catalog: boolean equals(Object)!)
	 * 
	 * @param name
	 *          has to be not null, otherwise file member becomes null!
	 * 
	 * @return The current catalog, which is a default instance at the moment.
	 */
	public static Catalog newInstance(String name)
	{
		Catalog catalog = new Catalog();
		catalog.name = name;
		catalog.file = new File(ACSettings.getInstance().getCatalogsDirectory(), getCatalogFileName(name));
		catalog.version = CURRENT_CATALOG_VERSION;
		return catalog;
	}

	/**
	 * Catalogs management method
	 */
	@Deprecated // /W #deprecated
	public static Vector<Catalog> getCatalogs()
	{
		updateCatalogs();
		return catalogs;
	}

	/**
	 * This updates the listing of the available catalogs in the catalogs
	 * directory. It is used by {@link #JCatalogsComboBox}
	 */
	@Deprecated // /W #deprecated
	public static void updateCatalogs()
	{
		catalogs.clear();
		File catalogsDir = ACSettings.getInstance().getCatalogsDirectory();
		catalogsDir.list(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String fileName)
			{
				Matcher m = CATALOG_FILENAME_PATTERN.matcher(fileName);
				if (m.matches())
				{
					String catalogName = m.group(1);
					Catalog catalog = new Catalog(new File(dir, fileName), catalogName);
					catalogs.add(catalog);
				}
				return false;
			}
		});
		Collections.sort(catalogs);
	}

	/**
	 * This actually creates a new catalog object and fills it with the content from the {@link #file}
	 */
	public static IfCatalog load(File file) throws JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(Catalog.class);
		Unmarshaller um = context.createUnmarshaller();

		// /W ValidationEventHandler is not helpful, user will get an info from GUIExceptionHandler
		// um.setEventHandler(new ValidationEventHandler()
		// {
		// @Override
		// public boolean handleEvent(ValidationEvent event)
		// {
		// ValidationEventLocator loc = event.getLocator();
		// String file = loc.getURL().getFile();
		// int lastSlash = file.lastIndexOf('/');
		// if (lastSlash > 0)
		// file = file.substring(lastSlash + 1);
		// // UI should be separated from program logic -> relocate to UI components
		// int ret = JOptionPane.showConfirmDialog(null,
		// String.format(OSMBStrs.RStr("Catalog.Loading.ErrMsg"), event.getMessage(), file, loc.getLineNumber(), loc.getColumnNumber()),
		// OSMBStrs.RStr("Catalog.Loading.Title"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
		// log.error(event.toString());
		// return (ret == JOptionPane.YES_OPTION);
		// }
		// });

		try
		{
			Catalog newCatalog = (Catalog) um.unmarshal(file);
			newCatalog.file = file;
			return newCatalog;
		}
		catch (Exception e)
		{
			throw new JAXBException(e.getMessage(), e);
		}
	}

	/**
	 * This checks whether testName is the name of an existing catalog in
	 * catalogsDirectory.
	 */
	@Deprecated // /W #deprecated
	public static boolean isCatalogsName(String testName)
	{
		boolean bRet = false;
		for (Catalog cTest : getCatalogs())
		{
			if (cTest.getName().equals(testName))
				bRet = true;
		}
		return bRet;
	}

	/**
	 * This checks whether testName is the independent part in the filename of
	 * an existing catalogFile in catalogsDirectory.
	 */
	public static boolean isCatalogsFileNamePart(final String testName)
	{
		if (testName == null) // /W otherwise #firstStart does not work
			return false;
		File testFile = new File(ACSettings.getInstance().getCatalogsDirectory(), getCatalogFileName(testName));
		return Files.exists(testFile.toPath());
	}

	/**
	 * This creates a standard name for a new catalog: YYYYMMDD_currentNumber
	 */
	public static String makeNewCatalogsName()
	{
		Calendar date = new GregorianCalendar();
		String newName = String.format("%4d%02d%02d_", date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DATE));
		int nAppend = 1;
		while (isCatalogsFileNamePart(newName + nAppend))
			nAppend++;
		return newName + nAppend;
	}

	// instance data
	@XmlTransient
	protected File file = null;

	protected String name = null;
	protected int version = 1;

	@XmlElements(
	{ @XmlElement(name = "Layer", type = Layer.class) })
	protected List<IfLayer> layers = new LinkedList<IfLayer>();

	// @XmlElement
	// protected int Num = layers.size();

	/**
	 * Will only be called by {@link #newInstance()}
	 */
	private Catalog()
	{
		name = "new ..."; // /W #???
		log.trace("default constructor catalog() called");
	}

	/**
	 * Load a catalog by it's name
	 * 
	 * @param name
	 */
	// /W #deprecated?
	public Catalog(String catalogName)
	{
		this(new File(ACSettings.getInstance().getCatalogsDirectory(), getCatalogFileName(catalogName)), catalogName);
	}

	// /W #deprecated?
	protected Catalog(File file, String name)
	{
		this.file = file;
		this.name = name;
	}

	public Catalog(Catalog catalog)
	{
		this.file = catalog.file;
		this.name = catalog.name;
		this.version = catalog.version;
		this.layers = catalog.layers;

		log.info("copy constructor catalog(catalog) called");
	}

	public Catalog(IfCatalog ifCatalog)
	{
		this.file = ifCatalog.getFile();
		this.name = ifCatalog.getName();
		this.version = ifCatalog.getVersion();
		// how to copy layers ?
		this.layers = ifCatalog.getLayers();
		log.info("copy constructor catalog(catalog) called");
	}

	@Override
	public void addLayer(IfLayer layer)
	{
		layers.add(layer);
	}

	@Override
	public void deleteLayer(IfLayer layer)
	{
		layers.remove(layer);
	}

	@Override
	public IfLayer getLayer(int index)
	{
		return layers.get(index);
	}

	@Override
	public int getLayerCount()
	{
		return layers.size();
	}

	@Override
	public List<IfLayer> getLayers()
	{
		return layers;
	}

	@XmlAttribute
	public int getSize()
	{
		return getLayerCount();
	}

	public void setSize(int newSize)
	{
	}

	@Override
	@XmlAttribute
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String newName)
	{
		this.name = newName;
	}

	@Override
	public String toString()
	{
		// return getName() + " (" + ")";
		return name; // /W #combobox: toString() sets items in dropdownList! But
		// name is fileNamesPart!!!
	}

	@Override
	public Iterator<IfLayer> iterator()
	{
		return layers.iterator();
	}

	/**
	 * 
	 */
	@Override
	public long calculateTilesToDownload()
	{
		long tiles = 0;

		for (IfLayer layer : layers)
		{
			tiles += layer.calculateTilesToDownload();
		}
		log.trace("catalog=" + getName() + ", tiles=" + tiles);
		return tiles;
	}
	
	/**
	 * This checks for empty catalogs. A catalog is empty, if there is no map with tiles in it.
	 * 
	 * @return 
	 */
	// /W #??? to IFCatalog too?
	public boolean isEmpty()
	{
		for (IfLayer layer : layers)
		{
			for (IfMap map : layer.getMaps())
			{
				if (map.getTileCount() > 0)
					return false;
			}
		}
		return true;
	}

	@Override
	public boolean isInvalid()
	{
		if (name == null) // name set?
			return true;
		if (isEmpty())
			return true;
		// Check for duplicate layer names
		HashSet<String> names = new HashSet<String>(layers.size());
		for (IfLayer layer : layers)
			names.add(layer.getName());
		if (names.size() < layers.size())
			return true; // at least one duplicate name found
		return false;
	}
	
	/////////////////////////////////////////////////////////////
	@Override
	public int getXBorderMin()
	{
		int xMin = Integer.MAX_VALUE;
		for (IfLayer l : layers)
		{
			xMin = Math.min(xMin, l.getXBorderMin());
		}
		return xMin;
	}
	
	@Override
	public int getXBorderMax()
	{
		int xMax = Integer.MIN_VALUE;
		for (IfLayer l : layers)
		{
			xMax = Math.max(xMax, l.getXBorderMax());
		}
		return xMax;
	}
	
	@Override
	public int getYBorderMin()
	{
		int yMin = Integer.MAX_VALUE;
		for (IfLayer l : layers)
		{
			yMin = Math.min(yMin, l.getYBorderMin());
		}
		return yMin;
	}
	
	@Override
	public int getYBorderMax()
	{
		int yMax = Integer.MIN_VALUE;
		for (IfLayer l : layers)
		{
			yMax = Math.max(yMax, l.getYBorderMax());
		}
		return yMax;
	}
	/////////////////////////////////////////////////////////////

	@Override
	public double getMinLat()
	{
		double lat = 90d;
		for (IfLayer l : layers)
		{
			lat = Math.min(lat, l.getMinLat());
		}
		return lat;
	}

	@Override
	public double getMaxLat()
	{
		double lat = -90d;
		for (IfLayer l : layers)
		{
			lat = Math.max(lat, l.getMaxLat());
		}
		return lat;
	}

	@Override
	public double getMinLon()
	{
		double lon = 180d;
		for (IfLayer l : layers)
		{
			lon = Math.min(lon, l.getMinLon());
		}
		return lon;
	}

	@Override
	public double getMaxLon()
	{
		double lon = -180d;
		for (IfLayer l : layers)
		{
			lon = Math.max(lon, l.getMaxLon());
		}
		return lon;
	}

	public String getToolTip()
	{
		StringWriter sw = new StringWriter(1024);
		// sw.write("<html>");
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_bundle_title"));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_bundle_name",
		// StringEscapeUtils.escapeHtml4(name)));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_bundle_layer",
		// layers.size()));
		// sw.write(OSMCBStrs.RStr("lp_bundle_info_bundle_format",
		// outputFormat.toString()));
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
	public Enumeration<?> children()
	{
		return Collections.enumeration(layers);
	}

	@Override
	public boolean getAllowsChildren()
	{
		return true;
	}

	@Override
	public TreeNode getChildAt(int childIndex)
	{
		return layers.get(childIndex);
	}

	@Override
	public int getChildCount()
	{
		return layers.size();
	}

	@Override
	public int getIndex(TreeNode node)
	{
		return layers.indexOf(node);
	}

	@Override
	public TreeNode getParent()
	{
		return null;
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	@XmlAttribute
	public int getVersion()
	{
		return version;
	}

	public void setVersion(int newVersion)
	{
		version = newVersion;
	}

	// @Override
	// public IfCatalog deepClone()
	// {
	// Catalog catalog = new Catalog();
	// catalog.version = version;
	// catalog.name = name;
	// // bundle.outputFormat = outputFormat;
	// for (IfLayer layer : layers)
	// {
	// catalog.layers.add(layer.deepClone(catalog));
	// }
	// return catalog;
	// }

	@Override
	public boolean check()
	{
		boolean bOK = !isInvalid();

		return bOK;
	}

	@Override
	public File getFile()
	{
		return file;
	}

	/**
	 * Checks if the file already exists
	 * 
	 * @see osmb.program.catalog.IFCatalogProfile#exists()
	 */
	@Override
	public boolean exists()
	{
		return file.isFile();
	}

	/**
	 * Deletes the file, if not now, then on exit
	 * 
	 * @see osmb.program.catalog.IfCatalogProfile#delete()
	 */
	@Override
	public void delete()
	{
		if (!file.delete())
			file.deleteOnExit();
	}

	/**
	 * Compares the IFCatalogProfile of this object to the IfCatalogProfile of
	 * the other one. Specifically it compares the files. Currently it does not
	 * compare the names or contents of the catalog.
	 * 
	 * @see osmb.program.catalog.IfCatalogProfile#equals(java.lang.Object)
	 */
	@Override
	public int compareTo(IfCatalogProfile other)
	{
		return file.compareTo(other.getFile());
	}

	/**
	 * Tests if the IFCatalogProfile of this object equals the IfCatalogProfile
	 * of the other one. Specifically it checks if the files are equal.
	 * Currently it does not check if names or contents are equal
	 * 
	 * @see osmb.program.catalog.IfCatalogProfile#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		boolean bEq = false;
		if ((obj != null) && (obj instanceof IfCatalogProfile) && (file != null))
		{
			IfCatalogProfile p = (IfCatalogProfile) obj;
			bEq = file.equals(p.getFile());
		}
		return bEq;
	}

	/**
	 * Saves the catalog profile. This is the description of the catalog, its
	 * layers and maps, but NOT the actual image data.
	 * 
	 * @see osmb.program.catalog.IfCatalogProfile#save(osmb.program.catalog.IfCatalog)
	 */
	@Override
	public void save() throws JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(Catalog.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		FileOutputStream fo = null;
		try
		{
			// /W every catalog will be saved in(to) CatalogsDirectory with filename based on catalogs name.
			// Catalog file member is still needed to discard changes
			file = new File(ACSettings.getInstance().getCatalogsDirectory(), getCatalogFileName(name));
			fo = new FileOutputStream(file);
			m.marshal(this, fo);
		}
		catch (FileNotFoundException e)
		{
			throw new JAXBException(e);
		}
		finally
		{
			OSMBUtilities.closeStream(fo);
		}
	}

	@Override
	public IfCatalog getCatalog()
	{
		return this;
	}
}
