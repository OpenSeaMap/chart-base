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
package osmb.utilities.geo;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.apache.log4j.Logger;

import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.map.IfMapSpace;

public class CoordinateTileFormat extends NumberFormat
{
	protected static Logger log = Logger.getLogger(CoordinateTileFormat.class);

	private final boolean isLongitude;
	// /W #selCoord MainFrame.getMainGUI().previewMap.getGridZoom() (0 to 18) || osmcd.gui.mapview.JMapViewer.MAX_ZOOM (22)
	private static int nActZoom; 

	public CoordinateTileFormat(boolean isLongitude)
	{
		this.isLongitude = isLongitude;
	}

	// /W #selCoord
	public static void setActZoom(int zoom)
	{
		nActZoom = zoom;
	}
	
	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
	{
		IfMapSpace mapSpace = MercatorPower2MapSpace.INSTANCE_256;
		// int zoom = 1;  // /W #selCoord zoom -> nActZoom
		int tileNum = 0;
		if (isLongitude)
			tileNum = mapSpace.cLonToX(number, nActZoom);
		else
			tileNum = mapSpace.cLatToY(number, nActZoom);
		toAppendTo.append(String.format("%d / z%d ", tileNum / mapSpace.getTileSize(), nActZoom));
		// /W #selCoord test: toAppendTo.append(String.format("%d / z%d ", tileNum, nActZoom));
		return toAppendTo;
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
	{
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition)
	{
		IfMapSpace mapSpace = MercatorPower2MapSpace.INSTANCE_256;
		try
		{
			String[] tokens = source.trim().split("/");
			int zoom = 0;
			int tileNum = 0;
			if (tokens.length == 2)
			{
				String s = tokens[1].trim();
				if (s.startsWith("z"))
					s = s.substring(1);
				zoom = Integer.parseInt(s);
			}
			else
			{
				zoom = 1;
			}
			if (tokens.length > 0)
			{
				String s = tokens[0];
				s = s.trim();
				if ((s.indexOf('.') < 0) && (s.indexOf(',') < 0))
				{
					tileNum = Integer.parseInt(s);
					tileNum *= mapSpace.getTileSize();
				}
				else
				{
					double num = Double.parseDouble(s);
					tileNum = (int) (num * mapSpace.getTileSize());
				}
			}
			parsePosition.setIndex(source.length());
			if (isLongitude)
				return mapSpace.cXToLon(tileNum, zoom);
			return mapSpace.cYToLat(tileNum, zoom);
		}
		catch (Exception e)
		{
			parsePosition.setErrorIndex(0);
			log.error("e");
			return null;
		}
	}
}
