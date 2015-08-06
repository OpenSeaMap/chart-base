package osmb.program.catalog;

import java.io.File;

import javax.xml.bind.JAXBException;

/**
 * An IfCatalogProfile is a file which (probably) contains a catalog. It usually is not loaded from the harddisk, but you can check if it exists.
 * Obviously a Catalog implements an IFCatalogProfile.
 * 
 * @author humbach
 *
 */
public interface IfCatalogProfile
{
	@Override
	public abstract String toString();

	public abstract File getFile();

	public abstract String getName();

	public abstract boolean exists();

	public abstract void delete();

	public abstract int compareTo(IfCatalogProfile other);

	@Override
	public abstract boolean equals(Object other);

	// public abstract IfCatalog load() throws JAXBException;

	public abstract void save() throws JAXBException;
}