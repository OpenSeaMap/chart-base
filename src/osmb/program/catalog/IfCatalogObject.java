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
	 * This calculates the westernmost border over all maps in {@link  osmb.program.map.IfMapSpace#MAX_TECH_ZOOM zoom 22}.
	 * 
	 * @return {@link osmb.utilities.image.MercatorPixelCoordinate Border Coordinate} (W)
	 */
	public int getXBorderMin();

	/**
	 * This calculates the easternmost border over all maps in {@link  osmb.program.map.IfMapSpace#MAX_TECH_ZOOM zoom 22}.
	 * 
	 * @return {@link osmb.utilities.image.MercatorPixelCoordinate Border Coordinate} (E)
	 */
	public int getXBorderMax();

	/**
	 * This calculates the northernmost border over all maps in {@link  osmb.program.map.IfMapSpace#MAX_TECH_ZOOM zoom 22}.
	 * 
	 * @return {@link osmb.utilities.image.MercatorPixelCoordinate Border Coordinate} (N)
	 */
	public int getYBorderMin();

	/**
	 * This calculates the southernmost border over all maps in {@link  osmb.program.map.IfMapSpace#MAX_TECH_ZOOM zoom 22}.
	 * 
	 * @return {@link osmb.utilities.image.MercatorPixelCoordinate Border Coordinate} (S)
	 */
	public int getYBorderMax();

	/**
	 * This calculates the latiude of the southernmost pixel over all maps.
	 * 
	 * @return {@link osmb.utilities.geo.EastNorthCoordinate Double pixel coordinate} (S)
	 */
	double getMinLat();

	/**
	 * This calculates the latiude of the northernmost pixel over all maps.
	 * 
	 * @return {@link osmb.utilities.geo.EastNorthCoordinate Double pixel coordinate} (N)
	 */
	double getMaxLat();

	/**
	 * This calculates the longitude of the westernmost pixel over all maps.
	 * 
	 * @return {@link osmb.utilities.geo.EastNorthCoordinate Double pixel coordinate} (W)
	 */
	double getMinLon();

	/**
	 * This calculates the longitude of the easternmost pixel over all maps.
	 * 
	 * @return {@link osmb.utilities.geo.EastNorthCoordinate Double pixel coordinate} (E)
	 */
	double getMaxLon();

	/**
	 * This simply calculates all tiles included in the object. It currently (2015-08) does not take in account that tiles are shared by several maps.
	 * It recurses down until the map. It further ignores if tiles are already downloaded and available in the tile store or not.
	 */
	long calculateTilesToLoad();

	IfCatalog getCatalog();
}
