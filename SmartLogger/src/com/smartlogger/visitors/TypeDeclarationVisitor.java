package com.smartlogger.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class TypeDeclarationVisitor extends ASTVisitor {
	List<TypeDeclaration> classes = new ArrayList<TypeDeclaration>();

	@Override
	public boolean visit(TypeDeclaration node) {

		if (!node.isInterface() && node.isPackageMemberTypeDeclaration()) {
			classes.add(node);
		}

		return super.visit(node);
	}

	public List<TypeDeclaration> getClasses() {
		return classes;
	}
}
