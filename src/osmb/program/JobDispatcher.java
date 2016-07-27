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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Not a singleton any longer. This is done because we have to handle different separate pools, i.e. for layer and map, or downloader etc...
 * 
 * @author humbach
 */
public class JobDispatcher extends ThreadPoolExecutor implements ThreadFactory, RejectedExecutionHandler
{
	protected static final Logger log = Logger.getLogger(JobDispatcher.class);

	private static final int WORKER_THREAD_INIT_COUNT = 50;
	private static final int WORKER_THREAD_MAX_COUNT = 500;

	/**
	 * Specifies the time span in seconds that a worker thread waits for new jobs to perform. If the time span has elapsed the worker thread terminates itself.
	 */
	private static final int WORKER_THREAD_TIMEOUT = 120;

	/**
	 * This holds the TID for the next Thread
	 */
	private int WORKER_THREAD_ID = 1;

	/**
	 * Removes all jobs from the queue that are currently not being processed.
	 */
	public void cancelOutstandingJobs()
	{
		log.debug("waiting jobs=" + getQueue().size());
		getQueue().clear();
		purge();
		log.debug("remaining jobs=" + getQueue().size());
	}

	/**
	 * Default constructor with no special values
	 */
	public JobDispatcher()
	{
		super(WORKER_THREAD_INIT_COUNT, WORKER_THREAD_MAX_COUNT, WORKER_THREAD_TIMEOUT, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		allowCoreThreadTimeOut(true);
	}

	/**
	 * Constructor with a specified maximum of concurrent threads in this pool. This maximum has to be at least the minimum or the other way round.
	 */
	public JobDispatcher(int nMaxConcThreads)
	{
		super(Math.min(nMaxConcThreads, WORKER_THREAD_INIT_COUNT), nMaxConcThreads, WORKER_THREAD_TIMEOUT, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		allowCoreThreadTimeOut(true);
	}

	@Override
	public Thread newThread(Runnable job)
	{
		int id;
		synchronized (this)
		{
			id = WORKER_THREAD_ID++;
		}
		log.trace("New worker thread created with id=" + id);
		return new DelayedInterruptThread(job, "Thread-" + id);
	}

	@Override
	public void rejectedExecution(Runnable job, ThreadPoolExecutor executor)
	{
		log.error("Job rejected: " + job + " by " + executor);
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable, java.lang.Throwable)
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t)
	{
		super.afterExecute(r, t);
		if (t == null)
			log.debug("t=" + t + " r=" + r + " finished");
		else
			try
			{
				throw t;
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#beforeExecute(java.lang.Thread, java.lang.Runnable)
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r)
	{
		log.debug("START t=" + t + " r=" + r);
		super.beforeExecute(t, r);
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	@Override
	public void purge()
	{
		// TODO Auto-generated method stub
		super.purge();
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#remove(java.lang.Runnable)
	 */
	@Override
	public boolean remove(Runnable task)
	{
		return super.remove(task);
	}

	/**
	 * Sets a new max thread number.
	 */
	public void setNewMaxThreads(int nMax)
	{
		setMaximumPoolSize(nMax);
	}

	/**
	 * nyr
	 * 
	 * @param strPref
	 *          The thread name prefix to be used by this thread pool.
	 */
	public void setNamePref(String strPref)
	{
		// TODO modify the ThreadFactory to accommodate the prefix setting
	}

	/*
	 * prestartCoreThread()
	 */
}
