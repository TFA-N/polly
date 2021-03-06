package de.skuzzle.polly.core.parser.ast.expressions;

import java.util.Arrays;
import java.util.List;

import de.skuzzle.polly.core.parser.Position;
import de.skuzzle.polly.core.parser.ast.ResolvableIdentifier;
import de.skuzzle.polly.core.parser.ast.expressions.literals.ProductLiteral;
import de.skuzzle.polly.core.parser.ast.lang.Operator;
import de.skuzzle.polly.core.parser.ast.lang.Operator.OpType;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversal;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.core.parser.ast.visitor.ASTVisitor;
import de.skuzzle.polly.core.parser.ast.visitor.Transformation;


/**
 * Represents a call of an operator. OperatorCalls can be created using one of the static 
 * factory methods.
 *  
 * @author Simon Taddiken
 */
public class OperatorCall extends Call {
    
    /**
     * Creates a new OperatorCall to a binary expression which has two operands.
     * 
     * @param position Position of the call within the input.
     * @param operator The called operator.
     * @param left The left operand.
     * @param right The right operand.
     * @return The created Operator call.
     */
    public final static OperatorCall binary(Position position, OpType operator, 
            Expression left, Expression right) {
        return new OperatorCall(position, operator, 
            Arrays.asList(new Expression[] {left, right}), false);
    }
    
    
    
    /**
     * Creates a new OperatorCall to a unary expression which has one operand.
     * 
     * @param position Position of the call within the input.
     * @param operator The called operator.
     * @param operand The operand of the operator.
     * @param postfix Whether this is a postfix operator. If <code>false</code>, this 
     *          operator is assumed to be prefix.
     * @return The created Operator call.
     */
    public final static OperatorCall unary(Position position, OpType operator, 
            Expression operand, boolean postfix) {
        return new OperatorCall(position, operator, 
            Arrays.asList(new Expression[] {operand}), postfix);
    }
    
    
    
    /**
     * Creates a new OperatorCall to a ternary expression which has three operands.
     * 
     * @param position Position of the call within the input.
     * @param operator The called operator.
     * @param operand1 The first operand.
     * @param operand2 The second operand.
     * @param operand3 The third operand.
     * @return The created Operator call.
     */
    public final static OperatorCall ternary(Position position, OpType operator, 
            Expression operand1, Expression operand2, Expression operand3) {
        return new OperatorCall(position, operator, 
            Arrays.asList(new Expression[] {operand1, operand2, operand3}), false);
    }
    
    

    private final OpType operator;
    private final boolean postfix;
    
    
    
    /**
     * Creates a new Operator call.
     * 
     * @param position Position of the call within the source.
     * @param operator The called operator.
     * @param parameters The actual parameters of the call.
     * @param postfix Whether this is a postfix operator. If <code>false</code>, this 
     *          operator is assumed to be prefix (only taken into account for unary 
     *          operators.)
     */
    public OperatorCall(Position position, OpType operator, 
            List<Expression> parameters, boolean postfix) {
        super(position, 
            new VarAccess(position, new ResolvableIdentifier(position, 
                operator.getId())),
            new ProductLiteral(position, parameters));
        this.operator = operator;
        this.postfix = postfix;
    }


    
    /**
     * Gets the operator type that is being called.
     * 
     * @return The operator type.
     */
    public Operator.OpType getOperator() {
        return this.operator;
    }
    
    
    
    /**
     * For unary operators, returns whether it is a postix or prefix op. For other 
     * operators, the result is undefined but likely to be <code>false</code>.
     * 
     * @return Whether this is a postfix operator.
     */
    public boolean isPostfix() {
        return this.postfix;
    }

    
    
    @Override
    public boolean visit(ASTVisitor visitor) throws ASTTraversalException {
        return visitor.visit(this);
    }
    
    
    
    @Override
    public Expression transform(Transformation transformation)
            throws ASTTraversalException {
        return transformation.transformOperatorCall(this);
    }
    
    
    @Override
    public boolean traverse(ASTTraversal visitor) throws ASTTraversalException {
        switch (visitor.before(this)) {
        case ASTTraversal.SKIP: return true;
        case ASTTraversal.ABORT: return false;
        }
        if (!this.getLhs().traverse(visitor)) {
            return false;
        }
        if (!this.getRhs().traverse(visitor)) {
            return false;
        }
        return visitor.after(this) == ASTTraversal.CONTINUE;
    }
    
    
    
    @Override
    public String toString() {
        return "[OpCall: " + this.operator.getId() + 
            ", type:" + this.getUnique() + "]";
    }
}
