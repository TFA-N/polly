package de.skuzzle.polly.core.parser.ast.lang.operators;

import java.util.ArrayList;
import java.util.List;

import de.skuzzle.polly.core.parser.Position;
import de.skuzzle.polly.core.parser.ast.declarations.Namespace;
import de.skuzzle.polly.core.parser.ast.declarations.types.Type;
import de.skuzzle.polly.core.parser.ast.expressions.Expression;
import de.skuzzle.polly.core.parser.ast.expressions.literals.ListLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.Literal;
import de.skuzzle.polly.core.parser.ast.expressions.literals.NumberLiteral;
import de.skuzzle.polly.core.parser.ast.lang.TernaryOperator;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.core.parser.ast.visitor.ExecutionVisitor;
import de.skuzzle.polly.core.parser.problems.ProblemReporter;
import de.skuzzle.polly.core.parser.problems.Problems;
import de.skuzzle.polly.tools.collections.Stack;


public class TernaryDotDot extends 
        TernaryOperator<NumberLiteral, NumberLiteral, NumberLiteral> {

    /** Maximum size for generated ListLiterals */
    public final static int MAX_LIST_SIZE = 10000;
    
    
    
    /**
     * Creates a ListLiteral that contains numbers from <code>first</code> until
     * <code>second</code>
     * 
     * @param first First number in the generated list.
     * @param second Last number in the generated list.
     * @param step Step between each generated number.
     * @param resultPos Suitable position for the resulting literal.
     * @param reporter ProblemReporter to report possible errors.
     * @return New ListLiteral containing the sequence of numbers.
     * @throws ASTTraversalException If start or end definitions are illegal.
     */
    public static ListLiteral createSequence(NumberLiteral first, NumberLiteral second, 
            NumberLiteral step, Position resultPos, ProblemReporter reporter) 
                throws ASTTraversalException {
        
        double start = first.getValue();
        double end = second.getValue();
        double s = step.getValue();
        double values = start;
        
        if (start > end && s > 0.0) {
            reporter.runtimeProblem(Problems.ILLEGAL_INDIZES, resultPos, start, end);
        } else if (start > end) {
            double tmp = start;
            start = end;
            end = tmp;
        }
        
        double listSize = Math.abs(end - start) / s;
        if (listSize > MAX_LIST_SIZE) {
            reporter.runtimeProblem(Problems.LIST_SIZE, resultPos, listSize, 
                MAX_LIST_SIZE);
        }
        
        final List<Expression> content = new ArrayList<Expression>();
        
        while (start <= end) {
            content.add(new NumberLiteral(resultPos, values));
            values += s;
            start += Math.abs(s);
        }
        final ListLiteral ll = new ListLiteral(resultPos, content);
        ll.addType(ll.getUnique());
        ll.setUnique(Type.NUM.listOf());
        return ll;
    }

    
    
    public TernaryDotDot() {
        super(OpType.DOTDOT);
        this.initTypes(Type.NUM.listOf(), Type.NUM, Type.NUM, Type.NUM);
    }

    
    
    @Override
    protected void exec(Stack<Literal> stack, Namespace ns, NumberLiteral first,
            NumberLiteral second, NumberLiteral third, Position resultPos, 
            ExecutionVisitor execVisitor) throws ASTTraversalException {
        
        switch (this.getOp()) {
        case DOTDOT:
            stack.push(createSequence(first, second, third, resultPos, 
                execVisitor.getReporter()));
            break;
        default:
            this.invalidOperatorType(this.getOp());
        }
    }
}
