package osmb.program;

import osmb.program.ACSettings.Directories;
import osmb.utilities.UnitSystem;

public interface IfSettings
{
	public String getUserAgent();

	public String getVersion();

	public UnitSystem getUnitSystem();

	public Directories getDirectories();
}
