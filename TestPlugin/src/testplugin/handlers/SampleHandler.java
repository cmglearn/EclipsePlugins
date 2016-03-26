package testplugin.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("restriction")
public class SampleHandler extends AbstractHandler
{

	static
	{
		try
		{
			PrintStream out = new PrintStream(new FileOutputStream("e:/eclipseOutput.log"));
			System.setOut(out);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

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
