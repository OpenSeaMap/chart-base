package osmb.program;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import osmb.utilities.OSMBUtilities;

public abstract class ACApp implements IfApp
{
	// class data / statics
	static protected IfApp gApp = null;

	static public IfApp getApp()
	{
		return gApp;
	};

	static public String getProgramDirStr()
	{
		String strDirName = null;
		try
		{
			strDirName = programDir.getCanonicalPath();
		}
		catch (IOException e)
		{
		}
		return strDirName;
	}

	static public File getProgramDir()
	{
		return programDir;
	}

	// instance data, usually all protected
	protected IfCommandLine cmdl = null;
	protected ACSettings pSets = null;
	protected String[] ARGS = null;
	protected static File programDir = null;
	protected JArgs mCmdlParser = new JArgs();

	protected ACApp()
	{
		gApp = this;
	}

	// default implementations of public methods
	@Override
	public ACSettings getSettings()
	{
		return pSets;
	}

	public void findProgramDir()
	{
		programDir = OSMBUtilities.getClassLocation(this.getClass());
		if ("bin".equals(programDir.getName())) // remove the bin dir -> this usually happens only in a development environment
			programDir = programDir.getParentFile();
	}

	public JArgs getCmdlParser()
	{
		return mCmdlParser;
	}

	@Override
	public void setArgs(String[] strArgs)
	{
		this.ARGS = strArgs;
	}

	@Override
	public String[] getArgs()
	{
		return ARGS;
	}

	protected void loadSettings()
	{
		try
		{
			pSets = ACSettings.load();
		}
		catch (JAXBException e)
		{
			e.printStackTrace();
		}
	}

	protected abstract void parseCommandLine();

	public abstract int runWork();
}
