package testplugin.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import testplugin.handlers.CommonUtils.MethodSignature;


public class TestsCallersHierarchy
{

	public static void testFindCallersHierarchyWithRecurionsDetection(List<MethodSignature> methodSignatures)
	{
		try
		{
			for (MethodSignature ms : methodSignatures)
			{
				System.out.println("############# Checking Call Hierarchy for signature: " + ms + " #########################");
				IMethod method = CommonUtils.findMethod(ms);
				if (method != null)
				{
					MethodWrapper[] callerRoots = CallHierarchy.getDefault().getCallerRoots(new IMember[] { method });
					String prefix = "-";
					for (MethodWrapper mw : callerRoots)
					{
						findCallersPaths(mw, prefix);
					}
				}
			}
		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private static void testFindCallersHierarchyForMethodInOtherProjects(List<MethodSignature> methodSignatures)
	{
		try
		{
			for (MethodSignature ms : methodSignatures)
			{
				System.out.println("############# Checking Call Hierarchy for signature: " + ms + " #########################");
				IMethod method = CommonUtils.findMethod(ms.prjName, ms.clazz, ms.method);
				if (method != null)
				{
					MethodWrapper[] callerRoots = CallHierarchy.getDefault().getCallerRoots(new IMember[] { method });
					String prefix = "-";
					for (MethodWrapper mw : callerRoots)
					{
						displayCallersPaths(mw, prefix);
					}
				}
			}
		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private static void testFindCallersHierarchyForMethodInOtherProjects(MethodSignature[] methodSignatures)
	{
		try
		{
			for (MethodSignature ms : methodSignatures)
			{
				System.out.println("############# Checking Call Hierarchy for signature: " + ms + " #########################");
				IMethod method = CommonUtils.findMethod(ms.prjName, ms.clazz, ms.method);
				if (method != null)
				{
					MethodWrapper[] callerRoots = CallHierarchy.getDefault().getCallerRoots(new IMember[] { method });
					String prefix = "-";
					for (MethodWrapper mw : callerRoots)
					{
						displayCallersPaths(mw, prefix);
					}
				}
			}
		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private static void testFindCallersHierarchyForMethodInOtherProjects()
	{
		try
		{
			IMethod method = CommonUtils.findMethod("TestPrj", "com.mind.test.TestClass", "com.mind.test.pck.MyObject1 complexMethod(com.mind.test.pck.MyObject2, int, String)");
			if (method != null)
			{
				System.out.println(method.toString());

				MethodWrapper[] callerRoots = CallHierarchy.getDefault().getCallerRoots(new IMember[] { method });
				String prefix = "-";
				for (MethodWrapper mw : callerRoots)
				{
					displayCallersPaths(mw, prefix);
				}
			}

		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private void testFindCallersHierarchyForMethod()
	{
		try
		{
			IMethod method = CommonUtils.findMethod("TestPrj", "com.mind.test.TestClass", "com.mind.test.pck.MyObject1 complexMethod(com.mind.test.pck.MyObject2, int, String)");
			if (method != null)
			{
				System.out.println(method.toString());

				MethodWrapper[] callerRoots = CallHierarchy.getDefault().getCallerRoots(new IMember[] { method });
				String prefix = "-";
				for (MethodWrapper mw : callerRoots)
				{
					displayCallersPaths(mw, prefix);
				}
				// for (MethodWrapper methodWrapper : callerRoots) {
				// // methodWrapper.getMethodCall()
				// //System.out.println(methodWrapper.toString());
				// // CallHierarchy.getCallLocation( methodWrapper)
				// MethodWrapper[] callers1 = methodWrapper.getCalls(new
				// NullProgressMonitor());
				// for (MethodWrapper caller1 : callers1) {
				// MethodWrapper[] callers2 = caller1.getCalls(new
				// NullProgressMonitor());
				// for (MethodWrapper caller2 : callers2) {
				// System.out.println();caller2.getCalls(new
				// NullProgressMonitor())[0].getCalls(new
				// NullProgressMonitor());
				// }
				// }
				// }
			}

		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private static void findCallersPaths(MethodWrapper mw, String prefix) throws CoreException
	{
		if (mw == null)
		{
			return;
		}
		String callerSignature = CommonUtils.buildCallerSignature(mw);
		if (METHODS_CALLERS_ALREADY_DISPLAYED.contains(callerSignature))
		{
			System.out.println(prefix + "!!!STOP - callers for method " + callerSignature + " already printed!!!");
			return;
		}
		if (detectedRecursion(mw, mw.getParent()))
		{
			System.out.println(prefix + "!!!STOP-Recursion at " + callerSignature + "!!!");
			return;
		}

		String localPrefix = "." + prefix;
		System.out.println(localPrefix + ("lvl=" + String.format("%03d", mw.getLevel())) + " " + callerSignature);

		if (declaredInBean(mw) || declaredInDelegate(mw))
		{
			return;

		}
		MethodWrapper[] callers = mw.getCalls(new NullProgressMonitor());
		for (MethodWrapper caller : callers)
		{
			findCallersPaths(caller, localPrefix);
		}
		METHODS_CALLERS_ALREADY_DISPLAYED.add(callerSignature);
	}

	private static boolean declaredInBean(MethodWrapper mw) throws CoreException
	{
		IType declaringType = mw.getMember().getDeclaringType();
		String fullyQualifiedName = declaringType.getFullyQualifiedName();
		if (BEANS.contains(fullyQualifiedName))
		{
			return true;
		}
		ITypeHierarchy typeH = declaringType.newTypeHierarchy(new NullProgressMonitor());
		IType[] allSuperclasses = typeH.getAllSuperclasses(declaringType);
		for (IType iType : allSuperclasses)
		{
			if (iType.getFullyQualifiedName().equals("com.mind.j2ee.utils.ejb3.bean.BaseSessionBean"))
			{
				BEANS.add(fullyQualifiedName);
				return true;
			}
		}
		// mw.getMember().getTypeRoot().findPrimaryType().getFullyQualifiedName();
		return false;
	}

	private static boolean declaredInDelegate(MethodWrapper mw) throws CoreException
	{
		IType declaringType = mw.getMember().getDeclaringType();
		String fullyQualifiedName = declaringType.getFullyQualifiedName();
		if (DELEGATES.contains(fullyQualifiedName))
		{
			return true;
		}
		ITypeHierarchy typeH = declaringType.newTypeHierarchy(new NullProgressMonitor());
		IType[] allSuperclasses = typeH.getAllSuperclasses(declaringType);
		for (IType iType : allSuperclasses)
		{
			if (iType.getFullyQualifiedName().equals("com.mind.j2ee.utils.ejb3.BaseDelegate"))
			{
				DELEGATES.add(fullyQualifiedName);
				return true;
			}
		}
		// mw.getMember().getTypeRoot().findPrimaryType().getFullyQualifiedName();
		return false;
	}

	private static void displayCallersPaths(MethodWrapper mw, String prefix) throws CoreException
	{

		// long start = System.currentTimeMillis();
		MethodWrapper[] callers = mw.getCalls(new NullProgressMonitor());
		// System.out.println("Took "+ ((System.currentTimeMillis() - start)
		// /1000.) + " s");
		String localPrefix = "." + prefix;
		for (MethodWrapper caller : callers)
		{
			// caller.getMember().getDeclaringType().getFullyQualifiedName();
			// caller.getMember().getElementName();caller.getMember()

			if (caller.getParent() != null && caller.getParent().getParent() != null && caller.getParent().getParent().getMethodCall().equals(caller.getMethodCall()))
			{
				System.out.println("!!!STOP-LOOP!!!");
				return;
			} else
			{
				// caller.getMethodCall().equals(caller.getParent().getParent().getParent().getParent().getParent().getMethodCall())
				String callerSignature = CommonUtils.buildCallerSignature(caller);
				System.out.println(localPrefix + ("lvl=" + String.format("%03d", caller.getLevel())) + " " + callerSignature);
				displayCallersPaths(caller, localPrefix);
			}
		}
	}

	private static boolean detectedRecursion(MethodWrapper mw, MethodWrapper checkedMw)
	{

		if (checkedMw == null)
		{
			return false;
		}
		// both mw & checkedMw are not null
		if (mw.getMethodCall().getKey().equals(checkedMw.getMethodCall().getKey()))
		{
			return true;//
		} else
		{
			return detectedRecursion(mw, checkedMw.getParent());
		}

	}
	
	private void testFindMethod()
	{
		try
		{

			IMethod method = CommonUtils.findMethod("TestPrj", "com.mind.test.TestClass", "com.mind.test.pck.MyObject1 complexMethod(com.mind.test.pck.MyObject2, int, String)");
			System.out.println(method.toString());

			IMethod method1 = CommonUtils.findMethod("TestPrj", "com.mind.test.TestClass", " com.mind.test.pck.MyObject1 complexMethod(com.mind.test.pck.MyObject2 ,  int ,  String ) ");
			System.out.println(method1.toString());

			// IType foundType =
			// JdtUtil.getJavaProject("TestPrj").findType("com.mind.test.TestClass");
			// if (foundType != null) {
			// for (IMethod method : foundType.getMethods()) {
			// System.out.println(method.toString());JavaModelManager.getJavaModelManager().peekAtInfo(method);
			// }
			// //foundType.getMethod(name, parameterTypeSignatures)
			// }

		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}


	// private void walaTest() {
	// try {
	//
	// IType foundType =
	// CommonUtils.getJavaProject("TestPrj").findType("com.mind.test.TestClass");
	//
	// IMethod findJavaMethodInProjects =
	// JdtUtil.findJavaMethodInProjects("com.mind.test.TestClass",
	// /* "complexMethod(MyObject2,int,String)" */
	// "complexMethod(com.mind.test.MyObject2,int,java.lang.String)",
	// Arrays.asList(JdtUtil.getJavaProject("TestPrj")));
	// System.out.println(findJavaMethodInProjects.getSource());
	//
	// IMethod findJavaMethodInProjects1 =
	// JdtUtil.findJavaMethodInProjects("com.mind.test.TestClass",
	// "complexMethod(com.mind.test.pck.MyObject2,int,String)",
	// Arrays.asList(JdtUtil.getJavaProject("TestPrj")));
	// System.out.println(findJavaMethodInProjects1.getSource());
	//
	// if (foundType != null) {
	//
	// IMethod method = foundType.getMethod("complexMethod",
	// Signature.getParameterTypes(
	// "com.mind.test.TestClass.complexMethod(com.mind.test.MyObject2 param1,
	// int param2, java.lang.String param3) throws com.mind.test.MyObject3"));
	//
	// System.out.println(method.toString());
	// }
	//
	// MethodWrapper m;
	// // CallHierarchy.getDefault().getCallLocation(element)
	//
	// } catch (CoreException e) {
	// e.printStackTrace();
	// }
	// }

	private void test1()
	{
		try
		{
			IType foundType = CommonUtils.getJavaProject("TestPrj").findType("com.mind.test.TestClass");
			if (foundType != null)
			{
				IMethod method = foundType.getMethod("method", Signature.getParameterTypes("void com.mind.test.TestClass.method()"));
				System.out.println(method.toString());

				// MethodWrapper[] callerRoots =
				// CallHierarchy.getDefault().getCallerRoots(new
				// IMethod[]{method});
				Set<IMethod> callersOf = getCallersOf(method);
				// method.getDeclaringType().getFullyQualifiedName()
				// method.getParameterNames()

				for (IMethod iMethod : callersOf)
				{
					System.out.println(iMethod.toString());
					displayAllCallersHierarchy(iMethod);
				}

			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void displayAllCallersHierarchy(IMethod method) throws Exception
	{
		Set<IMethod> callersOf = getCallersOf(method);
		if (callersOf != null)
		{
			for (IMethod iMethod : callersOf)
			{
				if (iMethod != null)
				{
					System.out.println(iMethod.getDefaultValue());
				}
			}
		}

	}

	private void wala()
	{
		try
		{
			CommonUtils.getJavaProject("TestPrj").getChildren();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void vogella()
	{
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		// Loop over all projects
		for (IProject project : projects)
		{
			try
			{
				printProjectInfo(project);
			} catch (CoreException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void printProjectInfo(IProject project) throws CoreException, CoreException
	{
		System.out.println("Working in project " + project.getName());
		// check if we have a Java project
		if (project.isNatureEnabled("org.eclipse.jdt.core.javanature"))
		{
			IJavaProject javaProject = JavaCore.create(project);
			printPackageInfos(javaProject);
		}
	}

	private void printPackageInfos(IJavaProject javaProject) throws CoreException
	{
		IPackageFragment[] packages = javaProject.getPackageFragments();
		for (IPackageFragment mypackage : packages)
		{
			// Package fragments include all packages in the
			// classpath
			// We will only look at the package from the source
			// folder
			// K_BINARY would include also included JARS, e.g.
			// rt.jar
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE)
			{
				System.out.println("Package " + mypackage.getElementName());
				printICompilationUnitInfo(mypackage);

			}

		}
	}

	private void printICompilationUnitInfo(IPackageFragment mypackage) throws CoreException
	{
		for (ICompilationUnit unit : mypackage.getCompilationUnits())
		{
			printCompilationUnitDetails(unit);

		}
	}

	private void printIMethods(ICompilationUnit unit) throws CoreException
	{
		IType[] allTypes = unit.getAllTypes();
		for (IType type : allTypes)
		{
			printIMethodDetails(type);
		}
	}

	private void printCompilationUnitDetails(ICompilationUnit unit) throws CoreException
	{
		System.out.println("Source file " + unit.getElementName());
		// Document doc = new Document(unit.getSource());
		// System.out.println("Has number of lines: " + doc.getNumberOfLines());
		printIMethods(unit);
	}

	private void printIMethodDetails(IType type) throws CoreException
	{
		IMethod[] methods = type.getMethods();
		for (IMethod method : methods)
		{

			System.out.println("Method name " + method.getElementName());
			System.out.println("Signature " + method.getSignature());
			System.out.println("Return Type " + method.getReturnType());

		}
	}

	public static Set<IMethod> getCallersOf(IMethod m)
	{

		CallHierarchy callHierarchy = CallHierarchy.getDefault();

		IMember[] members = { m };

		MethodWrapper[] methodWrappers = callHierarchy.getCallerRoots(members);
		HashSet<IMethod> callers = new HashSet<IMethod>();
		for (MethodWrapper mw : methodWrappers)
		{
			MethodWrapper[] mw2 = mw.getCalls(new NullProgressMonitor());
			Set<IMethod> temp = getIMethods(mw2);
			callers.addAll(temp);
		}

		return callers;
	}

	static Set<IMethod> getIMethods(MethodWrapper[] methodWrappers)
	{
		HashSet<IMethod> c = new HashSet<IMethod>();
		for (MethodWrapper m : methodWrappers)
		{
			IMethod im = getIMethodFromMethodWrapper(m);
			if (im != null)
			{
				c.add(im);
			}
		}
		return c;
	}

	static IMethod getIMethodFromMethodWrapper(MethodWrapper m)
	{
		try
		{
			IMember im = m.getMember();
			if (im.getElementType() == IJavaElement.METHOD)
			{
				return (IMethod) m.getMember();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}


	private static class MyRequestor extends SearchRequestor
	{
		IMethod foundMethod;

		@Override
		public void acceptSearchMatch(SearchMatch sm) throws CoreException
		{
			foundMethod = (IMethod) sm.getElement();
			// System.out.println(foundMethod);
		}

	}


	private static Set<String> BEANS = new HashSet<String>();
	private static Set<String> DELEGATES = new HashSet<String>();
	private static Set<String> METHODS_CALLERS_ALREADY_DISPLAYED = new HashSet<String>();
	private static MethodSignature[] SIGNATURES = new MethodSignature[] { new MethodSignature("TestPrj", "com.mind.test.TestClass", "void method()", false), new MethodSignature("TestPrj", "com.mind.test.TestClass", "MyObject1 complexMethod(MyObject2 param1, int param2, String param3)", false),
			new MethodSignature("TestPrj", "com.mind.test.TestClass", "com.mind.test.pck.MyObject1 complexMethod(com.mind.test.pck.MyObject2 param1, int param2, String param3)", false) };


}
