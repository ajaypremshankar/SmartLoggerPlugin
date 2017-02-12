package com.smartlogger.creators;

import java.util.List;

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
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
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
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;

import com.smartlogger.CommonUtil;
import com.smartlogger.MethodUtil;

public class Log4jLogCreator extends AbstractLogCreator{

	
	
	public Statement createNewLoggerStatementFor(Block block, String loggerFieldName) {
		AST blockAST = block.getAST();
		MethodInvocation newInvocation = blockAST.newMethodInvocation();
		newInvocation.setExpression(blockAST.newSimpleName(loggerFieldName!=null?loggerFieldName:"logger"));
		newInvocation.setName(blockAST.newSimpleName("debug"));
		StringLiteral sl = blockAST.newStringLiteral();
		Expression expr = setLoggerStatementLiteral(block, sl);
		newInvocation.arguments().add(expr);

		handleErrorLoggingForCatchBlock(block, newInvocation);

		Statement newStatement = blockAST.newExpressionStatement(newInvocation);
		return newStatement;
	}


	private Expression setLoggerStatementLiteral(Block block,
			StringLiteral sl) {

		Expression returnExpr = null;
		StringBuffer message = new StringBuffer();
		ASTNode parent = block.getParent();
		if (parent instanceof MethodDeclaration
				&& !((MethodDeclaration) parent).isConstructor()) {

			MethodDeclaration enclosingMethod = (MethodDeclaration) parent;
			message.append("Entering into method : ").append(
					enclosingMethod.getName());

			if (MethodUtil.hasParameters(enclosingMethod)) {

				InfixExpression expr = block.getAST().newInfixExpression();
				expr.setOperator(Operator.PLUS);

				sl.setLiteralValue(message.toString());

				expr.setLeftOperand(sl);

				appendMethodArgumentsInLoggerStatement(block, expr);

				returnExpr = expr;
			} else {
				sl.setLiteralValue(message.toString());
				returnExpr = sl;
			}

		} else if (parent instanceof MethodDeclaration) {
			MethodDeclaration enclosingConstructor = (MethodDeclaration) parent;
			message.append("Inside constructor of : ").append(
					enclosingConstructor.getName());
			if (MethodUtil.hasParameters(enclosingConstructor)) {

				InfixExpression expr = block.getAST().newInfixExpression();
				expr.setOperator(Operator.PLUS);

				sl.setLiteralValue(message.toString());

				expr.setLeftOperand(sl);

				appendMethodArgumentsInLoggerStatement(block, expr);

				returnExpr = expr;
			} else {
				sl.setLiteralValue(message.toString());
				returnExpr = sl;
			}
		} else if (parent instanceof IfStatement) {
			IfStatement parentIf = (IfStatement) parent;
			Statement elseStmt = parentIf.getElseStatement();
			if (CommonUtil.isNotNull(elseStmt) && elseStmt == block) {
				message.append("Entering into else-block");
			} else {
				message.append("Entering into if-block");
			}
			sl.setLiteralValue(message.toString());
			returnExpr = sl;
		} else if (parent instanceof ForStatement) {
			ForStatement parentFor = (ForStatement) parent;
			List initializersList = parentFor.initializers();
			VariableDeclarationExpression initializers = (VariableDeclarationExpression) (CommonUtil
					.isNotEmpty(initializersList) ? initializersList.get(0)
					: null);
			message.append("Entering into for-loop block with ");

			sl.setLiteralValue(message.toString());

			InfixExpression expr = block.getAST().newInfixExpression();
			expr.setOperator(Operator.PLUS);
			expr.setLeftOperand(sl);

			for (VariableDeclaration declarationFragment : (List<VariableDeclarationFragment>) initializers
					.fragments()) {
				setParamNameValueIntoAddtiveExpression(expr,
						declarationFragment);
			}

			returnExpr = expr;
		} else if (parent instanceof EnhancedForStatement) {
			EnhancedForStatement parentFor = (EnhancedForStatement) parent;
			message.append("Entering into enhanced-For loop block with ");
			sl.setLiteralValue(message.toString());

			InfixExpression expr = block.getAST().newInfixExpression();
			expr.setOperator(Operator.PLUS);
			expr.setLeftOperand(sl);

			setParamNameValueIntoAddtiveExpression(expr,
					parentFor.getParameter());

			returnExpr = expr;
		} else if (parent instanceof TryStatement) {

			TryStatement tryStmt = (TryStatement) parent;
			Block finallyBlock = tryStmt.getFinally();

			if (CommonUtil.isNotNull(finallyBlock) && finallyBlock == block) {
				message.append("Entering into finally-block");
				sl.setLiteralValue(message.toString());

				returnExpr = sl;
			} else if (tryStmt.getBody() == block) {
				message.append("Entering into try-block");
				sl.setLiteralValue(message.toString());

				returnExpr = sl;
			}

		} else if (parent instanceof CatchClause) {

			message.append("Entering into the catch-block with an exception : ");

			sl.setLiteralValue(message.toString());
			returnExpr = sl;

		} else if (parent instanceof WhileStatement) {
			WhileStatement parentWhile = (WhileStatement) parent;
			message.append("Entering into the while-loop");

			sl.setLiteralValue(message.toString());
			returnExpr = sl;

		} else if (parent instanceof DoStatement) {
			DoStatement parentDo = (DoStatement) parent;
			message.append("Entering into the while-loop");

			sl.setLiteralValue(message.toString());
			returnExpr = sl;

		} else if (parent instanceof Initializer) {
			Initializer parentInit = (Initializer) parent;

			message.append("Entering into the Initializer block");

			sl.setLiteralValue(message.toString());
			returnExpr = sl;

		} else {
			message.append("Entering into the block");

			sl.setLiteralValue(message.toString());
			returnExpr = sl;
		}

		return returnExpr;
	}

	private void setParamNameValueIntoAddtiveExpression(
			InfixExpression infixExpression, VariableDeclaration arg) {
		StringLiteral paramNameWithColon = infixExpression.getAST()
				.newStringLiteral();
		paramNameWithColon.setLiteralValue(arg.getName()
				.getFullyQualifiedName() + " : ");
		setOperandIntoAdditiveExpression(infixExpression, paramNameWithColon);

		SimpleName paramValue = infixExpression.getAST().newSimpleName(
				arg.getName().getFullyQualifiedName());
		setOperandIntoAdditiveExpression(infixExpression, paramValue);

		StringLiteral commaSep = infixExpression.getAST().newStringLiteral();
		commaSep.setLiteralValue(", ");
		setOperandIntoAdditiveExpression(infixExpression, commaSep);
	}

	private void setOperandIntoAdditiveExpression(InfixExpression infix,
			Expression operandExpr) {
		if (infix.getRightOperand().toString() == null
				|| "MISSING".equals(infix.getRightOperand().toString())) {
			infix.setRightOperand(operandExpr);
		} else {

			infix.extendedOperands().add(operandExpr);
		}
	}

	private void appendMethodArgumentsInLoggerStatement(Block block,
			InfixExpression infixExpression) {

		MethodDeclaration methodDeclaration = (MethodDeclaration) block
				.getParent();
		List<SingleVariableDeclaration> params = (List<SingleVariableDeclaration>) methodDeclaration
				.parameters();
		StringBuilder message = new StringBuilder();

		if (CommonUtil.isNullOrEmpty(params)) {
			message.append(" with arguements ");
		}

		StringLiteral sl = block.getAST().newStringLiteral();
		infixExpression.setOperator(Operator.PLUS);

		sl.setLiteralValue(message.toString());

		infixExpression.setRightOperand(sl);

		for (SingleVariableDeclaration arg : params) {
			setParamNameValueIntoAddtiveExpression(infixExpression,
					arg);
		}
	}


	@Override
	public String getLoggerImport() {
		return CommonUtil.LOG4J_LOGGER_IMPORT;
	}



}
