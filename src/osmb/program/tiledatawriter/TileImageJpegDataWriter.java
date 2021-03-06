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
package osmb.program.tiledatawriter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;

import osmb.program.tiles.TileImageType;

public class TileImageJpegDataWriter implements IfTileImageDataWriter
{
	protected static final Logger log = Logger.getLogger(TileImageJpegDataWriter.class);

	protected ImageWriter jpegImageWriter = null;

	protected ImageWriteParam iwp = null;

	protected float jpegCompressionLevel;

	/**
	 * 
	 * @param jpegCompressionLevel
	 *          a float between 0 and 1; 1 specifies minimum compression and maximum quality
	 */
	public TileImageJpegDataWriter(double jpegCompressionLevel)
	{
		this((float) jpegCompressionLevel);
	}

	public TileImageJpegDataWriter(float jpegCompressionLevel)
	{
		this.jpegCompressionLevel = jpegCompressionLevel;
	}

	public TileImageJpegDataWriter(TileImageJpegDataWriter jpegWriter)
	{
		this(jpegWriter.getJpegCompressionLevel());
	}

	@Override
	public void initialize()
	{
		if (log.isTraceEnabled())
		{
			String s = "Available JPEG image writers:";
			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
			while (writers.hasNext())
			{
				ImageWriter w = writers.next();
				s += "\n\t" + w.getClass().getName();
			}
			log.trace(s);
		}
		jpegImageWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
		if (jpegImageWriter == null)
			throw new NullPointerException("Unable to create a JPEG image writer");
		jpegImageWriter.addIIOWriteWarningListener(ImageWriterWarningListener.INSTANCE);
		log.debug("Used JPEG image writer: " + jpegImageWriter.getClass().getName());
		iwp = jpegImageWriter.getDefaultWriteParam();
		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwp.setCompressionQuality(jpegCompressionLevel);
	}

	public void setJpegCompressionLevel(float jpegCompressionLevel)
	{
		this.jpegCompressionLevel = jpegCompressionLevel;
		iwp.setCompressionQuality(jpegCompressionLevel);
	}

	public float getJpegCompressionLevel()
	{
		return jpegCompressionLevel;
	}

	@Override
	public void processImage(BufferedImage image, OutputStream out) throws IOException
	{
		if (image.getColorModel().hasAlpha())
		{
			// Javas JPEG writes has a bug when the image has alpha transparency
			// see http://stackoverflow.com/questions/4386446/problem-using-imageio-write-jpg-file

			BufferedImage imageRGB = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = imageRGB.createGraphics();
			g.drawImage(image, null, 0, 0);
			g.dispose();
			image = imageRGB;
		}

		ImageOutputStream imageOut = ImageIO.createImageOutputStream(out);
		jpegImageWriter.setOutput(imageOut);
		IIOImage ioImage = new IIOImage(image, null, null);
		jpegImageWriter.write(null, ioImage, iwp);
	}

	@Override
	public void dispose()
	{
		jpegImageWriter.dispose();
		jpegImageWriter = null;
	}

	@Override
	public TileImageType getType()
	{
		return TileImageType.JPG;
	}

	public static boolean performOpenJDKJpegTest()
	{
		try
		{
			TileImageJpegDataWriter writer = new TileImageJpegDataWriter(0.99d);
			writer.initialize();
			OutputStream out = new NullOutputStream();
			BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
			writer.processImage(image, out);
			return true;
		}
		catch (Exception e)
		{
			log.debug("Jpeg test failed", e);
			return false;
		}
	}
}
