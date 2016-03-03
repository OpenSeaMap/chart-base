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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

//W #mapSpace import osmb.mapsources.IfMapSource.LoadMethod;
//W #mapSpace import osmb.mapsources.mapspace.MapSpaceFactory;
import osmb.program.Logging;
//W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tiles.TileException;
import osmb.program.tiles.TileImageType;
import osmb.utilities.OSMBStrs;
import osmb.utilities.OSMBUtilities;

@XmlRootElement(name = "localTileZip")
public class CustomLocalTileZipMapSource extends CustomLocalMapSource implements IfFileBasedMapSource
{
	private static final Logger log = Logger.getLogger(CustomLocalTileZipMapSource.class);

	private MapSourceLoaderInfo loaderInfo = null;

	// private IfMapSpace mapSpace = MapSpaceFactory.getInstance(256, true); // W #mapSpace =

	private boolean initialized = false;

	private String fileSyntax = null;

	private TileImageType tileImageType = null;

	@XmlElement(nillable = false, defaultValue = "CustomLocal")
	private String name = "Custom";

	@XmlElement(name = "zipFile", required = true)
	private File[] zipFiles = new File[] {};

	@XmlElement()
	private CustomMapSourceType sourceType = CustomMapSourceType.DIR_ZOOM_X_Y;

	@XmlElement(defaultValue = "false")
	private boolean invertYCoordinate = false;

	private LinkedList<ZipFile> zips = new LinkedList<ZipFile>();

	public CustomLocalTileZipMapSource()
	{
		super();
	}

	protected synchronized void openZipFile()
	{
		for (File zipFile : zipFiles)
		{
			if (!zipFile.isFile())
			{
				JOptionPane.showMessageDialog(null, String.format(OSMBStrs.RStr("msg_custom_map_invalid_source_zip_title"), name, zipFile.toString()),
				    OSMBStrs.RStr("msg_custom_map_invalid_source_zip_title"), JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				try
				{
					Logging.LOG.debug("Opening zip file " + zipFile.getAbsolutePath());
					zips.add(new ZipFile(zipFile));
					Logging.LOG.debug("Zip file open completed");
				}
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(null, String.format(OSMBStrs.RStr("msg_custom_map_failed_open_source_zip"), name, zipFile.toString()),
					    OSMBStrs.RStr("msg_custom_map_failed_open_source_zip_title"), JOptionPane.ERROR_MESSAGE);
				}
			}
		}
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
			openZipFile();
			if (zips.size() == 0)
				return;
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

	public synchronized void initializeDirType()
	{
		int min = 18;
		int max = 0;
		for (ZipFile zip : zips)
		{
			for (int z = 18; z > 0; z--)
			{
				ZipEntry entry = zip.getEntry(Integer.toString(z) + "/");
				if (entry != null)
				{
					max = Math.max(max, z);
					break;
				}
			}
			for (int z = 0; z < 18; z++)
			{
				ZipEntry entry = zip.getEntry(Integer.toString(z) + "/");
				if (entry != null)
				{
					min = Math.min(min, z);
					break;
				}
			}
		}
		Enumeration<? extends ZipEntry> entries = zips.get(0).entries();
		String syntax = "%d/%d/%d";
		while (entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			if (entry.isDirectory())
				continue;
			String name = entry.getName();
			int i = name.lastIndexOf("/");
			name = name.substring(i + 1);

			String[] parts = name.split("\\.");
			if (parts.length < 2 || parts.length > 3)
				break;
			syntax += "." + parts[1];
			tileImageType = TileImageType.getTileImageType(parts[1]);
			if (parts.length == 3)
				syntax += "." + parts[2];
			fileSyntax = syntax;
			log.debug("Detected file syntax: " + fileSyntax + " tileImageType=" + tileImageType);
			break;
		}
	}

	public synchronized void initializeQuadKeyType()
	{
		Pattern p = Pattern.compile("([0123]+)\\.(png|gif|jpg)", Pattern.CASE_INSENSITIVE);
		Enumeration<? extends ZipEntry> entries = zips.get(0).entries();
		String fileExt = null;
		while (entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			Matcher m = p.matcher(entry.getName());
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

		for (ZipFile zipFile : zips)
		{
			entries = zipFile.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
				Matcher m = p.matcher(entry.getName());
				if (!m.matches())
					continue;
				if (fileSyntax == null)
					fileSyntax = "%s." + m.group(2);
				int z = m.group(1).length();
				min = Math.min(min, z);
				max = Math.max(max, z);
			}
		}
	}

	@Override
	public byte[] loadTileData(TileAddress tAddr)
	{
		String fileName = "----";
		if (!initialized)
			initialize();
		if (fileSyntax == null)
			return null;
		if (log.isTraceEnabled())
			log.trace(String.format("Loading tile %s", tAddr));

		try
		{
			// if (invertYCoordinate)
			// y = ((1 << zoom) - y - 1);
			ZipEntry entry = null;
			switch (sourceType)
			{
				case DIR_ZOOM_X_Y:
					fileName = String.format(fileSyntax, tAddr);
					break;
				case DIR_ZOOM_Y_X:
					fileName = String.format(fileSyntax, tAddr);
					break;
				case QUADKEY:
					fileName = String.format(fileSyntax, MapSourceTools.encodeQuadTree(tAddr));
					break;
				default:
					throw new RuntimeException("Invalid source type");
			}
			for (ZipFile zip : zips)
			{
				entry = zip.getEntry(fileName);
				if (entry != null)
				{
					InputStream in;
					in = zip.getInputStream(entry);
					byte[] data = OSMBUtilities.getInputBytes(in);
					in.close();
					return data;
				}
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("Map tile file not found in zip files: " + fileName);
		return null;
	}

	@Override
	public BufferedImage getTileImage(int zoom, int x, int y) throws IOException, TileException, InterruptedException
	{
		byte[] data = getTileData(zoom, x, y);
		if (data == null)
			return null;
		return ImageIO.read(new ByteArrayInputStream(data));
	}
}
