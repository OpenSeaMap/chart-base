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
package osmb.program.tilestore;

import com.sleepycat.je.DatabaseException;

public class TileStoreException extends DatabaseException
{
	private static final long serialVersionUID = 1L;

	public TileStoreException(Throwable t)
	{
		super(t);
	}

	public TileStoreException(String message)
	{
		super(message);
	}

	public TileStoreException(String message, Throwable t)
	{
		super(message, t);
	}

}
