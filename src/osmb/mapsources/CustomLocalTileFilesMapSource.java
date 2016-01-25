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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;

//W #mapSpace import osmb.mapsources.IfMapSource.LoadMethod;
//W #mapSpace import osmb.mapsources.mapspace.MapSpaceFactory;
import osmb.program.jaxb.ColorAdapter;
//W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.utilities.OSMBStrs;
import osmb.utilities.OSMBUtilities;

//import osmcd.gui.mapview.PreviewMap;
//W #mapSpace MP2MapSpace EastNorthCoordinate <-> GeoCoordinate MP2Corner <-> MercatorPixelCoordinate
@XmlRootElement(name = "localTileFiles")
public class CustomLocalTileFilesMapSource implements IfFileBasedMapSource
{
	private static final Logger log = Logger.getLogger(CustomLocalTileFilesMapSource.class);

	private MapSourceLoaderInfo loaderInfo = null;

	// W #mapSpace
	// private IfMapSpace mapSpace = MapSpaceFactory.getInstance(256, true); // W #mapSpace =

	private boolean initialized = false;

	private String fileSyntax = null;

	private TileImageType tileImageType = null;

	@XmlElement(nillable = false, defaultValue = "CustomLocal")
	private String name = "Custom";

	private int minZoom = 0;

	private int maxZoom = 18;

	@XmlElement(required = true)
	private File sourceFolder = null;

	@XmlElement()
	private CustomMapSourceType sourceType = CustomMapSourceType.DIR_ZOOM_X_Y;

	@XmlElement(defaultValue = "false")
	private boolean invertYCoordinate = false;

	@XmlElement(defaultValue = "#000000")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	private Color backgroundColor = Color.BLACK;

	public CustomLocalTileFilesMapSource()
	{
		super();
	}

	@Override
	public synchronized void initialize()
	{
		if (initialized)
			return;
		reinitialize();
	}

	@Override
	public void reinitialize()
	{
		try
		{
			if (!sourceFolder.isDirectory())
			{
				JOptionPane.showMessageDialog(null, String.format(OSMBStrs.RStr("msg_environment_invalid_source_folder"), name, sourceFolder.toString()),
				    OSMBStrs.RStr("msg_environment_invalid_source_folder_title"), JOptionPane.ERROR_MESSAGE);
				initialized = true;
				return;
			}
			switch (sourceType)
			{
				case DIR_ZOOM_X_Y:
				case DIR_ZOOM_Y_X:
					initializeDirType();
					break;
				case QUADKEY:
					initializeQuadKeyType();
					break;
				default:
					throw new RuntimeException("Invalid source type");
			}
		}
		finally
		{
			initialized = true;
		}
	}

	private void initializeDirType()
	{
		/* Update zoom levels */
		FileFilter ff = new NumericDirFileFilter();
		File[] zoomDirs = sourceFolder.listFiles(ff);
		if (zoomDirs.length < 1)
		{
			JOptionPane.showMessageDialog(null, String.format(OSMBStrs.RStr("msg_environment_invalid_source_folder_zoom"), name, sourceFolder),
			    OSMBStrs.RStr("msg_environment_invalid_source_folder_title"), JOptionPane.ERROR_MESSAGE);
			initialized = true;
			return;
		}
		int min = 18;
		int max = 0;
		for (File file : zoomDirs)
		{
			int z = Integer.parseInt(file.getName());
			min = Math.min(min, z);
			max = Math.max(max, z);
		}
		minZoom = min;
		maxZoom = max;

		for (File zDir : zoomDirs)
		{
			for (File xDir : zDir.listFiles(ff))
			{
				try
				{
					xDir.listFiles(new FilenameFilter()
					{

						String syntax = "%d/%d/%d";

						@Override
						public boolean accept(File dir, String name)
						{
							String[] parts = name.split("\\.");
							if (parts.length < 2 || parts.length > 3)
								return false;
							syntax += "." + parts[1];
							if (parts.length == 3)
								syntax += "." + parts[2];
							tileImageType = TileImageType.getTileImageType(parts[1]);
							fileSyntax = syntax;
							log.debug("Detected file syntax: " + fileSyntax + " tileImageType=" + tileImageType);
							throw new RuntimeException("break");
						}
					});
				}
				catch (RuntimeException e)
				{
				}
				catch (Exception e)
				{
					log.error(e.getMessage());
				}
				return;
			}
		}
	}

	private void initializeQuadKeyType()
	{
		String[] files = sourceFolder.list();
		Pattern p = Pattern.compile("([0123]+)\\.(png|gif|jpg)", Pattern.CASE_INSENSITIVE);
		String fileExt = null;
		for (String file : files)
		{
			Matcher m = p.matcher(file);
			if (!m.matches())
				continue;
			fileExt = m.group(2);
			break;
		}
		if (fileExt == null)
			return; // Error no suitable file found
		fileSyntax = "%s." + fileExt;

		tileImageType = TileImageType.getTileImageType(fileExt);
		p = Pattern.compile("([0123]+)\\.(" + fileExt + ")", Pattern.CASE_INSENSITIVE);

		int min = 18;
		int max = 1;

		for (String file : files)
		{
			Matcher m = p.matcher(file);
			if (!m.matches())
				continue;
			if (fileSyntax == null)
				fileSyntax = "%s." + m.group(2);
			int z = m.group(1).length();
			min = Math.min(min, z);
			max = Math.max(max, z);
		}
		minZoom = min;
		maxZoom = max;
	}

	@Override
	public byte[] getTileData(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException
	{
		if (!initialized)
			initialize();
		if (fileSyntax == null)
			return null;
		if (log.isTraceEnabled())
			log.trace(String.format("Loading tile z=%d x=%d y=%d", zoom, x, y));

		if (invertYCoordinate)
			y = ((1 << zoom) - y - 1);
		String fileName;
		switch (sourceType)
		{
			case DIR_ZOOM_X_Y:
				fileName = String.format(fileSyntax, zoom, x, y);
				break;
			case DIR_ZOOM_Y_X:
				fileName = String.format(fileSyntax, zoom, y, x);
				break;
			case QUADKEY:
				fileName = String.format(fileSyntax, MapSourceTools.encodeQuadTree(zoom, x, y));
				break;
			default:
				throw new RuntimeException("Invalid source type");
		}
		File file = new File(sourceFolder, fileName);
		try
		{
			return OSMBUtilities.getFileBytes(file);
		}
		catch (FileNotFoundException e)
		{
			log.debug("Map tile file not found: " + file.getAbsolutePath());
			return null;
		}
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y, LoadMethod loadMethod) throws IOException, TileException, InterruptedException
	{
		byte[] data = getTileData(zoom, x, y, loadMethod);
		if (data == null)
			return null;
		return ImageIO.read(new ByteArrayInputStream(data));
	}

	@Override
	public TileImageType getTileImageType()
	{
		return tileImageType;
	}

	@Override
	public int getMaxZoom()
	{
		return maxZoom;
	}

	@Override
	public int getMinZoom()
	{
		return minZoom;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	// W #mapSpace MP2MapSpace EastNorthCoordinate <-> GeoCoordinate MP2Corner <-> MercatorPixelCoordinate
	// public IfMapSpace getMapSpace()
	// {
	// return mapSpace;
	// }

	@Override
	public Color getBackgroundColor()
	{
		return backgroundColor;
	}

	@Override
	@XmlTransient
	public MapSourceLoaderInfo getLoaderInfo()
	{
		return loaderInfo;
	}

	@Override
	public void setLoaderInfo(MapSourceLoaderInfo loaderInfo)
	{
		this.loaderInfo = loaderInfo;
	}

	private static class NumericDirFileFilter implements FileFilter
	{

		private Pattern p = Pattern.compile("^\\d+$");

		@Override
		public boolean accept(File f)
		{
			if (!f.isDirectory())
				return false;
			return p.matcher(f.getName()).matches();
		}
	}

	@Override
	public BufferedImage downloadTileImage(int zoom, int x, int y) throws IOException, TileException, InterruptedException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
