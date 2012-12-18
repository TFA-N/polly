package de.skuzzle.polly.parsing.ast.expressions;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.ast.declarations.types.Type;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.parsing.ast.visitor.Visitor;
import de.skuzzle.polly.parsing.types.FunctionType;
import de.skuzzle.polly.parsing.util.Stack;
import de.skuzzle.polly.parsing.ast.visitor.TypeResolver.CallContext;


/**
 * Expression that does nothing except to represent the type that has been set in the
 * Constructor.
 * 
 * It will be used to represent formal parameter types. Thus, if this instance 
 * represents a Function, it must pop one signature off the stack (if one exists) 
 * when being visited.
 * 
 * @author Simon Taddiken
 */
public class Empty extends Expression {

    private static final long serialVersionUID = 1L;

    private final transient Stack<CallContext> signatureStack;

    

    public Empty(Type type, Position position, Stack<CallContext> signatureStack) {
        super(position, type);
        this.signatureStack = signatureStack;
    }



    @Override
    public void visit(Visitor visitor) throws ASTTraversalException {
        if (this.getUnique() instanceof FunctionType
                && !this.signatureStack.isEmpty()) {
            this.signatureStack.pop();
        }
    }
}
