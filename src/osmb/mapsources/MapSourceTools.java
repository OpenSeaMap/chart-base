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

//W #mapSpace import osmb.program.map.IfMapSpace;

/**
 * Utility methods used by several map sources.
 */
public class MapSourceTools
{
	protected static final char[] NUM_CHAR =
	{ '0', '1', '2', '3' };

	/**
	 * See: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param zoom
	 * @param tilex
	 * @param tiley
	 * @return quadtree encoded tile number
	 * 
	 */
	// public static String encodeQuadTree(int zoom, int tilex, int tiley)
	public static String encodeQuadTree(TileAddress tAddr)
	{
		char[] tileNum = new char[tAddr.getZoom()];
		int x = tAddr.getX();
		int y = tAddr.getY();
		for (int i = tAddr.getZoom() - 1; i >= 0; i--)
		{
			// Binary encoding using ones for tilex and twos for tiley. If a bit is set in tilex and tiley we get a three.
			int num = (x % 2) | ((y % 2) << 1);
			tileNum[i] = NUM_CHAR[num];
			x >>= 1;
			y >>= 1;
		}
		return new String(tileNum);
	}

	/**
	 * Calculates latitude and longitude of the four borders of the specified tile of <code>mapsource</code> regarding the zoom level specified by
	 * <code>zoom</code>.
	 * 
	 * @param mapSource
	 * @param zoom
	 * @param tilex
	 *          horizontal tile number
	 * @param tiley
	 *          vertical tile number
	 * @return <code>double[] {lon_min: west, lat_min: south, lon_max: east, lat_max: north}</code>
	 */
	public static double[] calculateLatLon(int zoom, int tilex, int tiley)
	{
		int tileSize = MP2MapSpace.getTileSize();
		double[] result = new double[4];
		tilex *= tileSize;
		tiley *= tileSize;
		result[0] = MP2MapSpace.cXToLonLeftBorder(tilex, zoom);
		result[1] = MP2MapSpace.cYToLatLowerBorder(tiley, zoom);
		result[2] = MP2MapSpace.cXToLonRightBorder(tilex, zoom);
		result[3] = MP2MapSpace.cYToLatUpperBorder(tiley, zoom);
		return result;
	}

	public static String formatMapUrl(String mapUrl, TileAddress tAddr)
	{
		String tmp = mapUrl;
		tmp = tmp.replace("{$x}", Integer.toString(tAddr.getX()));
		tmp = tmp.replace("{$y}", Integer.toString(tAddr.getY()));
		tmp = tmp.replace("{$z}", Integer.toString(tAddr.getZoom()));
		tmp = tmp.replace("{$q}", MapSourceTools.encodeQuadTree(tAddr));
		return tmp;
	}

	public static String formatMapUrl(String mapUrl, int serverNum, TileAddress tAddr)
	{
		String tmp = mapUrl;
		tmp = tmp.replace("{$servernum}", Integer.toString(serverNum));
		return formatMapUrl(tmp, tAddr);
	}

	public static String formatMapUrl(String mapUrl, String serverPart, TileAddress tAddr)
	{
		String tmp = mapUrl;
		tmp = tmp.replace("{$serverpart}", serverPart);
		return formatMapUrl(tmp, tAddr);
	}
}
