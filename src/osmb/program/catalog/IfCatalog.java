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
import java.util.List;

import javax.xml.bind.JAXBException;

import osmb.program.map.IfLayer;

/**
 * to enable typing of catalog objects
 * 
 * @author humbach
 *
 */
public interface IfCatalog extends IfCatalogObject, Iterable<IfLayer>
// public interface IfCatalog extends Iterable<IfLayer>
{
	/**
	 * @return Number of layers in this catalog
	 */
	int getLayerCount();

	IfLayer getLayer(int index);

	/**
	 * has to be a @XmlAttribute
	 * 
	 * @return version number
	 */
	int getVersion();

	List<IfLayer> getLayers();

	// IfCatalog deepClone();
	//
	boolean check();

	// ? public boolean isEmpty();

	void addLayer(IfLayer l);

	void deleteLayer(IfLayer l);

	void save() throws JAXBException;

	File getFile();

	long calcMapsToCompose();
}
