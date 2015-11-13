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

	/**
	 * @return
	 */
	public int getXBorderMin();

	/**
	 * @return
	 */
	public int getXBorderMax();

	/**
	 * @return
	 */
	public int getYBorderMin();

	/**
	 * @return
	 */
	public int getYBorderMax();

	/**
	 * @return
	 */
	double getMinLat();

	/**
	 * @return
	 */
	double getMaxLat();

	/**
	 * @return
	 */
	double getMinLon();

	/**
	 * @return
	 */
	double getMaxLon();

	/**
	 * This simply calculates all tiles included in the object. It currently (2015-08) does not take in account that tiles are shared by several maps.
	 * It recurses down until the map. It further ignores if tiles are already downloaded and available in the tile store or not.
	 */
	long calculateTilesToLoad();

	IfCatalog getCatalog();
}
