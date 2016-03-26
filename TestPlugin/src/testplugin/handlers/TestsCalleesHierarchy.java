package testplugin.handlers;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import testplugin.handlers.CommonUtils.MethodSignature;

public class TestsCalleesHierarchy
{

	public static void testFindCallersHierarchyWithRecurionsDetection()
	{

		try
		{
			for (MethodSignature ms : Arrays.asList(SIGNATURES))
			{
				long startTimeMillis = System.currentTimeMillis();
				System.out.println("############# Checking Callee Hierarchy for signature: " + ms + " #########################");
				System.out.println(" - starting at " + new Timestamp(startTimeMillis));
				IMethod method = CommonUtils.findMethod(ms);
				if (method != null)
				{
					MethodWrapper[] calleesRoots = CallHierarchy.getDefault().getCalleeRoots(new IMember[] { method });
					String prefix = "-";
					for (MethodWrapper mw : calleesRoots)
					{
						findCalleesPaths(mw, prefix);
					}
				}
				long endTimeMillis = System.currentTimeMillis();
				System.out.println(" - ended at " + new Timestamp(endTimeMillis) + "; took " + ((endTimeMillis - startTimeMillis) / 1000 / 60) + " minutes");
			}
		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private static boolean findCalleesPaths(MethodWrapper mw, String prefix) throws CoreException
	{
		String localPrefix = "." + prefix;
		if (mw == null)
		{
			return false;
		}

		if (mw.isRecursive())
		{
			return false;// recursive call
		}
		if (mw.getMember().getElementType() != IJavaElement.METHOD)
		{
			return false;// not method
		}
		String fullyQualifiedName = mw.getMember().getDeclaringType().getFullyQualifiedName();
		if (!fullyQualifiedName.startsWith("com.mind"))
		{
			return false; // not MIND
		}
		if ((mw.getMember() instanceof SourceMethod) && ((SourceMethod) mw.getMember()).isConstructor())
		{
			if (fullyQualifiedName.contains("Query"))
			{
				// displayCalle(mw, localPrefix);
				return true;
			}
			return false;// constructor
		}
		if (mw.getMember().isBinary())
		{
			return false;// is JAR
		}
		if (!belongsToProjects(mw, ALLOWED_PROJECTS))// TODO: allow delegates,stubs
		{
			return false;
		}
		if (extendsExceptionClasses(mw))
		{
			return false;
		}
		if (!CommonUtils.atLeastOneOfParamsBelongsTo(mw, MANDATORY_CLASSES))
		{
			return false;
		}

		String calleeSignature = CommonUtils.buildCalleeSignature(mw);
		if (detectedRecursion(mw, mw.getParent()))
		{
			System.out.println(prefix + "!!!STOP-Recursion at " + calleeSignature + "!!!");
			return false;
		}

		if (METHODS_CALLEES_ALREADY_DISPLAYED.contains(calleeSignature))
		{
			System.out.println(prefix + "!!!STOP - callees for method " + calleeSignature + " already printed!!!");
			return false;
		}

		System.out.println(localPrefix + ("lvl=" + String.format("%03d", mw.getLevel())) + " " + calleeSignature);

		MethodWrapper[] callers = mw.getCalls(new NullProgressMonitor());
		for (MethodWrapper caller : callers)
		{
			if (findCalleesPaths(caller, localPrefix))
			{
				// Query constructor found -> do not continue with rest of methods on the same level
				break;
			}
		}
		METHODS_CALLEES_ALREADY_DISPLAYED.add(calleeSignature);
		return false;
	}

	private static boolean extendsExceptionClasses(MethodWrapper mw) throws JavaModelException
	{
		IType declaringType = mw.getMember().getDeclaringType();
		if (EXCEPTION_CLASSES.contains(declaringType.getFullyQualifiedName()))
			return true;
		ITypeHierarchy typeH = declaringType.newTypeHierarchy(new NullProgressMonitor());
		IType[] allSuperclasses = typeH.getAllSuperclasses(declaringType);
		for (IType iType : allSuperclasses)
		{
			if (EXCEPTION_CLASSES.contains(iType.getFullyQualifiedName()))
			{
				return true;
			}
		}
		IType[] allInterfaces = typeH.getAllInterfaces();
		for (IType iType : allInterfaces)
		{
			if (EXCEPTION_CLASSES.contains(iType.getFullyQualifiedName()))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean belongsToProjects(MethodWrapper mw, Set<String> prjNames)
	{// mw.getMember().getDeclaringType().getParent().getParent()
		// mw.getMember().getDeclaringType().getJavaProject()//TODO:???
		IJavaElement currMember = mw.getMember();
		while (currMember.getElementType() != IJavaElement.JAVA_PROJECT)
		{
			currMember = currMember.getParent();
		}
		if (prjNames.contains(currMember.getElementName()))
		{
			return true;
		}
		return false;
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

	private static Set<String> ALLOWED_PROJECTS = new HashSet<String>()
	{
		{
			add("MindBill-Common");
			// add("MindBill-Core_Core-ejbClient");
			add("MindBill-Core_Core-ejb");

		}
	};
	private static Set<String> EXCEPTION_CLASSES = new HashSet<String>()
	{
		{
			add("com.mind.common.dto.DTO");
			add("com.mind.csr.core.common.general.dto.DTO");
			add("com.mind.common.user.User");
			add("com.mind.csr.core.misc.CoreSystemSettings");
			add("com.mind.common.account.constants.AccountConst.TaxationEngines");
			// add("com.mind.csr.core.misc.CoreCacheManager");//TODO: decide if add to exceptions or not
		}
	};

	private static Set<String> MANDATORY_CLASSES = new HashSet<String>()
	{
		{
			// !!!ONLY IF NECESSARY!!!
			// add("com.mind.utils.connection.db.DBConnection");
			// add("com.mind.j2ee.utils.CoreContext.BaseContext");
			// add("com.mind.j2ee.utils.CoreContext.CoreContext");
			// add("com.mind.csr.core.db.Query");
			// add("com.mind.infrastructure.db.Query");
			// add("com.mind.utils.db.MindQuery");
			// add("com.mind.utils.db.MindStatement");
			// add("java.sql.PreparedStatement");

			add("DBConnection");
			add("BaseContext");
			add("CoreContext");
			add("Query");
			add("MindQuery");
			add("MindStatement");
			add("PreparedStatement");
		}
	};

	private static MethodSignature[] SIGNATURES = new MethodSignature[] {
	/*
	 * new MethodSignature("TestPrj", "com.mind.test.TestClass", "void method()", false), new MethodSignature("TestPrj", "com.mind.test.TestClass", "MyObject1 complexMethod(MyObject2 param1, int param2, String param3)", false), new MethodSignature("TestPrj", "com.mind.test.TestClass",
	 * "com.mind.test.pck.MyObject1 complexMethod(com.mind.test.pck.MyObject2 param1, int param2, String param3)", false)
	 */
	/* new MethodSignature("CallerTestPrj", "com.mind.callertestprj.CallerTest3", "MyObject3 test1(com.mind.test.MyObject1 p1, MyObject1 p2, int i, String s)", false) */
	/* new MethodSignature("MindBill-Core_Core-ejb", "com.mind.csr.core.management.account.beans.session.AccountBean", "AccountDTO load(BaseContext info, long accountID, String code)", false), */
	new MethodSignature("MindBill-Core_Core-ejb", "com.mind.csr.core.management.account.beans.session.AccountBean", "boolean update(BaseContext info, long accountID, String accountCode, Map mapDto)", false) };
	private static Set<String> METHODS_CALLEES_ALREADY_DISPLAYED = new HashSet<String>();
}
