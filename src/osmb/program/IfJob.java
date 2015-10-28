package osmb.program;

public interface IfJob extends Runnable
{
	void run(JobDispatcher dispatcher) throws Exception;
}
