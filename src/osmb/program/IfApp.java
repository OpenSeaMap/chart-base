package osmb.program;


/**
 * general interface for ALL apps, regardless of OS or if Console, Service or GUI-App it only specifies the public access to any app. details of implementation
 * like abstract protected methods etc are declared in the abstract class ACApp
 * 
 * @author humbach
 * 
 */
public interface IfApp
{
	String[] getArgs();

	void setArgs(String[] strArgs);

	ACSettings getSettings();
}
