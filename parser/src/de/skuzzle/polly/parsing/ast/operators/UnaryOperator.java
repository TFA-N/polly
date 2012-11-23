package de.skuzzle.polly.parsing.ast.operators;

import java.util.Arrays;
import java.util.Collection;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.Stack;
import de.skuzzle.polly.parsing.ast.declarations.Declaration;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.declarations.Parameter;
import de.skuzzle.polly.parsing.ast.declarations.VarDeclaration;
import de.skuzzle.polly.parsing.ast.expressions.Identifier;
import de.skuzzle.polly.parsing.ast.expressions.ResolvableIdentifier;
import de.skuzzle.polly.parsing.ast.expressions.literals.FunctionLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.Literal;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.parsing.ast.visitor.Visitor;
import de.skuzzle.polly.parsing.types.FunctionType;
import de.skuzzle.polly.parsing.types.Type;


public abstract class UnaryOperator<O extends Literal> extends Operator {

    protected final static String PARAM_NAME = "$param";
    private final Type operandType;
    
    
    
    public UnaryOperator(OpType op, Type resultType, Type operandType) {
        super(op, resultType);
        this.operandType = operandType;
    }

    
    
    @Override
    public Declaration createDeclaration() {
        Collection<Parameter> p = Arrays.asList(new Parameter[] {
            new Parameter(Position.EMPTY, new ResolvableIdentifier(Position.EMPTY, 
                PARAM_NAME), this.operandType)});
        
        final FunctionLiteral func = new FunctionLiteral(Position.EMPTY, p, this);
        func.setType(new FunctionType(this.getType(), Parameter.asType(p)));
        func.setReturnType(this.getType());
        
        final Identifier fakeId = new Identifier(Position.EMPTY, this.getOp().getId());
        return new VarDeclaration(func.getPosition(), fakeId, func);
    }

    
    
    @Override
    @SuppressWarnings("unchecked")
    public void execute(Stack<Literal> stack, Namespace ns, Visitor execVisitor)
            throws ASTTraversalException {
        final O operand = (O) ns.resolveVar(
            new ResolvableIdentifier(this.getPosition(), PARAM_NAME), 
            Type.ANY).getExpression();
        
        this.exec(stack, ns, operand, operand.getPosition());
    }

    
    
    /**
     * Called to generate the result of this operator on the stack.
     * 
     * @param stack The current execution stack.
     * @param ns The current execution namespace.
     * @param operand Operand literal of this operator.
     * @param resultPos Position that can be used as position for the result literal.
     * @throws ASTTraversalException If executing fails for any reason.
     */
    protected abstract void exec(Stack<Literal> stack, Namespace ns, 
        O operand, Position resultPos) throws ASTTraversalException;
}