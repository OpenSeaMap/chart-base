package osmb.utilities.path;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * - element of a TreeSet of directory entries, newest (last modified) first
 * 
 * @author humbach
 */
public class DirEntry implements Comparable<DirEntry>
{
	Path mtDEPath;
	FileTime mtDEDate;

	public void SetName(Path path)
	{
		mtDEPath = path;
	}

	public String GetPathStr()
	{
		return mtDEPath.toString();
	}

	public Path GetPath()
	{
		return mtDEPath;
	}

	public void SetDate(FileTime date)
	{
		mtDEDate = date;
	}

	public FileTime GetDate()
	{
		return mtDEDate;
	}

	public String GetDateStr()
	{
		return mtDEDate.toString();
	}

	@Override
	public String toString()
	{
		return GetPathStr() + ", " + GetDateStr();
	}

	@Override
	public int compareTo(DirEntry tDE)
	{
		return -1 * mtDEDate.compareTo(tDE.mtDEDate);
	}
}
