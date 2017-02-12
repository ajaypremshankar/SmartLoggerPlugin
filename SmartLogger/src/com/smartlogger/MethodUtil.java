package com.smartlogger;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class MethodUtil {
	public static boolean hasParameters(MethodDeclaration methodDeclaration) {
		List<SingleVariableDeclaration> params = (List<SingleVariableDeclaration>) methodDeclaration
				.parameters();

		return CommonUtil.isNotEmpty(params);
	}
	
	public static boolean isSetterOrGetter(Block block) {

		boolean isSetterOrGetter = false;
		ASTNode parent = block.getParent();
		if (parent instanceof MethodDeclaration
				&& !((MethodDeclaration) parent).isConstructor()) {

			List statementsInBlock = block.statements();

			
			if (CommonUtil.isNotNull(statementsInBlock) && statementsInBlock.size() == 1) {
				ASTNode firstStatement = (ASTNode) statementsInBlock.get(0);
				if (firstStatement instanceof ReturnStatement
						|| firstStatement instanceof ExpressionStatement) {
					isSetterOrGetter = true;
				}

			}else if(CommonUtil.isNullOrEmpty(statementsInBlock)){
				isSetterOrGetter = true;
			}
		}
		return isSetterOrGetter;
	}
	
	
}
