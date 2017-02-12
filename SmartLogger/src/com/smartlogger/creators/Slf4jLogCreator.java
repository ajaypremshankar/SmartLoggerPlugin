package com.smartlogger.creators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import com.smartlogger.CommonUtil;
import com.smartlogger.MethodUtil;

public class Slf4jLogCreator extends AbstractLogCreator {

	@Override
	public Statement createNewLoggerStatementFor(Block block, String loggerFieldName) {
		AST blockAST = block.getAST();
		MethodInvocation newInvocation = blockAST.newMethodInvocation();
		newInvocation.setExpression(blockAST.newSimpleName(loggerFieldName!=null?loggerFieldName:"logger"));
		newInvocation.setName(blockAST.newSimpleName("debug"));
		StringLiteral sl = blockAST.newStringLiteral();
		List<SimpleName> argumentsForLogging = setLoggerStatementLiteral(block,
				sl);
		newInvocation.arguments().add(sl);

		boolean isCatchBlock = handleErrorLoggingForCatchBlock(block,
				newInvocation);

		if (!isCatchBlock && CommonUtil.isNotEmpty(argumentsForLogging)) {
			newInvocation.arguments().addAll(argumentsForLogging);
		}

		Statement newStatement = blockAST.newExpressionStatement(newInvocation);
		return newStatement;
	}

	private List<SimpleName> setLoggerStatementLiteral(Block block,
			StringLiteral sl) {

		List<SimpleName> argumentsForLogging = new ArrayList<SimpleName>();

		StringBuffer message = new StringBuffer();
		ASTNode parent = block.getParent();
		if (parent instanceof MethodDeclaration
				&& !((MethodDeclaration) parent).isConstructor()) {

			MethodDeclaration enclosingMethod = (MethodDeclaration) parent;
			message.append("Entering into method : ").append(
					enclosingMethod.getName());

			if (MethodUtil.hasParameters(enclosingMethod)) {
				appendMethodArgumentsInLoggerStatement(block, message,
						argumentsForLogging);
			}

		} else if (parent instanceof MethodDeclaration) {
			MethodDeclaration enclosingConstructor = (MethodDeclaration) parent;
			message.append("Inside constructor of : ").append(
					enclosingConstructor.getName());

			if (MethodUtil.hasParameters(enclosingConstructor)) {
				appendMethodArgumentsInLoggerStatement(block, message,
						argumentsForLogging);
			}

		} else if (parent instanceof IfStatement) {
			IfStatement parentIf = (IfStatement) parent;
			Statement elseStmt = parentIf.getElseStatement();
			if (CommonUtil.isNotNull(elseStmt) && elseStmt == block) {
				message.append("Entering into else-block");
			} else {
				message.append("Entering into if-block");
			}

		} else if (parent instanceof ForStatement) {
			ForStatement parentFor = (ForStatement) parent;
			List initializersList = parentFor.initializers();
			Expression initializers = (Expression) (CommonUtil
					.isNotEmpty(initializersList) ? initializersList.get(0)
					: null);
			message.append("Entering into for-loop block. ");

			if (initializers instanceof VariableDeclarationExpression) {
				for (VariableDeclaration declarationFragment : (List<VariableDeclarationFragment>) ((VariableDeclarationExpression) initializers)
						.fragments()) {
					setParamNameValueIntoAddtiveExpression(message,
							declarationFragment);
				}
			}

		} else if (parent instanceof EnhancedForStatement) {
			EnhancedForStatement parentFor = (EnhancedForStatement) parent;
			message.append("Entering into enhanced-For loop block with ");

			setParamNameValueIntoAddtiveExpression(message,
					parentFor.getParameter());
		} else if (parent instanceof TryStatement) {

			TryStatement tryStmt = (TryStatement) parent;
			Block finallyBlock = tryStmt.getFinally();

			if (CommonUtil.isNotNull(finallyBlock) && finallyBlock == block) {
				message.append("Entering into finally-block");
			} else if (tryStmt.getBody() == block) {
				message.append("Entering into try-block");
			}

		} else if (parent instanceof CatchClause) {

			message.append("Entering into the catch-block with an exception : ");

		} else if (parent instanceof WhileStatement) {
			WhileStatement parentWhile = (WhileStatement) parent;
			message.append("Entering into the while-loop");

		} else if (parent instanceof DoStatement) {
			DoStatement parentDo = (DoStatement) parent;
			message.append("Entering into the while-loop");

		} else if (parent instanceof Initializer) {
			Initializer parentInit = (Initializer) parent;

			message.append("Entering into the Initializer block");

		} else {
			message.append("Entering into the block");
		}

		sl.setLiteralValue(message.toString());

		return argumentsForLogging;
	}

	private void appendMethodArgumentsInLoggerStatement(Block block,
			StringBuffer message, List<SimpleName> argsForLogging) {

		MethodDeclaration methodDeclaration = (MethodDeclaration) block
				.getParent();
		List<SingleVariableDeclaration> params = (List<SingleVariableDeclaration>) methodDeclaration
				.parameters();

		if (CommonUtil.isNotEmpty(params)) {
			message.append(" with arguements ");
		}

		for (SingleVariableDeclaration arg : params) {
			setParamNameValueIntoAddtiveExpression(message, arg);
			argsForLogging.add(block.getAST().newSimpleName(
					arg.getName().getFullyQualifiedName()));
		}
	}

	private void setParamNameValueIntoAddtiveExpression(StringBuffer message,
			VariableDeclaration arg) {

		message.append(" ").append(arg.getName().getFullyQualifiedName())
				.append(" : ").append("{} ,");
	}

	@Override
	public String getLoggerImport() {
		return CommonUtil.SLF4J_LOGGER_IMPORT;
	}

}
