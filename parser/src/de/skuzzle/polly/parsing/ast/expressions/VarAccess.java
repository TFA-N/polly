package de.skuzzle.polly.parsing.ast.expressions;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.ast.ResolvableIdentifier;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.parsing.ast.visitor.Transformation;
import de.skuzzle.polly.parsing.ast.visitor.ASTVisitor;


/**
 * Represents the access of a variable.
 * 
 * @author Simon Taddiken
 */
public class VarAccess extends Expression {

    private static final long serialVersionUID = 1L;
    
    private ResolvableIdentifier identifier;
    
    
    public VarAccess(Position position, ResolvableIdentifier identifier) {
        super(position);
        if (identifier == null) {
            throw new NullPointerException("identifier is null");
        }
        this.identifier = identifier;
    }
    
    
    
    /**
     * Gets the name of the variable that is accessed.
     * 
     * @return The variables name.
     */
    public ResolvableIdentifier getIdentifier() {
        return this.identifier;
    }

    
    
    @Override
    public void visit(ASTVisitor visitor) throws ASTTraversalException {
        visitor.visitVarAccess(this);
    }
    
    
    @Override
    public Expression transform(Transformation transformation) 
            throws ASTTraversalException {
        return transformation.transformVarAccess(this);
    }
    
    
    @Override
    public String toString() {
        return "[VarAccess: " + this.identifier + ", " + super.toString() + "]";
    }
}
