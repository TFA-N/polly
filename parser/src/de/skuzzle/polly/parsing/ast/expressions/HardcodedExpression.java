package de.skuzzle.polly.parsing.ast.expressions;

import java.util.LinkedList;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.expressions.literals.Literal;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.parsing.ast.visitor.Visitor;
import de.skuzzle.polly.parsing.types.Type;


/**
 * This expression must be used for preprovided functionality for example operators.
 * 
 * @author Simon Taddiken
 */
public abstract class HardcodedExpression extends Expression {

    /**
     * Creates new HardcodedExpression with the given type.
     * 
     * @param type Type of this expression.
     */
    public HardcodedExpression(Type type) {
        super(Position.EMPTY, type);
    }
    
    
    
    /**
     * Executes this hardcoded expression. Execution of this expression may leave exactly
     * one new literal of the same type as this expression on the stack.
     * 
     * @param stack The stack used for execution. 
     * @param ns The current execution namespace.
     * @param execVisitor The visitor that is responsible for the execution of the AST.
     * @throws ASTTraversalException If executing fails.
     */
    public abstract void execute(LinkedList<Literal> stack, Namespace ns, 
            Visitor execVisitor) throws ASTTraversalException;
    
    
    
    @Override
    public void visit(Visitor visitor) throws ASTTraversalException {
        visitor.visitHardCoded(this);
    }
}
