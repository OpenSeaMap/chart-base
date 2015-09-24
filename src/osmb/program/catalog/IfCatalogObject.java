package osmb.program.catalog;

import osmb.exceptions.InvalidNameException;

public interface IfCatalogObject
{
	/**
	 * has to be a @XmlAttribute
	 */
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
