package com.smartlogger;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;

public class BlockUtil {

	public static boolean isBlockEligibleForLogging(Block block,
			String loggerFieldName) {

		ASTNode targetNode = null;

		ASTNode parent = block.getParent();
		List statements = block.statements();
		if (parent instanceof MethodDeclaration
				&& ((MethodDeclaration) parent).isConstructor()) {
			targetNode = block.statements().size() > 0 ? (ASTNode) statements
					.get(block.statements().size() - 1) : null;
		} else {
			targetNode = block.statements().size() > 0 ? (ASTNode) statements
					.get(0) : null;
		}

		return CommonUtil.isNotNull(targetNode)
				&& targetNode.toString().startsWith(loggerFieldName + ".");
	}

	public static boolean isEligibleForLogging(Block block) {
		return !MethodUtil.isSetterOrGetter(block);
	}

}
