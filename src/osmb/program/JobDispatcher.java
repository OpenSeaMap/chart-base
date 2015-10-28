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
 * Not a singleton anymore. This is done because we have to handle different separate pools, i.e. for layer and map, or downloader etc...
 * 
 * @author humbach
 */
public class JobDispatcher extends ThreadPoolExecutor implements ThreadFactory, RejectedExecutionHandler
{
	private static final Logger log = Logger.getLogger(JobDispatcher.class);
	private static JobDispatcher INSTANCE = new JobDispatcher();

	private static final int WORKER_THREAD_INIT_COUNT = 5;
	private static final int WORKER_THREAD_MAX_COUNT = 500;

	/**
	 * Specifies the time span in seconds that a worker thread waits for new jobs to perform. If the time span has elapsed the worker thread terminates itself.
	 * Only the first worker thread works differently, it ignores the timeout and will never terminate itself.
	 */
	private static final int WORKER_THREAD_TIMEOUT = 30;

	/**
	 * @deprecated see {@link JobDispatcher}
	 */
	@Deprecated
	public static JobDispatcher getInstance()
	{
		log.debug("do not use any longer");
		return null;
	}

	private int WORKER_THREAD_ID = 1;
	// private final BlockingQueue<Runnable> jobQueue;
	// private final ThreadPoolExecutor executor;

	/**
	 * Removes all jobs from the queue that are currently not being processed.
	 */
	public void cancelOutstandingJobs()
	{
		getQueue().clear();
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

	/**
	 * Why this ?
	 * 
	 * @deprecated Use execute(job) instead
	 */
	@Deprecated
	public void addJob(Runnable job)
	{
		execute(job);
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
		return new DelayedInterruptThread(job, "Thread " + id);
	}

	@Override
	public void rejectedExecution(Runnable job, ThreadPoolExecutor executor)
	{
		log.error("Job rejected: " + job + " by " + executor);
	}
}
