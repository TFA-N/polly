package de.skuzzle.polly.parsing.ast.visitor;

import java.io.PrintStream;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.ast.Identifier;
import de.skuzzle.polly.parsing.ast.Node;
import de.skuzzle.polly.parsing.ast.Root;
import de.skuzzle.polly.parsing.ast.declarations.Declaration;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.expressions.Assignment;
import de.skuzzle.polly.parsing.ast.expressions.Braced;
import de.skuzzle.polly.parsing.ast.expressions.Call;
import de.skuzzle.polly.parsing.ast.expressions.Delete;
import de.skuzzle.polly.parsing.ast.expressions.Expression;
import de.skuzzle.polly.parsing.ast.expressions.Inspect;
import de.skuzzle.polly.parsing.ast.expressions.NamespaceAccess;
import de.skuzzle.polly.parsing.ast.expressions.OperatorCall;
import de.skuzzle.polly.parsing.ast.expressions.VarAccess;
import de.skuzzle.polly.parsing.ast.expressions.literals.ChannelLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.DateLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.FunctionLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.HelpLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.ListLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.Literal;
import de.skuzzle.polly.parsing.ast.expressions.literals.LiteralFormatter;
import de.skuzzle.polly.parsing.ast.expressions.literals.NumberLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.ProductLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.StringLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.TimespanLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.UserLiteral;
import de.skuzzle.polly.parsing.util.ASTDotBuilder;


public class ExpASTVisualizer extends DepthFirstVisitor {
    
    
    private ASTDotBuilder dotBuilder;
    private final LiteralFormatter formatter;
    
    
    
    public ExpASTVisualizer() {
        this.formatter = new LiteralFormatter() {
            
            @Override
            public String formatUser(UserLiteral user) {
                return DEFAULT.formatUser(user);
            }
            
            
            
            @Override
            public String formatTimespan(TimespanLiteral timespan) {
                return DEFAULT.formatTimespan(timespan);
            }
            
            
            
            @Override
            public String formatString(StringLiteral string) {
                return string.getValue();
            }
            
            
            
            @Override
            public String formatNumberLiteral(NumberLiteral number) {
                return DEFAULT.formatNumberLiteral(number);
            }
            
            
            
            @Override
            public String formatList(ListLiteral listLiteral) {
                return DEFAULT.formatList(listLiteral);
            }
            
            
            
            @Override
            public String formatFunction(FunctionLiteral functionLiteral) {
                return "\\" + DEFAULT.formatFunction(functionLiteral);
            }
            
            
            
            @Override
            public String formatDate(DateLiteral date) {
                return DEFAULT.formatDate(date);
            }
            
            
            
            @Override
            public String formatChannel(ChannelLiteral channel) {
                return DEFAULT.formatChannel(channel);
            }



            @Override
            public String formatHelp(HelpLiteral helpLiteral) {
                return DEFAULT.formatHelp(helpLiteral);
            }
        };
    }
    
    
    
    public void visualize(Node root, PrintStream out, Namespace ns) 
            throws ASTTraversalException {
        this.dotBuilder = new ASTDotBuilder(out);
        //this.dotBuilder.printNameSpace(ns);
        root.visit(this);
        this.dotBuilder.finish();
    }
    
    
    
    @Override
    public void visit(Root root) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(root);
        this.dotBuilder.printNode(root, "Root", root.getPosition().toString());
        this.dotBuilder.printNode(root.getCommand(), "Name: " + root.getCommand(), 
                root.getCommand().getPosition().toString());
        this.dotBuilder.printEdge(root, root.getCommand(), "command");
        for (final Expression exp : root.getExpressions()) {
            exp.visit(this);
            this.dotBuilder.printEdge(root, exp, Unparser.toString(exp, this.formatter));
        }
        this.after(root);
    }
    
    
    
    @Override
    public void visit(Call call) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        
        this.before(call);
        this.dotBuilder.printExpression("Call", call);
        call.getLhs().visit(this);
        call.getRhs().visit(this);
        
        this.dotBuilder.printEdge(call, call.getLhs(), Unparser.toString(call.getLhs(), this.formatter));
        this.dotBuilder.printEdge(call, call.getRhs(), Unparser.toString(call.getRhs(), this.formatter));
        this.after(call);
    }
    
    
    
    @Override
    public void visit(OperatorCall call) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        
        this.before(call);
        this.dotBuilder.printExpression("OpCall: " + call.getOperator().getId(), call);
        call.getLhs().visit(this);
        call.getRhs().visit(this);
        
        this.dotBuilder.printEdge(call, call.getLhs(), Unparser.toString(call.getLhs(), this.formatter));
        this.dotBuilder.printEdge(call, call.getRhs(), Unparser.toString(call.getRhs(), this.formatter));
        this.after(call);
    }
    
    
    
    @Override
    public void visit(ListLiteral list) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(list);
        this.dotBuilder.printExpression("List", list);
        
        int i = 0;
        for (final Expression exp : list.getContent()) {
            exp.visit(this);
            this.dotBuilder.printEdge(list, exp, "" + (i++));
        }
        this.after(list);
    }
    
    
    
    @Override
    public void visit(Literal literal) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(literal);
        this.dotBuilder.printExpression(literal.toString(), literal);
        this.after(literal);
    }
    
    
    
    @Override
    public void visit(ProductLiteral product) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(product);
        this.dotBuilder.printExpression("Product", product);
        
        int i = 0;
        for (final Expression exp : product.getContent()) {
            exp.visit(this);
            this.dotBuilder.printEdge(product, exp, "" + (i++));
        }
        this.after(product);
    }
    
    
    
    @Override
    public void visit(FunctionLiteral func) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(func);
        
        this.dotBuilder.printExpression("Function", func);
        for (final Declaration param : func.getFormal()) {
            param.visit(this);
            this.dotBuilder.printEdge(func, param, "param");
        }
        func.getBody().visit(this);
        this.dotBuilder.printEdge(func, func.getBody(), "body");
        
        for (final Declaration param : func.getFormal()) {
            for (final VarAccess va : param.getUsage()) {
                this.dotBuilder.printUsage(va, param);
            }
        }
        
        this.after(func);
    }

    
    
    @Override
    public void visit(Assignment assign) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(assign);
        this.dotBuilder.printExpression("Assignment", assign);
        assign.getExpression().visit(this);
        this.dotBuilder.printNode(assign.getName(), "Name: " + assign.getName(), 
            assign.getName().getPosition().toString());
        this.dotBuilder.printEdge(assign, assign.getExpression(), "expresion");
        this.dotBuilder.printEdge(assign, assign.getName(), "to");
        this.after(assign);
    }
    
    
    
    @Override
    public void visit(NamespaceAccess access) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(access);
        
        this.dotBuilder.printExpression("Namespace Access", access);
        access.getLhs().visit(this);
        access.getRhs().visit(this);
        this.dotBuilder.printEdge(access, access.getLhs(), "LHS");
        this.dotBuilder.printEdge(access, access.getRhs(), "RHS");
        
        this.after(access);
    }
    
    
    
    @Override
    public void before(VarAccess access) throws ASTTraversalException {
        this.dotBuilder.printExpression("VarAccess: " + access.getIdentifier(), access);
    }
    
    
    
    @Override
    public void before(Declaration decl) throws ASTTraversalException {
        final StringBuilder b = new StringBuilder();
        b.append("Usage (by name):\\n");
        for (final VarAccess va : decl.getUsage()) {
            b.append(va.getPosition());
            b.append("\\n");
        }
        String pos = decl.getPosition() == Position.NONE 
            ? "" : " (" + decl.getPosition().toString() + ")";
        
        this.dotBuilder.printNode(decl, "Declaration" + pos ,  
            "Name: " + decl.getName(), 
            "Type: " + decl.getType(),
            b.toString());
    }
    
    
    
    @Override
    public void visit(Braced braced) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(braced);
        this.dotBuilder.printExpression("Embraced", braced);
        braced.getExpression().visit(this);
        this.dotBuilder.printEdge(braced, braced.getExpression(), "");
        
        this.after(braced);
    }
    
    
    
    @Override
    public void visit(Delete delete) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(delete);
        this.dotBuilder.printExpression("Delete", delete);
        for (final Identifier id : delete.getIdentifiers()) {
            this.dotBuilder.printNode(id, id.getId());
            this.dotBuilder.printEdge(delete, id, "");
        }
        this.after(delete);
    }
    
    
    
    @Override
    public void visit(Inspect inspect) throws ASTTraversalException {
        if (this.aborted) {
            return;
        }
        this.before(inspect);
        this.dotBuilder.printExpression("Inspect", inspect);
        inspect.getAccess().visit(this);
        this.dotBuilder.printEdge(inspect, inspect.getAccess(), "");
        this.after(inspect);
    }
}
