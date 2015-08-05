package osmb.program.catalog;

import java.io.File;

import javax.xml.bind.JAXBException;

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