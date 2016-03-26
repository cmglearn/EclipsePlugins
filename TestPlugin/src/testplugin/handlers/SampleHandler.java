package testplugin.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("restriction")
public class SampleHandler extends AbstractHandler
{

//	static
//	{
//		CommonUtils.redirectOutputToFile("e:/eclipseOutput.log");
//	}

	/**
	 * The constructor.
	 */
	public SampleHandler()
	{
	}

	/**
	 * the command has been executed, so extract extract the needed information from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		CommonUtils.redirectOutputToFile("e:/eclipseOutput.log");
		System.out.println(">>>START_TASK");
		// IWorkbenchWindow window = HandlerUtil
		// .getActiveWorkbenchWindowChecked(event);
		// MessageDialog.openInformation(window.getShell(), "TestPlugin",
		// "Hello, Eclipse world");

		// callersHierarchy();
		calleesHierarchy();
		System.out.println(">>>END_TASK");
		return null;
	}

	private void calleesHierarchy()
	{
		TestsCalleesHierarchy.testFindCallersHierarchyWithRecurionsDetection();
	}

	private void callersHierarchy()
	{
		// test1();

		// walaTest();

		// testFindMethod();

		// vogella();

		// wala();

		// testFindCallersHierarchyForMethod();
		// testFindCallersHierarchyForMethodInOtherProjects();
		// testFindCallersHierarchyForMethodInOtherProjects("TestPrj", SIGNATURES);//!!!WORKS!!!
		// testFindCallersHierarchyForMethodInOtherProjects(readFile(new File("E:\\inputPlugin.txt")));// !!!WORKS!!!
		TestsCallersHierarchy.testFindCallersHierarchyWithRecurionsDetection(CommonUtils.readFile(new File("E:\\inputPlugin.txt")));
	}
}
