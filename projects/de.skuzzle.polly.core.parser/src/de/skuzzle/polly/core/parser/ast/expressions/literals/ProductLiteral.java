package de.skuzzle.polly.core.parser.ast.expressions.literals;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.skuzzle.polly.core.parser.Position;
import de.skuzzle.polly.core.parser.ast.declarations.types.ProductType;
import de.skuzzle.polly.core.parser.ast.declarations.types.Substitution;
import de.skuzzle.polly.core.parser.ast.declarations.types.Type;
import de.skuzzle.polly.core.parser.ast.expressions.Expression;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversal;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.core.parser.ast.visitor.ASTVisitor;
import de.skuzzle.polly.core.parser.ast.visitor.Transformation;


public class ProductLiteral extends ListLiteral {
    
    public ProductLiteral(Position position, List<Expression> content) {
        super(position, content);
    }
    
    
    
    @Override
    public void setUnique(Type unique) {
        if (unique == Type.UNKNOWN) {
            super.setUnique(unique);
            return;
        } else if (!(unique instanceof ProductType)) {
            throw new IllegalArgumentException("no product type");
        }
        super.setUnique(unique);
        final ProductType ptc = (ProductType) unique;
        final Iterator<Type> typeIt = ptc.getTypes().iterator();
        for (final Expression exp : this.getContent()) {
            exp.setUnique(typeIt.next());
        }
    }
    
    
    
    @Override
    public boolean addType(Type type) {
        if (!(type instanceof ProductType)) {
            throw new IllegalArgumentException("no product type");
        }
        super.addType(type);
        final ProductType ptc = (ProductType) type;
        final Iterator<Type> typeIt = ptc.getTypes().iterator();
        for (final Expression exp : this.getContent()) {
            exp.addType(typeIt.next());
        }
        return true;
    }
    
    
    @Override
    public boolean addType(Type type, Substitution constraint) {
        if (!(type instanceof ProductType)) {
            throw new IllegalArgumentException("no product type");
        }
        super.addType(type, constraint);
        final ProductType ptc = (ProductType) type;
        final Iterator<Type> typeIt = ptc.getTypes().iterator();
        for (final Expression exp : this.getContent()) {
            exp.addType(typeIt.next(), constraint);
        }
        return true;
    }
    
    
    
    @Override
    public void setTypes(Collection<Type> types) {
        for (final Expression exp : this.getContent()) {
            exp.getTypes().clear();
        }
        super.setTypes(types);
    }
    
    
    
    @Override
    public boolean visit(ASTVisitor visitor) throws ASTTraversalException {
        return visitor.visit(this);
    }
    
    
    @Override
    public ProductLiteral transform(Transformation transformation)
            throws ASTTraversalException {
        return transformation.transformProduct(this);
    }
    
    
    
    @Override
    public boolean traverse(ASTTraversal visitor) throws ASTTraversalException {
        switch (visitor.before(this)) {
        case ASTTraversal.SKIP: return true;
        case ASTTraversal.ABORT: return false;
        }
        for (final Expression exp : this.getContent()) {
            if (!exp.traverse(visitor)) {
                return false;
            }
        }
        return visitor.after(this) == ASTTraversal.CONTINUE;
    }
}
