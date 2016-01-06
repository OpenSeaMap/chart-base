package osmb.utilities.lzma;

import java.io.IOException;

public class OsmbLzma
{
	static public void encode7z(String strInFileName, String strOutFileName) throws IOException
	{
		java.io.File inFile = new java.io.File(strInFileName);
		java.io.File outFile = new java.io.File(strOutFileName);

		java.io.BufferedInputStream inStream = new java.io.BufferedInputStream(new java.io.FileInputStream(inFile));
		java.io.BufferedOutputStream outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile));

		boolean eos = false;
		{
			SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();
			encoder.SetEndMarkerMode(eos);
			encoder.WriteCoderProperties(outStream);
			long fileSize;
			if (eos)
				fileSize = -1;
			else
				fileSize = inFile.length();
			for (int i = 0; i < 8; i++)
				outStream.write((int) (fileSize >>> (8 * i)) & 0xFF);
			encoder.Code(inStream, outStream, -1, -1, null);
		}
		outStream.flush();
		outStream.close();
		inStream.close();
	}
}