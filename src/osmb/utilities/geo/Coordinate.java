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

/**
 * This class provides conversion methods for geographic coordinates.<br>
 * 
 * Double values in degrees to integer values in milliseconds and the other way round.
 *
 */
// W rename to CoordinateMillisecond???
public class Coordinate
{

	public static final int MILLISECOND = 1, SECOND = MILLISECOND * 1000, MINUTE = SECOND * 60, DEGREE = MINUTE * 60;

	/**
	 * @param value
	 *          Latitude or longitude as double in degrees.
	 * @return Value as integer in milliseconds.
	 */
	public static int doubleToInt(double value)
	{
		int degree = (int) value;
		int minute = (int) (value = (value - degree) * 60d);
		int second = (int) (value = (value - minute) * 60d);
		int millisecond = (int) (value = (value - second) * 1000d);
		return degree * DEGREE + minute * MINUTE + second * SECOND + millisecond * MILLISECOND;
	}

	/**
	 * @param value
	 *          Latitude or longitude as integer in milliseconds.
	 * @return Value as double in degrees.
	 */
	public static double intToDouble(int value)
	{
		double degree = value / DEGREE;
		double minute = (value = value % DEGREE) / MINUTE;
		double second = (value %= MINUTE) / SECOND;
		double millisecond = (value %= SECOND) / MILLISECOND;
		return degree + minute / 60d + second / 3600d + millisecond / 3600000d;
	}

	/**
	 * @param value
	 *          Latitude or longitude as integer in milliseconds.
	 * @return The degree portion of value as integer.
	 */
	public static int getDegree(int value)
	{
		return value / DEGREE;
	}

	/**
	 * @param value
	 *          Latitude or longitude as integer in milliseconds.
	 * @return The minute portion of value as integer.
	 */
	public static int getMinute(int value)
	{
		return Math.abs(value) % DEGREE / MINUTE;
	}

	/**
	 * @param value
	 *          Latitude or longitude as integer in milliseconds.
	 * @return The second portion of value as integer.
	 */
	public static int getSecond(int value)
	{
		return Math.abs(value) % MINUTE / SECOND;
	}

	/**
	 * @param value
	 *          Latitude or longitude as integer in milliseconds.
	 * @return The millisecond portion of value as integer.
	 */
	public static int getMillisecond(int value)
	{
		return Math.abs(value) % SECOND / MILLISECOND;
	}
}
