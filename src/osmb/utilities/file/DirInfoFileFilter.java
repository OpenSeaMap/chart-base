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
package osmb.utilities.file;

import java.io.File;
import java.io.FileFilter;
import osmb.utilities.OSMBUtilities;

public class DirInfoFileFilter implements FileFilter
{
	long dirSize = 0;
	int fileCount = 0;

	public DirInfoFileFilter()
	{}

	@Override
	public boolean accept(File f)
	{
		if (f.isDirectory())
			return false;
		OSMBUtilities.checkForInterruptionRt();
		dirSize += f.length();
		fileCount++;
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
