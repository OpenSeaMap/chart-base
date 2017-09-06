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

/**
 * This Filter accepts only files ending with a single ext. It is not allowed to specify a list here.
 * 
 * @author humbach
 */
public class PathExtFilter implements DirectoryStream.Filter<Path>
{
	private String acceptedFileExt;

	public PathExtFilter(String acceptedFileExt)
	{
		this.acceptedFileExt = acceptedFileExt;
	}

	@Override
	public boolean accept(Path tP)
	{
		boolean bExtOk = false;
		if (!Files.isDirectory(tP))
		{
			bExtOk = tP.subpath(tP.getNameCount() - 1, tP.getNameCount()).toString().endsWith(acceptedFileExt);
		}
		return bExtOk;
	}
}
