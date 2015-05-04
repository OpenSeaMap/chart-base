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

import java.awt.Color;
import java.awt.Font;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import osmb.program.WgsGrid.WgsDensity;
import osmb.program.jaxb.ColorAdapter;

public class WgsGridSettings implements Cloneable
{
	public static final Color DEFAULT_COLOR = Color.BLUE;
	public static final WgsDensity DEFAULT_DENSITY = WgsDensity.SECOND_1;
	public static final Font DEFAULT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	public static final float WIDTH_DEFAULT = 1f;
	public static final float WIDTH_MIN = 0.5f;
	public static final float WIDTH_MAX = 5f;

	@XmlElement(defaultValue = "#0000FF")
	@XmlJavaTypeAdapter(ColorAdapter.class)
	public Color color = DEFAULT_COLOR;

	public boolean compressLabels = false;
	public WgsDensity density = DEFAULT_DENSITY;
	public boolean enabled = false;

	// @XmlElement(defaultValue = "SansSerif-PLAIN-12")
	// @XmlJavaTypeAdapter(FontAdapter.class)
	// public Font font = DEFAULT_FONT;

	public float width = WIDTH_DEFAULT;

	@Override
	public WgsGridSettings clone()
	{
		try
		{
			return (WgsGridSettings) super.clone();
		}
		catch (Exception e)
		{
			return new WgsGridSettings();
		}
	}

	public void checkValues()
	{
		if (width < WIDTH_MIN || width > WIDTH_MAX)
		{
			width = WIDTH_DEFAULT;
		}
		if (density == null)
		{
			density = DEFAULT_DENSITY;
		}
		if (color == null)
		{
			color = DEFAULT_COLOR;
		}
	}
}
