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
package osmb.program.tiles;


/**
 * An {@link UnrecoverableDownloadException} indicates that there has been a problem on client side that made it impossible to download a certain file (usually
 * a iMap tile image). Therefore the error is independent of the network connection between client and server and the server itself.
 */
public class UnrecoverableDownloadException extends TileException
{
	private static final long serialVersionUID = 1L;
	public static int ERROR_CODE_NORMAL = 0;
	public static int ERROR_CODE_CONTENT_TYPE = 1;
	private int errorCode = ERROR_CODE_NORMAL;

	public UnrecoverableDownloadException(String message)
	{
		super(message);
		errorCode = ERROR_CODE_NORMAL;
	}

	public UnrecoverableDownloadException(String message, int errorCode)
	{
		super(message);
		this.errorCode = errorCode;
	}

	public int getErrorCode()
	{
		return errorCode;
	}
}
