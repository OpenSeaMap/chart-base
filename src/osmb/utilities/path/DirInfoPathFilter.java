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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import osmb.utilities.OSMBUtilities;

/**
 * A {@link DirectoryStream.Filter} returning only directories.
 */
public class DirInfoPathFilter implements DirectoryStream.Filter<Path>
{
	long dirSize = 0;
	int fileCount = 0;

	public DirInfoPathFilter()
	{
	}

	@Override
	public boolean accept(Path tP)
	{
		try
		{
			if (!Files.isDirectory(tP))
			{
				OSMBUtilities.checkForInterruptionRt();
				dirSize += Files.size(tP);
				fileCount++;
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public long getDirSize()
	{
		return dirSize;
	}

	public int getFileCount()
	{
		return fileCount;
	}
}
