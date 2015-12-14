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

import java.awt.Point;
import java.util.LinkedList;

import javax.swing.tree.TreeNode;

import osmb.exceptions.InvalidNameException;
import osmb.mapsources.IfMapSource;
import osmb.program.catalog.IfCapabilityDeletable;
import osmb.program.catalog.IfCatalog;
import osmb.program.catalog.IfCatalogObject;
import osmb.program.tiles.TileImageParameters;
//W #mapSpace import osmb.utilities.geo.EastNorthCoordinate;

public interface IfLayer extends IfCatalogObject, Iterable<IfMap>, IfCapabilityDeletable, TreeNode
{
	void addMap(IfMap map);

	int getMapCount();

	IfMap getMap(int index);

	LinkedList<IfMap> getMaps();

	IfLayer deepClone(IfCatalog catalog);

	int getZoomLvl();

	void setZoomLvl(int nZoomLvl);
	
//W #mapSpace ??? EastNorthCoordinate <-> GeoCoordinate
// unused!!!
//	void addMapsAutocut(String mapNameBase, IfMapSource mapSource, EastNorthCoordinate minCoordinate, EastNorthCoordinate maxCoordinate, int zoom,
//	    TileImageParameters parameters, int maxMapSize) throws InvalidNameException;

	void addMapsAutocut(String mapNameBase, IfMapSource mapSource, Point minPixelCoordinate, Point maxPixelCoordinate, int zoom, TileImageParameters parameters,
	    int maxMapSize, int overlapTiles) throws InvalidNameException;
}
