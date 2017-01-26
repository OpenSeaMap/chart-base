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
package osmb.utilities.path;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamePatternFileFilter implements FileFilter
{
	protected final Pattern pattern;

	public NamePatternFileFilter(Pattern pattern)
	{
		this.pattern = pattern;
	}

	public NamePatternFileFilter(String regex)
	{
		this.pattern = Pattern.compile(regex);
	}

	@Override
	public boolean accept(File pathname)
	{
		Matcher m = pattern.matcher(pathname.getName());
		return m.matches();
	}
}
