package com.smartlogger;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.smartlogger.creators.AbstractLogCreator;
import com.smartlogger.creators.Slf4jLogCreator;
import com.smartlogger.visitors.BlockVisitor;
import com.smartlogger.visitors.LoggerFieldVisitor;
import com.smartlogger.visitors.TypeDeclarationVisitor;

public class PluginManager extends AbstractHandler {

	private static AbstractLogCreator logCreator = new Slf4jLogCreator();

	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";

	TextEdit edits = null;

	private String loggerFieldName;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		// Loop over all projects
		for (IProject project : projects) {
			try {
				if (project.isNatureEnabled(JDT_NATURE)) {
					analyseBlocks(project);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (MalformedTreeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	private void analyseBlocks(IProject project) throws MalformedTreeException,
			BadLocationException, IOException, IllegalArgumentException,
			CoreException {

		IPackageFragment[] packages = JavaCore.create(project)
				.getPackageFragments();
		// parse(JavaCore.create(project));
		for (IPackageFragment mypackage : packages) {
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				createAST(mypackage);
			}

		}
	}

	/*
	 * private void initProperties() throws IOException { InputStream is =
	 * this.getClass().getResourceAsStream( "/config.properties"); properties =
	 * new Properties(); properties.load(is);
	 * 
	 * String exludeUnits = (String) properties.get("ExcludeUnits");
	 * 
	 * unitExclusionList = Collections.EMPTY_LIST; if (exludeUnits != null &&
	 * !"".equals(exludeUnits)) { unitExclusionList =
	 * Arrays.asList(exludeUnits.split(",")); unitExclusionList.remove(""); }
	 * 
	 * String exludePackages = (String) properties.get("ExcludePackages");
	 * 
	 * packageExclusionList = Collections.EMPTY_LIST; if (exludeUnits != null &&
	 * !"".equals(exludeUnits)) { packageExclusionList =
	 * Arrays.asList(exludePackages.split(","));
	 * packageExclusionList.remove(""); }
	 * 
	 * }
	 */
	private void createAST(IPackageFragment mypackage)
			throws MalformedTreeException, BadLocationException, IOException,
			IllegalArgumentException, CoreException {
		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {

			if (!CommonUtil.isLoggingRequiredFor(unit)) {
				System.out.println("Test file is being skipped : "
						+ unit.getElementName());
				continue;
			}

			CompilationUnit parse = parse(unit, ASTParser.K_COMPILATION_UNIT);

			// Edit for Logger import
			edits = CommonUtil.createEditOrAddChild(edits,
					logCreator.getTextEditForLoggerImport(parse));

			TypeDeclarationVisitor typeVisitor = new TypeDeclarationVisitor();
			parse.accept(typeVisitor);

			for (TypeDeclaration typeDeclaration : typeVisitor.getClasses()) {
				createLoggerDeclarationForType(typeDeclaration, parse);
				createBlockASTForType(typeDeclaration, parse);
				loggerFieldName = null;
			}
			applyEditsToTheUnit(unit);

		}
	}

	private void createLoggerDeclarationForType(TypeDeclaration type,
			CompilationUnit parse) throws MalformedTreeException,
			IllegalArgumentException, BadLocationException, CoreException {

		LoggerFieldVisitor visitor = new LoggerFieldVisitor();
		type.accept(visitor);

		if (visitor.getLoggers().size() == 0) {

			FieldDeclaration newStatement = AbstractLogCreator
					.createNewLoggerDeclarationStatement(type);

			writeFieldDeclarationIntoUnit(parse, type, newStatement);
		} else {
			FieldDeclaration node = visitor.getLoggers().get(0);
			List<VariableDeclarationFragment> fragments = (List<VariableDeclarationFragment>) node
					.fragments();
			VariableDeclarationFragment declarationFragment = fragments.get(0);

			loggerFieldName = declarationFragment.getName()
					.getFullyQualifiedName();

			System.out.println("This unit already has logger declared");
		}

	}

	private TextEdit writeFieldDeclarationIntoUnit(CompilationUnit unit,
			TypeDeclaration type, FieldDeclaration fieldDeclaration)
			throws MalformedTreeException, IllegalArgumentException,
			CoreException {

		ASTRewrite rewriter = ASTRewrite.create(type.getAST());

		ListRewrite listRewrite = rewriter.getListRewrite(type,
				TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		listRewrite.insertFirst(fieldDeclaration, null);

		edits = CommonUtil.createEditOrAddChild(edits, rewriter.rewriteAST());
		return edits;
	}

	public void createBlockASTForType(TypeDeclaration type,
			CompilationUnit parse) throws JavaModelException,
			MalformedTreeException, BadLocationException {

		// Get a block visitor to get blocks
		BlockVisitor visitor = new BlockVisitor();
		type.accept(visitor);

		for (Block block : visitor.getBlocks()) {

			if (!BlockUtil.isBlockEligibleForLogging(block, loggerFieldName)) {

				Statement newStatement = logCreator
						.createNewLoggerStatementFor(block, loggerFieldName);

				// create ListRewrite

				writeStatementIntoUnit(block, newStatement);

			} else {
				System.out.println("This block already contains logger");
				continue;
			}

		}

	}

	private void applyEditsToTheUnit(ICompilationUnit unit)
			throws MalformedTreeException, BadLocationException,
			JavaModelException {

		Document document = new Document(unit.getSource());

		if (CommonUtil.isNotNull(edits)) {
			edits.apply(document);

			// this is the code for adding statements
			unit.getBuffer().setContents(document.get());

			unit.getBuffer().save(null, true);
		}
		edits = null;

	}

	private TextEdit writeStatementIntoUnit(Block block, Statement newStatement)
			throws MalformedTreeException, JavaModelException,
			IllegalArgumentException {
		ASTRewrite rewriter = ASTRewrite.create(block.getAST());
		ListRewrite listRewrite = rewriter.getListRewrite(block,
				Block.STATEMENTS_PROPERTY);

		ASTNode parent = block.getParent();
		if (parent instanceof MethodDeclaration
				&& ((MethodDeclaration) parent).isConstructor()) {
			listRewrite.insertLast(newStatement, null);
		} else {
			listRewrite.insertFirst(newStatement, null);
		}

		edits = CommonUtil.createEditOrAddChild(edits, rewriter.rewriteAST());

		return edits;
	}

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the
	 * Java source file
	 * 
	 * @param unit
	 * @return
	 */

	private static CompilationUnit parse(ICompilationUnit unit, int kind) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(kind);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}
}
