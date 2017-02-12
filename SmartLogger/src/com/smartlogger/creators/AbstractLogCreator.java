package com.smartlogger.creators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.text.edits.TextEdit;

import com.smartlogger.CommonUtil;
import com.smartlogger.visitors.LoggerFieldVisitor;

public abstract class AbstractLogCreator {

	public abstract Statement createNewLoggerStatementFor(Block block, String loggerFieldName);

	public abstract String getLoggerImport();

	public static FieldDeclaration createNewLoggerDeclarationStatement(
			TypeDeclaration classBody) {
		AST unitAST = classBody.getAST();

		VariableDeclarationFragment fragment = unitAST
				.newVariableDeclarationFragment();
		fragment.setName(unitAST.newSimpleName("logger"));
		final FieldDeclaration declaration = unitAST
				.newFieldDeclaration(fragment);
		declaration.setType(unitAST.newSimpleType(unitAST.newName("Logger")));
		declaration.modifiers().addAll(
				ASTNodeFactory.newModifiers(unitAST, Modifier.PRIVATE
						| Modifier.STATIC | Modifier.FINAL));

		MethodInvocation methodInvocation = unitAST.newMethodInvocation();
		methodInvocation.setName(unitAST.newSimpleName("getLogger"));
		methodInvocation.setExpression(unitAST.newSimpleName("SCLogger"));
		fragment.setInitializer(methodInvocation);

		return declaration;

	}

	protected boolean handleErrorLoggingForCatchBlock(Block block,
			MethodInvocation newInvocation) {

		ASTNode parent = block.getParent();
		
		boolean isCatchClause = parent instanceof CatchClause;
		if (isCatchClause) {
			CatchClause catchClause = (CatchClause) parent;
			SingleVariableDeclaration exception = catchClause.getException();
			newInvocation.setName(block.getAST().newSimpleName("error"));
			newInvocation.arguments().add(
					block.getAST().newSimpleName(
							exception.getName().getFullyQualifiedName()));
			
		}
		
		return isCatchClause;

	}

	public TextEdit getTextEditForLoggerImport(CompilationUnit unit)
			throws CoreException {
		ImportRewrite importRewrite = ImportRewrite.create(unit, true);

		importRewrite.removeImport(CommonUtil.LOG4J_LOGGER_IMPORT);
		importRewrite.addImport(getLoggerImport());
		importRewrite.addImport("com.lg4j.Logger");
		return importRewrite.rewriteImports(null);
	}

}
