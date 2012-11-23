package de.skuzzle.polly.parsing.ast.visitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


import de.skuzzle.polly.parsing.LinkedStack;
import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.Stack;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.declarations.Parameter;
import de.skuzzle.polly.parsing.ast.declarations.VarDeclaration;
import de.skuzzle.polly.parsing.ast.expressions.NamespaceAccess;
import de.skuzzle.polly.parsing.ast.expressions.Assignment;
import de.skuzzle.polly.parsing.ast.expressions.Hardcoded;
import de.skuzzle.polly.parsing.ast.expressions.Call;
import de.skuzzle.polly.parsing.ast.expressions.Expression;
import de.skuzzle.polly.parsing.ast.expressions.OperatorCall;
import de.skuzzle.polly.parsing.ast.expressions.VarAccess;
import de.skuzzle.polly.parsing.ast.expressions.literals.FunctionLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.ListLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.Literal;
import de.skuzzle.polly.parsing.ast.expressions.literals.NumberLiteral;
import de.skuzzle.polly.parsing.ast.operators.BinaryArithmetic;
import de.skuzzle.polly.parsing.ast.operators.Operator;
import de.skuzzle.polly.parsing.ast.operators.Operator.OpType;



public class ExecutionVisitor extends DepthFirstVisitor {
    
    public static void main(String[] args) throws ASTTraversalException, IOException {
        final Operator add = new BinaryArithmetic(OpType.ADD);
        final Operator sub = new BinaryArithmetic(OpType.SUB);
        final Operator mul = new BinaryArithmetic(OpType.MUL);
        final Operator div = new BinaryArithmetic(OpType.DIV);
        Namespace.forName("me").declare(add.createDeclaration());
        Namespace.forName("me").declare(sub.createDeclaration());
        Namespace.forName("me").declare(mul.createDeclaration());
        Namespace.forName("me").declare(div.createDeclaration());
        
        Expression left = new NumberLiteral(Position.EMPTY, 5.0);
        Expression right = new NumberLiteral(Position.EMPTY, 2.0);
        OperatorCall binary1 = OperatorCall.binary(Position.EMPTY, OpType.ADD, left, right);
     
        left = binary1;
        right = new NumberLiteral(Position.EMPTY, 8.0);
        OperatorCall binary2 = OperatorCall.binary(Position.EMPTY, OpType.MUL, left, right);
        
        TypeResolver tr = new TypeResolver("me");
        binary2.visit(tr);
        
        ExecutionVisitor ev = new ExecutionVisitor("me");
        binary2.visit(ev);
        
        System.out.println("Result: " + ev.stack.peek());
    }
    
    

    private final Stack<Literal> stack;
    private Namespace nspace;
    
    
    public ExecutionVisitor(String executor) {
        this.stack = new LinkedStack<Literal>();
        this.nspace = Namespace.forName(executor);
    }
    
    
    
    /**
     * Determines whether we have a unique result (e.g. stack has only one element).
     * 
     * @return Whether we have a unique result.
     */
    public boolean hasResult() {
        return this.stack.size() == 1;
    }
    
    
    
    /**
     * Gets the result of the execution run of this visitor.
     * 
     * @return The executions result.
     */
    public Literal getResult() {
        return this.stack.peek();
    }
    
    
    
    /**
     * Creates a new sub namespace of the current namespace and sets that new namespace
     * as the current one.
     * 
     * @return The created namespace.
     */
    private Namespace enter() {
        return this.nspace = this.nspace.enter();
    }
    
    
    
    /**
     * Sets the current namespace as the parent of the current namespace.
     * 
     * @return The parent of the former current namespace.
     */
    private Namespace leave() {
        return this.nspace = this.nspace.getParent();
    }
    
    
    
    @Override
    public void visitLiteral(Literal literal) throws ASTTraversalException {
        this.beforeLiteral(literal);
        this.stack.push(literal);
        this.afterLiteral(literal);
    }
    
    
    
    @Override
    public void visitFunctionLiteral(FunctionLiteral func) throws ASTTraversalException {
        this.beforeFunctionLiteral(func);
        this.stack.push(func);
        this.afterFunctionLiteral(func);
    }
    
    
    
    @Override
    public void visitListLiteral(ListLiteral list) throws ASTTraversalException {
        // create collection of executed list content
        final Collection<Expression> executed = new ArrayList<Expression>();
        for (final Expression exp : list.getContent()) {
            // places executed expression on the stack
            exp.visit(this);
            executed.add(this.stack.pop());
        }
        final ListLiteral result = new ListLiteral(list.getPosition(), executed);
        this.stack.push(result);
    }
    
    
    
    @Override
    public void visitAccess(NamespaceAccess access) throws ASTTraversalException {
        this.beforeAccess(access);
        
        // store current ns and switch to new one
        final Namespace backup = this.nspace;
        
        // get namespace which is accessed here and has the current namespace as 
        // parent. 
        // lhs of access is guaranteed to be a VarAccess
        final VarAccess va = (VarAccess) access.getLhs();
        this.nspace = Namespace.forName(va.getIdentifier()).derive(this.nspace);
        
        // execute expression and restore old namespace
        access.getRhs().visit(this);
        this.nspace = backup;
        
        this.afterAccess(access);
    }
    
    
    
    @Override
    public void visitHardCoded(Hardcoded hc) throws ASTTraversalException {
        this.beforeHardCoded(hc);
        hc.execute(this.stack, this.nspace, this);
        this.afterHardCoded(hc);
    }
    
    
    
    @Override
    public void visitAssignment(Assignment assign) 
            throws ASTTraversalException {
        this.beforeAssignment(assign);
        
        // result of assignment is the result of the assigned expression
        assign.getExpression().visit(this);
        
        this.afterAssignment(assign);
    }
    
    
    
    @Override
    public void visitOperatorCall(OperatorCall call) throws ASTTraversalException {
        this.beforeOperatorCall(call);
        this.visitCall(call);
        this.afterOperatorCall(call);
    }
    
    
    
    @Override
    public void visitCall(Call call) throws ASTTraversalException {
        this.beforeCall(call);
        
        // this will push the function call onto the stack
        call.getLhs().visit(this);
        
        final FunctionLiteral func = (FunctionLiteral) this.stack.pop();
        
        this.enter();
        final Iterator<Expression> actualIt = call.getParameters().iterator();
        final Iterator<Parameter> formalIt = func.getFormal().iterator();
        while (formalIt.hasNext()) {
            final Parameter formal = formalIt.next();
            final Expression actual = actualIt.next();
            
            // execute actual parameter
            actual.visit(this);
            
            // declare result as local variable for this call
            final Expression result = this.stack.pop();
            final VarDeclaration local = 
                new VarDeclaration(actual.getPosition(), formal.getName(), result);
            this.nspace.declare(local);
        }

        func.getExpression().visit(this);
        this.leave();
        
        this.afterCall(call);
    }
    
    
    
    @Override
    public void visitVarAccess(VarAccess access) throws ASTTraversalException {
        this.beforeVarAccess(access);
        
        final VarDeclaration vd = this.nspace.resolveVar(access.getIdentifier(), 
            access.getTypeToResolve());
                /*(VarDeclaration) access.getIdentifier().getDeclaration();*/
        vd.getExpression().visit(this);
        
        this.afterVarAccess(access);
    }
}