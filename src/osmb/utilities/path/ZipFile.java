package osmb.utilities.path;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class ZipFile
{
	protected Path mPath;

	static Path newZipFilePath(String strName)
	{
		Path newPath = null;
		return newPath;
	}

	public ZipFile(String strName)
	{
		URI zipURI = URI.create("jar:file:/" + strName);
		FileSystem zipfs;
		try
		{
			zipfs = FileSystems.newFileSystem(zipURI, null);
			mPath = zipfs.getPath(".");
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Path getPath()
	{
		return this.mPath;
	}
}
