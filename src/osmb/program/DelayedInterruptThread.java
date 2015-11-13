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

import java.util.concurrent.ThreadFactory;

/**
 * The Berkeley DB has some problems when someone interrupts the thread that is currently performing IO activity.
 * Therefore before executing any DB we allow to disable the {@link #interrupt()} method via {@link #pauseInterrupt()}.
 * After the "interrupt sensitive section" {@link #resumeInterrupt()} restores the regular behavior. If the thread has
 * been interrupted while interrupting was disabled {@link #resumeInterrupt()} catches up this.
 */
public class DelayedInterruptThread extends Thread
{
	private boolean interruptPaused = false;
	private boolean interruptedWhilePaused = false;

	public DelayedInterruptThread(String name)
	{
		super(name);
	}

	public DelayedInterruptThread(Runnable job)
	{
		super(job);
	}

	public DelayedInterruptThread(Runnable job, String name)
	{
		super(job, name);
	}

	/**
	 * This handles interrupting.
	 * If interrupting is currently disabled, it marks the attempt for later handling. {@link #resumeInterrupt()} then does the interrupting.
	 */
	@Override
	public void interrupt()
	{
		if (interruptPaused)
			interruptedWhilePaused = true;
		else
			super.interrupt();
	}

	/**
	 * This disables 'normal' interrupting.
	 */
	public void pauseInterrupt()
	{
		interruptPaused = true;
	}

	/**
	 * This reenables 'normal' interrupting.
	 * It further checks if somebody tried to interrupt in the time between. If so it does so now.
	 */
	public void resumeInterrupt()
	{
		interruptPaused = false;
		if (interruptedWhilePaused)
			this.interrupt();
	}

	public boolean interruptedWhilePaused()
	{
		return interruptedWhilePaused;
	}

	public static ThreadFactory createThreadFactory()
	{
		return new DIThreadFactory();
	}

	/**
	 * This factory produces {@link DelayedInterruptThread}s.
	 * 
	 * @author humbach
	 *
	 */
	private static class DIThreadFactory implements ThreadFactory
	{
		@Override
		public Thread newThread(Runnable job)
		{
			return new DelayedInterruptThread(job);
		}
	}
}
