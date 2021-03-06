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
package osmb.program;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.JComponent;

import osmb.mapsources.MP2MapSpace;
//W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.utilities.OSMBStrs;
import osmb.utilities.geo.Coordinate;

public class WgsGrid
{
	public static enum WgsDensity
	{
		DEGREES_90(0), DEGREES_45(1), DEGREES_30(2), DEGREES_15(3), DEGREES_10(4), DEGREES_5(5), DEGREES_2(6), DEGREE_1(7), MINUTES_30(8), MINUTES_20(9), MINUTES_10(
				10), MINUTES_5(11), MINUTES_2(12), MINUTE_1(13), SECONDS_30(15), SECONDS_20(15), SECONDS_10(16), SECONDS_5(17), SECONDS_2(18), SECOND_1(19);

		public final int iStep, minZoom;
		public final boolean compressDegree, compressMinute, displayMinute, displaySecond;

		// private final String string;

		private WgsDensity(final int minZoom)
		{
			this.minZoom = minZoom;
			String[] split = name().split("_");
			int value = Integer.parseInt(split[1]);

			if (split[0].startsWith("D"))
			{
				iStep = value * Coordinate.DEGREE;
				displayMinute = displaySecond = false;
				compressDegree = compressMinute = false;
			}
			else if (split[0].startsWith("M"))
			{
				iStep = value * Coordinate.MINUTE;
				compressDegree = true/* value <= 15 */;
				displayMinute = true;
				displaySecond = compressMinute = false;
			}
			else
			{
				iStep = value * Coordinate.SECOND;
				compressDegree = displayMinute = displaySecond = true;
				compressMinute = true/* value <= 15 */;
			}
		}

		@Override
		public String toString()
		{
			String[] split = name().split("_");
			String unitKey = "map_ctrl_wgs_grid_density_" + split[0].toLowerCase();
			return OSMBStrs.RStr("map_ctrl_wgs_grid_density_prefix") + " " + split[1] + " " + OSMBStrs.RStr(unitKey);
		}
	}

	public static enum Placement
	{
		BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT
	}

	private static final WgsDensity[] DENSITIES = WgsDensity.values();
	private static final Stroke BASIC_STROKE = new BasicStroke(1f);
	private static final int LABEL_OFFSET = 2;

	private final StringBuilder stringBuilder = new StringBuilder(16);
	private final Rectangle viewport = new Rectangle();
	public final WgsGridSettings s;
	private final JComponent c;

	private Placement placement = Placement.BOTTOM_RIGHT;
	private BasicStroke stroke;
	private int lastDegree, lastMinute;

	public WgsGrid(WgsGridSettings s, JComponent c)
	{
		this.s = s;
		this.c = c;
	}

	public void paintWgsGrid(Graphics2D g, Point tlc, int zoom) // W #mapSpace (Graphics2D g, IfMapSpace ms, Point tlc, int zoom)
	{
		if (!s.enabled)
		{
			return;
		}

		// Check density
		WgsDensity density = s.density;
		while (zoom < density.minZoom)
		{
			int index = density.ordinal();
			if (--index <= 0)
			{
				return;
			}
			density = DENSITIES[index];
		}

		final int maxPixels = MP2MapSpace.getSizeInPixel(zoom);

		// Check viewport
		viewport.width = c.getWidth();
		viewport.height = c.getHeight();
		if (tlc.x > maxPixels || tlc.y > maxPixels || tlc.x + viewport.width < 0 || tlc.y + viewport.width < 0)
		{
			return;
		}
		viewport.x = c.getX();
		viewport.y = c.getY();

		// Translate according to mapSpace
		viewport.translate(tlc.x, tlc.y);
		g = (Graphics2D) g.create();
		g.translate(-tlc.x, -tlc.y);

		// Calculate viewport coordinates
		final int x1 = Math.max(tlc.x, 0);
		final int y1 = Math.max(tlc.y, 0);
		final int x2 = Math.min(tlc.x + viewport.width, maxPixels);
		final int y2 = Math.min(tlc.y + viewport.height, maxPixels);

		// Calculate line coordinates
		final int hLineX1 = x1 + 1;
		final int hLineX2 = x2 - 1;
		final int vLineY1 = y1 + 1;
		final int vLineY2 = y2 - 1;

		// Calculate line indexes
		final int vMin = Coordinate.doubleToInt(MP2MapSpace.cXToLonLeftBorder(x1, zoom)) / density.iStep; // W #mapSpace (ms.cXToLon(x1, zoom)) / density.iStep;
		final int vMax;
		if (x2 == maxPixels) // W 180d-problem
			vMax = Coordinate.doubleToInt(MP2MapSpace.cXToLonRightBorder(x2, zoom)) / density.iStep;
		else
			vMax = Coordinate.doubleToInt(MP2MapSpace.cXToLonLeftBorder(x2, zoom)) / density.iStep; // W #mapSpace (ms.cXToLon(x2, zoom)) / density.iStep;
		final int hMin = Coordinate.doubleToInt(MP2MapSpace.cYToLatUpperBorder(y2, zoom)) / density.iStep; // W #mapSpace (ms.cYToLat(y2, zoom)) / density.iStep;
		final int hMax = Coordinate.doubleToInt(MP2MapSpace.cYToLatUpperBorder(y1, zoom)) / density.iStep; // W #mapSpace (ms.cYToLat(y1, zoom)) / density.iStep;

		g.setBackground(Color.WHITE);
		g.setColor(s.color);

		g.setStroke(checkAndGetStroke());
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Paint vertical lines
		for (int i = vMin; i <= vMax; i++)
		{
			int iLon = i * density.iStep;
			int x;
			if (iLon == 64800000) // W 180d-problem: 180° * 360000 milliseconds/° = 64800000 milliseconds
				x = x2; // (180d: no line to Paint!!!)
			else
			  // W #mapSpace cLonToXIndex!
			  x = MP2MapSpace.cLonToXIndex(Coordinate.intToDouble(iLon), zoom); // W #mapSpace ms.cLonToX(Coordinate.intToDouble(iLon), zoom);
			if (x > x1 && x < x2) // (180d: no line to Paint!!!)
			{
				g.drawLine(x, vLineY1, x, vLineY2);
			}
		}

		// Paint horizontal lines
		for (int i = hMin; i <= hMax; i++)
		{
			int iLat = i * density.iStep;
			// W #mapSpace cLatToYIndex (MIN_LAT not needed (-85.05112877980659... = 85°3'4.0636...")
			int y = MP2MapSpace.cLatToYIndex(Coordinate.intToDouble(iLat), zoom); // W #mapSpace ms.cLatToY(Coordinate.intToDouble(iLat), zoom);
			if (y > y1 && y < y2)
			{
				g.drawLine(hLineX1, y, hLineX2, y);
			}
		}

		// Set up font metrics
		g.setStroke(BASIC_STROKE);
		final FontMetrics fontMetrics = g.getFontMetrics();
		final int fontDescent = fontMetrics.getDescent();
		final int fontHeight = fontMetrics.getHeight();

		resetLabelCompression();

		// Shared Y coordinates for vertical labels
		int labelRectX;
		int labelRectY = y2 - LABEL_OFFSET - fontHeight;
		int labelY = y2 - LABEL_OFFSET - fontDescent;
		int labelX;

		// Paint vertical labels
		for (int i = vMin; i <= vMax; i++)
		{
			// Calculate coordinates
			int iLon = i * density.iStep;
			int x;
			if (iLon == 64800000) // W 180d-problem: 180° * 360000 milliseconds/° = 64800000 milliseconds
				x = maxPixels;
			else
				x = MP2MapSpace.cLonToXIndex(Coordinate.intToDouble(iLon), zoom); // W #mapSpace ms.cLonToX(Coordinate.intToDouble(iLon), zoom);

			// Prepare label
			String label = getLabel(iLon, density);
			int stringWidth = fontMetrics.stringWidth(label);
			labelRectX = labelX = x - stringWidth / 2;

			// Paint label
			if (viewport.contains(labelRectX, labelRectY, stringWidth, fontHeight))
			{
				g.clearRect(labelRectX, labelRectY, stringWidth, fontHeight);
				g.drawRect(labelRectX, labelRectY, stringWidth, fontHeight);
				g.drawString(label, labelX, labelY);
			}
			else
			{
				resetLabelCompression();
			}
		}

		resetLabelCompression();
		viewport.height -= fontHeight + LABEL_OFFSET;
		labelX = labelRectX = x1 + LABEL_OFFSET;

		// Paint horizontal labels
		for (int i = hMin; i <= hMax; i++)
		{
			int iLat = i * density.iStep;
			// W #mapSpace cLatToYIndex (MIN_LAT not needed (-85.05112877980659... = 85°3'4.0636...")
			int y = MP2MapSpace.cLatToYIndex(Coordinate.intToDouble(iLat), zoom);
			// Prepare label
			String label = getLabel(iLat, density);
			final int stringWidth = fontMetrics.stringWidth(label);
			if (placement == Placement.BOTTOM_RIGHT || placement == Placement.TOP_RIGHT)
			{
				labelX = labelRectX = x2 - LABEL_OFFSET - stringWidth;
			}
			labelRectY = y - fontHeight / 2;
			labelY = labelRectY + fontHeight - fontDescent;

			// Paint label
			if (viewport.contains(labelRectX, labelRectY, stringWidth, fontHeight))
			{
				g.clearRect(labelRectX, labelRectY, stringWidth, fontHeight);
				g.drawRect(labelRectX, labelRectY, stringWidth, fontHeight);
				g.drawString(label, labelX, labelY);
			}
			else
			{
				resetLabelCompression();
			}
		}
		g.dispose();
	}

	public void setPosition(Placement placement)
	{
		this.placement = placement != null ? placement : Placement.BOTTOM_RIGHT;
	}

	private Stroke checkAndGetStroke()
	{
		if (stroke == null || stroke.getLineWidth() != s.width)
		{
			stroke = new BasicStroke(s.width);
		}
		return stroke;
	}

	private void resetLabelCompression()
	{
		lastDegree = Integer.MAX_VALUE;
		lastMinute = Integer.MAX_VALUE;
	}

	private String getLabel(int coord, WgsDensity density)
	{
		float signum = Math.signum(coord);
		coord = Math.abs(coord);
		int degree = Coordinate.getDegree(coord);
		int minute = Coordinate.getMinute(coord);
		int second = Coordinate.getSecond(coord);
		stringBuilder.setLength(0);
		stringBuilder.append(" ");
		if (signum < 0) // add minus sign
			stringBuilder.append("-");
		if (!s.compressLabels || lastDegree != degree || !density.compressDegree)
		{
			stringBuilder.append(degree);
			stringBuilder.append('\u00B0');
		}
		if (density.displayMinute && (!s.compressLabels || lastMinute != minute || !density.compressMinute))
		{
			if (minute < 10)
			{
				stringBuilder.append('0');
			}
			stringBuilder.append(minute);
			stringBuilder.append('\'');
		}
		if (density.displaySecond)
		{
			if (second < 10)
			{
				stringBuilder.append('0');
			}
			stringBuilder.append(second);
			stringBuilder.append('\"');
		}
		stringBuilder.append(" ");
		lastDegree = degree;
		lastMinute = minute;
		return stringBuilder.toString();
	}
}
