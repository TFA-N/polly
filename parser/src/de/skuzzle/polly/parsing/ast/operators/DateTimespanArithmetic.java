package de.skuzzle.polly.parsing.ast.operators;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.Stack;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.expressions.literals.DateLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.Literal;
import de.skuzzle.polly.parsing.ast.expressions.literals.TimespanLiteral;
import de.skuzzle.polly.parsing.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.parsing.types.Type;


public class DateTimespanArithmetic extends BinaryOperator<DateLiteral, TimespanLiteral>{

    public DateTimespanArithmetic(OpType id) {
        super(id, Type.DATE, Type.DATE, Type.TIMESPAN);
    }
    
    

    @Override
    protected void exec(Stack<Literal> stack, Namespace ns, DateLiteral left,
            TimespanLiteral right, Position resultPos) throws ASTTraversalException {
        
        switch (this.getOp()) {
            case ADD:
                stack.push(new DateLiteral(resultPos, right.addToDate(left.getValue())));
                break;
            case SUB:
                final TimespanLiteral tmp = new TimespanLiteral(right.getPosition(), 
                    -right.getSeconds());
                stack.push(new DateLiteral(resultPos, tmp.addToDate(left.getValue())));
                break;
            default:
                this.invalidOperatorType(this.getOp());
        }
    }
    

}
