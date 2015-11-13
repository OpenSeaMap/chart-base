package osmb.utilities;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class OSMBStrs
{
	private static final String BUNDLE_NAME = "osmb.resources.text.loc-nls";
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private OSMBStrs()
	{
	}

	public static String RStr(String key)
	{
		try
		{
			return RESOURCE_BUNDLE.getString(key);
		}
		catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
	}

	public static String RFStr(String key, Object... args)
	{
		try
		{
			return String.format(RESOURCE_BUNDLE.getString(key), args);
		}
		catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
	}
}
