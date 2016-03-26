package testplugin.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

public class CommonUtils
{

	public static String buildCalleeSignature(MethodWrapper caller) throws CoreException
	{
		String result = "";
		// add declaring type
		IMember member = caller.getMember();// SourceType st; st.getFullyQualifiedName()
		result += member.getDeclaringType().getFullyQualifiedName();
		IMethod m = (IMethod) member;
		// add method name
		result += " " + m.getElementName() + "(";
		ILocalVariable[] parameters = m.getParameters();
		for (int i = 0; i < parameters.length; i++)
		{
			ILocalVariable var = parameters[i];
			result += var.getSource();
			if (i < (parameters.length - 1))
			{
				result += ", ";
			}
		}
		result += ")";

		return result;
	}

	public static boolean atLeastOneOfParamsBelongsTo(MethodWrapper mw, Set<String> listOfTypes) throws CoreException
	{
		// mw.getMember().getCompilationUnit().getImports() !!!only if necessary!!!
		IMethod m = (IMethod) mw.getMember();
		for (ILocalVariable var : m.getParameters())
		{
			for (String type : listOfTypes)
			{
				if (var.getTypeSignature().endsWith(type + ";"))
				{
					return true;
				}
			}
		}
		return false;
	}

	public static String buildCallerSignature(MethodWrapper caller) throws CoreException
	{
		String result = "";
		// add declaring type
		IMember member = caller.getMember();
		result += member.getDeclaringType().getFullyQualifiedName();
		IMethod m = (IMethod) member;
		// add method name
		result += " " + m.getElementName() + "(";
		ILocalVariable[] parameters = m.getParameters();
		for (int i = 0; i < parameters.length; i++)
		{
			ILocalVariable var = parameters[i];
			result += var.getSource();
			if (i < (parameters.length - 1))
			{
				result += ", ";
			}
		}
		result += ")";

		return result;
	}

	public static IJavaProject getJavaProject(String projectName)
	{
		if (projectName == null)
		{
			throw new IllegalArgumentException("null projectName");
		}
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IJavaModel javaModel = JavaCore.create(workspaceRoot);
		IJavaProject javaProject = javaModel.getJavaProject(projectName);
		return javaProject;
	}

	public static IMethod findMethod(MethodSignature ms) throws CoreException
	{
		String fixedSignature = ms.fixed ? ms.method : fixSignature(ms.method);
		IType foundType = getJavaProject(ms.prjName).findType(ms.clazz);
		if (foundType != null)
		{

			for (IMethod method : foundType.getMethods())
			{
				if (method.toString().contains(fixedSignature))
				{
					return method;
				}
			}
		}
		return null;
	}

	public static IMethod findMethod(String prj, String klass, String signature) throws CoreException
	{
		String fixedSignature = fixSignature(signature);
		IType foundType = getJavaProject(prj).findType(klass);
		if (foundType != null)
		{

			// SearchPattern pattern = SearchPattern.createPattern(signature, IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_EQUIVALENT_MATCH);
			// SearchPattern pattern = SearchPattern.createPattern(signature, IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_CASE_SENSITIVE );
			// IJavaSearchScope javaSearchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { foundType });
			// SearchEngine searchEngine = new SearchEngine();
			// MyRequestor myReq = new MyRequestor();
			// searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, javaSearchScope, myReq, null);
			// return myReq.foundMethod;

			for (IMethod method : foundType.getMethods())
			{
				if (method.toString().contains(fixedSignature))
				{
					method.getParameterNames();
					// SearchPattern pattern = SearchPattern.createPattern(method, IJavaSearchConstants.DECLARATIONS);
					// IJavaSearchScope javaSearchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { foundType });
					// IJavaSearchScope wsScope = SearchEngine.createWorkspaceScope();
					// SearchEngine searchEngine = new SearchEngine();
					// SearchRequestor s;
					// MyRequestor myReq = new MyRequestor();
					// searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, javaSearchScope, myReq, null);
					// return myReq.foundMethod;
					return method;
				}
				// System.out.println(method.toString());
				// JavaModelManager.getJavaModelManager().peekAtInfo(method);
			}
		}
		return null;
	}

	private static String removeParametersNames(String original)
	{

		int indexOfFirstBracket = original.indexOf("(");
		String result = original.substring(0, (indexOfFirstBracket + 1));
		int indexOfSecondBracket = original.lastIndexOf(")");
		result += removeParamsNames(original.substring(indexOfFirstBracket + 1, indexOfSecondBracket));
		result += ")";
		return result;
	}

	private static String removeParamsNames(String orig)
	{
		String result = "";
		String[] tokens = orig.split("\\,");
		for (int k = 0; k < tokens.length; k++)
		{
			String token = tokens[k];
			String[] tkns = token.split("\\ ");
			for (int i = 0; i < tkns.length - 1; i++)
			{
				result += tkns[i];
			}
			if (k < (tokens.length - 1))
			{
				result += ", ";
			}
		}
		return result;
	}

	private static String fixSignature(String original)
	{
		// remove additional spaces
		String result = original.replaceAll("\\s+", " ");
		// replace ' ,' with ','
		result = result.replaceAll(" ,", ",");
		// //replace ', ' with ','
		// result = result.replaceAll(", ", ",");
		// replace ' (' with '('
		result = result.replaceAll(" \\(", "(");
		// replace '( ' with '('
		result = result.replaceAll("\\( ", "(");
		// replace ' )' with ')'
		result = result.replaceAll(" \\)", ")");
		// replace ') ' with ')'
		result = result.replaceAll("\\) ", ")");
		// trim result
		result = result.trim();
		// System.out.println("ORIGINAL:" + original + "\n" + "RESULT :" +
		// result);
		// remove parameters names
		result = removeParametersNames(result);
		System.out.println("ORIGINAL:" + original + "\n" + "RESULT  :" + result);

		return result.trim();
	}

	public static List<MethodSignature> readFile(File fin)
	{
		List<MethodSignature> result = new ArrayList<>();
		try
		{
			FileInputStream fis = new FileInputStream(fin);

			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			String line = null;
			while ((line = br.readLine()) != null)
			{
				String[] split = line.split("\\#");
				System.out.println(line);
				MethodSignature ms = new MethodSignature(split[0], split[1], split[2], (split.length >= 4) && "Y".equals(split[3]));
				result.add(ms);
			}
			br.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public static class MethodSignature
	{
		public final String prjName;
		public final String clazz;
		public final String method;
		public final boolean fixed;

		public MethodSignature(String prjName, String clazz, String method, boolean fixed)
		{
			super();
			this.prjName = prjName;
			this.clazz = clazz;
			this.method = method;
			this.fixed = fixed;
		}

		@Override
		public String toString()
		{
			return "MethodSignature [prjName=" + prjName + "clazz=" + clazz + ", method=" + method + "]";
		}
	}
}
