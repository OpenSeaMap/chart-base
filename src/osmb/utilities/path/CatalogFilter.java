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
package osmb.utilities.path;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import osmb.program.catalog.Catalog;

/**
 * This Filter accepts only files following the scheme: "osmcb-catalog-NAME.xml".
 * 
 * @author humbach
 */
public class CatalogFilter implements DirectoryStream.Filter<Path>
{
	@Override
	public boolean accept(Path tP)
	{
		boolean bExtOk = false;
		if (!Files.isDirectory(tP))
		{
			bExtOk = tP.subpath(tP.getNameCount() - 1, tP.getNameCount()).toString().startsWith(Catalog.CATALOG_FILENAME_PREFIX)
			    && tP.subpath(tP.getNameCount() - 1, tP.getNameCount()).toString().endsWith("xml");
		}
		return bExtOk;
	}
}
