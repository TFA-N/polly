package de.skuzzle.polly.parsing.ast.expressions;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversal;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.parsing.ast.visitor.Transformation;
import de.skuzzle.polly.parsing.ast.visitor.ASTVisitor;


/**
 * This expression allows to access declarations in different namespaces than the current
 * execution context. 
 * 
 * @author Simon Taddiken
 */
public class NamespaceAccess extends Expression {

    private static final long serialVersionUID = 1L;
    
    private Expression lhs;
    private Expression rhs;
    
    
    
    /**
     * Creates a new NamespaceAccess.
     * 
     * @param position Position of this expression within the source.
     * @param lhs Left handed side of this access. Must be of type {@link VarAccess} 
     *          (which represents the name of the {@link Namespace} to access.
     * @param rhs Right handed side of this access. Must be of type {@link VarAccess}
     *          and represents the name of the variable in the namespace being accessed. 
     */
    public NamespaceAccess(Position position, Expression lhs, Expression rhs) {
        super(position);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    
    
    /**
     * Gets the left handed side of this expression (the namespace being accessed).
     * 
     * @return The expression which represents the namespace being accessed.
     */
    public Expression getLhs() {
        return this.lhs;
    }
    
    
    
    /**
     * Gets the right handed side of this expression (variable in the namespace being 
     * accessed).
     * 
     * @return The expression which represents the variable being accessed.
     */
    public Expression getRhs() {
        return this.rhs;
    }

    
    
    @Override
    public boolean visit(ASTVisitor visitor) throws ASTTraversalException {
        return visitor.visit(this);
    }
    
    
    
    @Override
    public Expression transform(Transformation transformation) 
            throws ASTTraversalException {
        return transformation.transformAccess(this);
    }
    
    
    
    @Override
    public boolean traverse(ASTTraversal visitor) throws ASTTraversalException {
        switch (visitor.before(this)) {
        case ASTTraversal.SKIP: return true;
        case ASTTraversal.ABORT: return false;
        }
        if (!this.lhs.traverse(visitor)) {
            return false;
        }
        if (!this.rhs.traverse(visitor)) {
            return false;
        }
        return visitor.after(this) == ASTTraversal.CONTINUE;
    }
}
