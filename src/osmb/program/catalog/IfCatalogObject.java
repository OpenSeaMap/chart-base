package osmb.program.catalog;

import javax.xml.bind.annotation.XmlAttribute;

import osmb.exceptions.InvalidNameException;

public interface IfCatalogObject
{
	@XmlAttribute
	String getName();

	void setName(String newName) throws InvalidNameException;

	boolean isInvalid();

	double getMinLat();

	double getMaxLat();

	double getMinLon();

	double getMaxLon();

	long calculateTilesToDownload();

	IfCatalog getCatalog();
}
