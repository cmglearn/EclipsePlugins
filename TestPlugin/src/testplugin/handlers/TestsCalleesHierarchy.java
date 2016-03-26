package testplugin.handlers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import testplugin.handlers.CommonUtils.MethodSignature;

@SuppressWarnings("restriction")
public class TestsCalleesHierarchy
{

	public static void testFindCallersHierarchyWithRecurionsDetection()
	{
		METHODS_CALLEES_ALREADY_DISPLAYED.clear();
		try
		{
			CallHierarchy callHierarchy = CallHierarchy.getDefault();
			for (MethodSignature ms : Arrays.asList(SIGNATURES))
			{
				long startTimeMillis = System.currentTimeMillis();
				CommonUtils.newLinePrint("");
				CommonUtils.newLinePrint("############# Checking Callee Hierarchy for signature: " + ms + " #########################");
				CommonUtils.newLinePrint(" - starting at " + new Timestamp(startTimeMillis));
				IMethod method = CommonUtils.findMethod(ms);
				if (method != null)
				{
					MethodWrapper[] calleesRoots = callHierarchy.getCalleeRoots(new IMember[] { method });
					String prefix = "-";
					for (MethodWrapper mw : calleesRoots)
					{
						findCalleesPaths(callHierarchy, mw, prefix);
					}
				}
				long endTimeMillis = System.currentTimeMillis();
				long noOfMillis = endTimeMillis - startTimeMillis;
				long noOfSeconds = noOfMillis / 1000;
				CommonUtils.newLinePrint(" - ended at " + new Timestamp(endTimeMillis) + "; took " + ((noOfSeconds < 60) ? noOfSeconds + " seconds" : (noOfSeconds / 60) + " minutes"));
			}
		} catch (CoreException e)
		{
			e.printStackTrace();
		}
	}

	private static boolean findCalleesPaths(CallHierarchy callHierarchy, MethodWrapper mw, String prefix) throws CoreException
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
			if (containsStopTokens(fullyQualifiedName))
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

		String callSignature = CommonUtils.buildCalleeSignature((IMethod) mw.getMember());
		if (detectedRecursion(mw, mw.getParent()))
		{
			CommonUtils.newLinePrint(prefix + "!!!STOP-Recursion at " + callSignature + "!!!");
			return false;
		}

		if (METHODS_CALLEES_ALREADY_DISPLAYED.contains(callSignature))
		{
			CommonUtils.newLinePrint(prefix + "!!!STOP - callees for method " + callSignature + " already printed!!!");
			return false;
		}

		CommonUtils.newLinePrint(localPrefix + ("lvl=" + String.format("%03d", mw.getLevel())) + " " + callSignature);

		if (mw.getMember().getDeclaringType().isInterface())
		{
			List<MethodWrapper> allImplementations = getAllImplementations(callHierarchy, mw, callSignature);
			for (MethodWrapper call : allImplementations)
			{

				if (findCalleesPaths(callHierarchy, call, localPrefix))
				{
					// Query constructor found -> do not continue with rest of methods on the same level
					break;
				}
			}

		} else
		{
			MethodWrapper[] calls = mw.getCalls(new NullProgressMonitor());
			for (MethodWrapper call : calls)
			{

				if (findCalleesPaths(callHierarchy, call, localPrefix))
				{
					// Query constructor found -> do not continue with rest of methods on the same level
					System.out.print(" -> !!!CHECK THAT METHOD!!!");
					break;
				}
			}
		}
		METHODS_CALLEES_ALREADY_DISPLAYED.add(callSignature);
		return false;
	}

	private static boolean containsStopTokens(String fullyQualifiedName)
	{
		for (String tkn : STOP_TOKENS)
		{
			if (fullyQualifiedName.contains(tkn))
			{
				return true;
			}
		}
		return false;
	}

	private static List<MethodWrapper> getAllImplementations(CallHierarchy callHierarchy, MethodWrapper mw, String signature) throws CoreException
	{
		IType declaringType = mw.getMember().getDeclaringType();
		ITypeHierarchy typeH = declaringType.newTypeHierarchy(new NullProgressMonitor());
		IType[] allSubtypes = typeH.getAllSubtypes(declaringType);
		List<MethodWrapper> result = new ArrayList<MethodWrapper>();
		Set<IType> checkedSubTypes = new HashSet<IType>();
		for (IType subType : allSubtypes)
		{
			if (!subType.isClass() || checkedSubTypes.contains(subType))
			{
				continue;
			}
			IMethod[] methods = subType.findMethods((IMethod) mw.getMember())/* subType.getMethods() */;
			for (IMethod iMethod : methods)
			{
				// String implSignature = CommonUtils.buildCalleeSignature(iMethod);
				// if (signature.equals(implSignature))
				// {
				MethodWrapper[] calleeRoots = callHierarchy.getCalleeRoots(new IMember[] { iMethod });
				for (MethodWrapper methodWrapper : calleeRoots)
				{
					result.add(methodWrapper);
				}
				// }
			}
			checkedSubTypes.add(subType);
		}
		return result;
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
			add("MindBill-Core_Core-ejb");
			add("MindBill-Core_Core-ejbClient");
			add("MindBill-Core_Core-ejbClient");
			add("MindBill-WF_MindBill_Client");
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
			/* new MethodSignature("MindBill-Core_Core-ejb", "com.mind.csr.core.management.account.beans.session.AccountBean", "boolean update(BaseContext info, long accountID, String accountCode, Map mapDto)", false), */

			// - HOTBILLING -
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "com.mind.csr.core.management.account.dto.AccountDTO getAccount ( com.mind.j2ee.utils.CoreContext.BaseContext info, long accountID, java.lang.String code )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "java.util.Map getAccount ( com.mind.j2ee.utils.CoreContext.BaseContext info, java.lang.String code, long accountID, java.util.Map requestedKeys )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "java.util.Map getAccountServiceList ( com.mind.j2ee.utils.CoreContext.BaseContext info, com.mind.csr.core.management.accountservice.dto.GetAccountServiceListFilterDTO filter )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "com.mind.csr.core.management.account.dto.LimitedCreditTypeDTO getLimitedCreditTypeByAccountID ( com.mind.j2ee.utils.CoreContext.BaseContext info, long accountID )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "java.util.Map<java.lang.String,java.lang.Double> getReactivationServicesInfo ( com.mind.j2ee.utils.CoreContext.BaseContext info, long accountID )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "java.util.Map<java.lang.String,java.lang.Double> getSuspensionServicesInfo ( com.mind.j2ee.utils.CoreContext.BaseContext info, long accountID )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient",
//					"java.util.Map<java.lang.Long,com.mind.csr.core.management.accountservice.dto.ASStatusUpdateInfoDTO> loadASInfoForStatusUpdate ( com.mind.j2ee.utils.CoreContext.BaseContext info, java.util.List<java.lang.Long> asIds )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "java.util.Map<java.lang.Long,com.mind.csr.core.management.status.dto.StatusDTO> loadASStatuses ( com.mind.j2ee.utils.CoreContext.BaseContext info, java.util.List<java.lang.Long> asIDs )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "java.util.List<java.lang.Long> loadBillableAccWithSameParrent ( com.mind.j2ee.utils.CoreContext.BaseContext info, long parentAcctId )", false),
//			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient", "java.util.List<java.lang.Long> loadNonBillableAccWithSameNBASlim ( com.mind.j2ee.utils.CoreContext.BaseContext info, long accountId )", false),
			new MethodSignature("MindBill-WF_MindBill_Client", "com.mind.ejb.client.CoreEjbClient",
					"java.util.Map<com.mind.utils.Pair<java.lang.Long,java.lang.String>,java.lang.Boolean> loadServiceNotAllowedToConsumeFromAccountBalanceData(com.mind.j2ee.utils.CoreContext.BaseContext, java.util.List<com.mind.utils.Pair<java.lang.Long,java.lang.String>>)", true)

	// - various tests -
	// new MethodSignature("MindBill-Core_Core-ejb", "com.mind.csr.core.management.account.actions.GetAccount", "AccountDTO getAccount(CoreContext context, LoadAccountFilter filter)", false),// check call to another project (Common)
	// new MethodSignature("MindBill-Core_Core-ejb", "com.mind.csr.core.management.status.actions.ProcessCorrelationQueue", "void perform(CoreContext tc)", false),// check storeprocedure
	// new MethodSignature("MindBill-Core_Core-ejb", "com.mind.csr.core.management.account.actions.GetAccount", "AccountDTO performWithoutViewAccountSR(CoreContext context, LoadAccountFilter filter)", false)

	};
	private static Set<String> METHODS_CALLEES_ALREADY_DISPLAYED = new HashSet<String>();
	private static Set<String> STOP_TOKENS = new HashSet<String>()
	{
		{
			add("Query");
			add("StoredProc");
		}
	};
}
