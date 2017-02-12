package com.smartlogger.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.smartlogger.CommonUtil;

public class LoggerFieldVisitor extends ASTVisitor {
	List<FieldDeclaration> loggers = new ArrayList<FieldDeclaration>();

	@Override
	public boolean visit(FieldDeclaration node) {
		List<VariableDeclarationFragment> fragments = (List<VariableDeclarationFragment>) node
				.fragments();
		VariableDeclarationFragment declarationFragment = fragments.get(0);
		
		//CommonUtil.LOGGER_NAME.equals(declarationFragment.getName().getFullyQualifiedName())

		if (CommonUtil.LOGGER_TYPE.equals(node.getType().toString())) {
			loggers.add(node);
		}

		return super.visit(node);
	}

	public List<FieldDeclaration> getLoggers() {
		return loggers;
	}
}
