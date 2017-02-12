package com.smartlogger.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class BlockVisitor extends ASTVisitor {
        List<Block> blocks = new ArrayList<Block>();
        
        @Override
        public boolean visit(Block node) {
                blocks.add(node);
                return super.visit(node);
        }

        public List<Block> getBlocks() {
                return blocks;
        }
}
