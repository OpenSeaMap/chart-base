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
package osmb.program;

/**
 * 
 * An abstract general app running under MS Windows. The application has to implement this.
 * It uses a graphical display etc
 * 
 * @author humbach
 * 
 * 
 */
public abstract class ACWindowsApp extends ACApp implements IfWinApp
{
	public ACWindowsApp()
	{
		super();
		try
		{
			;
		}
		catch (final Throwable t)
		{
			// display exception
			System.exit(1);
		}
	}

	protected boolean showSplashScreen()
	{
		return false; // /W if true, it works with Linux too
	}
}
