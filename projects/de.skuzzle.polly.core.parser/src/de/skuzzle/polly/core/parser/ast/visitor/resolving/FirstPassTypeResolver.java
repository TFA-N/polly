package de.skuzzle.polly.core.parser.ast.visitor.resolving;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import de.skuzzle.polly.core.parser.ast.Identifier;
import de.skuzzle.polly.core.parser.ast.ResolvableIdentifier;
import de.skuzzle.polly.core.parser.ast.declarations.Declaration;
import de.skuzzle.polly.core.parser.ast.declarations.Namespace;
import de.skuzzle.polly.core.parser.ast.declarations.types.MapType;
import de.skuzzle.polly.core.parser.ast.declarations.types.MissingType;
import de.skuzzle.polly.core.parser.ast.declarations.types.ProductType;
import de.skuzzle.polly.core.parser.ast.declarations.types.Substitution;
import de.skuzzle.polly.core.parser.ast.declarations.types.Type;
import de.skuzzle.polly.core.parser.ast.expressions.Assignment;
import de.skuzzle.polly.core.parser.ast.expressions.Call;
import de.skuzzle.polly.core.parser.ast.expressions.Delete;
import de.skuzzle.polly.core.parser.ast.expressions.Empty;
import de.skuzzle.polly.core.parser.ast.expressions.Expression;
import de.skuzzle.polly.core.parser.ast.expressions.Inspect;
import de.skuzzle.polly.core.parser.ast.expressions.NamespaceAccess;
import de.skuzzle.polly.core.parser.ast.expressions.Native;
import de.skuzzle.polly.core.parser.ast.expressions.OperatorCall;
import de.skuzzle.polly.core.parser.ast.expressions.VarAccess;
import de.skuzzle.polly.core.parser.ast.expressions.literals.FunctionLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.ListLiteral;
import de.skuzzle.polly.core.parser.ast.expressions.literals.ProductLiteral;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.core.parser.ast.visitor.DepthFirstVisitor;
import de.skuzzle.polly.core.parser.ast.visitor.Unparser;
import de.skuzzle.polly.core.parser.problems.ProblemReporter;
import de.skuzzle.polly.core.parser.problems.Problems;
import de.skuzzle.polly.core.parser.util.Combinator;
import de.skuzzle.polly.core.parser.util.Combinator.CombinationCallBack;


/**
 * This visitor resolves <b>all</b> possible types for an expression and stores them in
 * each expression's <i>types</i> attribute. A Second pass type resolval is needed to 
 * determine each expression's unique type.
 * 
 * @author Simon Taddiken
 * @see SecondPassTypeResolver
 */
class FirstPassTypeResolver extends AbstractTypeResolver {
    
    
    public FirstPassTypeResolver(Namespace namespace, ProblemReporter reporter) {
        super(namespace, reporter);
    }
    
    
    
    @Override
    public int before(Native node) throws ASTTraversalException {
        node.resolveType(this.nspace, this);
        return CONTINUE;
    }
    
    
    
    @Override
    public int before(Declaration node) throws ASTTraversalException {
        node.getExpression().visit(this);
        return CONTINUE;
    }
    
    
    
    @Override
    public boolean visit(final FunctionLiteral node) throws ASTTraversalException {
        switch (this.before(node)) {
        case SKIP: return true;
        case ABORT: return false;
        }
        
        final List<Type> source = new ArrayList<Type>();
        
        // resolve parameter types
        this.enter();
        final Set<String> names = new HashSet<String>();
        for (final Declaration d : node.getFormal()) {
            if (!names.add(d.getName().getId())) {
                this.reportError(d.getName(), Problems.DUPLICATED_DECL, d.getName());
            }
            if (!d.visit(this)) {
                return false;
            }
            source.add(d.getType());
            
            this.nspace.declare(d);
        }
        
        if (!node.getBody().visit(this)) {
            return false;
        }
        this.leave();
        
        for (final Type te : node.getBody().getTypes()) {
            if (node.getBody().hasConstraint(te)) {
                final Substitution constraint = node.getBody().getConstraint(te);
                
                final Type t = new ProductType(source).mapTo(te).subst(constraint.toSource());
                node.addType(t);
            } else {
                node.addType(new ProductType(source).mapTo(te));
            }
        }
        
        return this.after(node) == CONTINUE;
    }
    
    
    
    @Override
    public int after(ListLiteral node) throws ASTTraversalException {
        if (node.getContent().isEmpty()) {
            this.reportError(node, Problems.EMPTY_LIST);
        }
        for (final Expression exp : node.getContent()) {
            for (final Type t : exp.getTypes()) {
                node.addType(t.listOf(), exp.getConstraint(t));
            }
        }
        return CONTINUE;
    }
    
    
    
    @Override
    public int after(final ProductLiteral node) throws ASTTraversalException {
        
        // Use combinator to create all combinations of possible types
        final CombinationCallBack<Expression, Type> ccb = 
            new CombinationCallBack<Expression, Type>() {

            @Override
            public List<Type> getSubList(Expression outer) {
                return outer.getTypes();
            }

            
            @Override
            public void onNewCombination(List<Type> combination) {
                final ProductType prod = new ProductType(combination);
                
                final Iterator<Expression> contIt = node.getContent().iterator();
                final Iterator<Type> typeIt = prod.getTypes().iterator();
                
                // join constraints of product
                Substitution s = new Substitution();
                while (typeIt.hasNext()) {
                    final Expression nextExp = contIt.next();
                    final Type nextType = typeIt.next();
                    final Substitution constraint = nextExp.getConstraint(nextType);
                    s = s.join(constraint);
                }
                
                node.addType(prod, s);
            }
        };
        
        Combinator.combine(node.getContent(), ccb);
        return CONTINUE;
    }
    
    
    
    @Override
    public int after(final Assignment node) throws ASTTraversalException {
        if (node.isTemp()) {
            this.reportError(node, 
                "Tempor�re Deklarationen werden (noch?) nicht unterst�tzt.");
        }
        
        // deep transitive recursion check
        node.getExpression().visit(new DepthFirstVisitor() {
            @Override
            public int before(VarAccess access) throws ASTTraversalException {
                final Collection<Declaration> decls = 
                    nspace.lookupAll(access.getIdentifier());
                
                for (final Declaration decl : decls) {
                    if (decl.getName().equals(node.getName())) {
                        reportError(access, Problems.RECURSIVE_CALL);
                        return ABORT;
                    }
                    if (!decl.isNative()) {
                        if (!decl.getExpression().visit(this)) {
                            return ABORT;
                        }
                    }
                }
                return CONTINUE;
            } 
        });
        
        for (final Type t : node.getExpression().getTypes()) {
            final Declaration vd = new Declaration(node.getName().getPosition(), 
                node.getName(), new Empty(t, node.getExpression().getPosition()));
            this.nspace.declare(vd);
            
            node.addType(t, node.getExpression().getConstraint(t));
        }
        return CONTINUE;
    }
    
    
    
    @Override
    public boolean visit(OperatorCall node) throws ASTTraversalException {
        return this.visit((Call) node);
    }
    

    
    @Override
    public boolean visit(Call node) throws ASTTraversalException {
        switch (this.before(node)) {
        case SKIP: return true;
        case ABORT: return false;
        }
        
        // resolve parameter types
        if (!node.getRhs().visit(this)) {
            return false;
        }
        
        final List<Type> possibleTypes = new ArrayList<Type>(
            node.getRhs().getTypes().size());
        for (final Type rhsType : node.getRhs().getTypes()) {
            possibleTypes.add(rhsType.mapTo(Type.newTypeVar()));
        }
        
        // resolve called function's types
        if (!node.getLhs().visit(this)) {
            return false;
        }
        
        boolean hasMapType = false;
        for (final Type type : node.getLhs().getTypes()) {
            hasMapType |= type instanceof MapType;
        }
        if (!hasMapType) {
            this.reportError(node.getLhs(), Problems.NO_FUNCTION,
                Unparser.toString(node.getLhs()));
        } else if (node.getLhs().getTypes().isEmpty()) {
            this.reportError(node.getLhs(), Problems.UNKNOWN_FUNCTION, 
                Unparser.toString(node.getLhs()));
        } else {
            // sort out all lhs types that do not match the rhs types
            for (final Type possibleLhs : possibleTypes) {
                
                for (final Type lhs : node.getLhs().getTypes()) {
                    final Substitution subst = Type.unify(possibleLhs, lhs);
                    if (subst != null) {
                        // construct new type with the argument types of lhs, and 
                        // result type of rhs
                        final MapType lhsMap = (MapType) lhs;
                        final MapType plhsMap = (MapType) possibleLhs;
                    
                        final Substitution constraint = node.getRhs().getConstraint(
                            plhsMap.getSource());
                        
                        final MapType mtc = (MapType) plhsMap.getSource().mapTo(
                            lhsMap.getTarget()).subst(subst);
                        node.addType(mtc.getTarget(), subst.join(constraint));
                    }  
                }
            }
        }
            
            
        if (node.getTypes().isEmpty()) {
            final String problem = node instanceof OperatorCall 
                ? Problems.INCOMPATIBLE_OP 
                : Problems.INCOMPATIBLE_CALL;
            this.reportError(node.getLhs(), problem, 
                Unparser.toString(node.getLhs()));
            node.addType(new MissingType(new Identifier("$compatibilty")), null);
        }
        return this.after(node) == CONTINUE;
    }
    
    
    
    @Override
    public int before(VarAccess node) throws ASTTraversalException {
        final Set<Type> types = this.nspace.lookupFresh(node);
        node.addTypes(types);
        return CONTINUE;
    }
    
    
    
    @Override
    public boolean visit(NamespaceAccess node) throws ASTTraversalException {
        switch (this.before(node)) {
        case SKIP: return true;
        case ABORT: return false;
        }        
        if (!(node.getLhs() instanceof VarAccess)) {
            this.reportError(node.getLhs(), Problems.ILLEGAL_NS_ACCESS);
        } else if (!(node.getRhs() instanceof VarAccess)) {
            this.reportError(node.getRhs(), Problems.ILLEGAL_NS_ACCESS);
        }
        
        final VarAccess va = (VarAccess) node.getLhs();
        if (!Namespace.exists(va.getIdentifier())) {
            this.reportError(node.getLhs(), Problems.UNKNOWN_NS, va.getIdentifier());
        }
        final Namespace last = this.nspace;
        this.nspace = Namespace.forName(va.getIdentifier()).derive(this.nspace);
        if (!node.getRhs().visit(this)) {
            return false;
        }
        this.nspace = last;

        node.addTypes(node.getRhs().getTypes());
        
        return this.after(node) == CONTINUE;
    }
    
    
    
    @Override
    public int before(Delete node) throws ASTTraversalException {
        node.addType(Type.NUM, null);
        node.setUnique(Type.NUM);
        return CONTINUE;
    }
    
    
    
    @Override
    public int before(Inspect node) throws ASTTraversalException {
        Namespace target = null;
        ResolvableIdentifier var = null;
        
        if (node.getAccess() instanceof VarAccess) {
            final VarAccess va = (VarAccess) node.getAccess();
            
            target = this.nspace;
            var = va.getIdentifier();
            
        } else if (node.getAccess() instanceof NamespaceAccess) {
            final NamespaceAccess nsa = (NamespaceAccess) node.getAccess();
            final VarAccess nsName = (VarAccess) nsa.getLhs();
            
            if (!Namespace.exists(nsName.getIdentifier())) {
                this.reportError(nsName, Problems.UNKNOWN_NS, nsName.getIdentifier());
            }
            
            var = ((VarAccess) nsa.getRhs()).getIdentifier();
            target = Namespace.forName(nsName.getIdentifier());
        } else {
            throw new IllegalStateException("this should not be reachable");
        }
        
        final Collection<Declaration> decls = target.lookupAll(var);
        if (decls.isEmpty()) {
            this.reportError(var, Problems.UNKNOWN_VAR, var.getId());
        }
        node.setUnique(Type.STRING);
        node.addType(Type.STRING, null);
        return CONTINUE;
    }
}
