package de.skuzzle.polly.parsing.ast.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.ast.Node;
import de.skuzzle.polly.parsing.ast.ResolvableIdentifier;
import de.skuzzle.polly.parsing.ast.declarations.Namespace;
import de.skuzzle.polly.parsing.ast.declarations.TypeDeclaration;
import de.skuzzle.polly.parsing.ast.declarations.VarDeclaration;
import de.skuzzle.polly.parsing.ast.expressions.Assignment;
import de.skuzzle.polly.parsing.ast.expressions.Call;
import de.skuzzle.polly.parsing.ast.expressions.Empty;
import de.skuzzle.polly.parsing.ast.expressions.Expression;
import de.skuzzle.polly.parsing.ast.expressions.Native;
import de.skuzzle.polly.parsing.ast.expressions.NamespaceAccess;
import de.skuzzle.polly.parsing.ast.expressions.OperatorCall;
import de.skuzzle.polly.parsing.ast.expressions.VarAccess;
import de.skuzzle.polly.parsing.ast.expressions.literals.FunctionLiteral;
import de.skuzzle.polly.parsing.ast.expressions.literals.ListLiteral;
import de.skuzzle.polly.parsing.ast.expressions.parameters.FunctionParameter;
import de.skuzzle.polly.parsing.ast.expressions.parameters.ListParameter;
import de.skuzzle.polly.parsing.ast.expressions.parameters.Parameter;
import de.skuzzle.polly.parsing.types.FunctionType;
import de.skuzzle.polly.parsing.types.ListType;
import de.skuzzle.polly.parsing.types.Type;
import de.skuzzle.polly.parsing.util.LinkedStack;
import de.skuzzle.polly.parsing.util.Stack;



public class TypeResolver extends DepthFirstVisitor {
    
    
    
    /**
     * Class used to encapsulate information about recent function calls. It holds the
     * called functions's signature and a collection of the actual parameters.
     * 
     * @author Simon Taddiken
     */
    public final static class CallContext {
        
        public final FunctionType signature;
        public final List<Expression> actualParameters;
        
        public CallContext(FunctionType signature, List<Expression> actualParameters) {
            super();
            this.signature = signature;
            this.actualParameters = actualParameters;
        }
    }
    
    
    
    private Namespace nspace;
    private final Namespace rootNs;
    private final Set<Node> checked;
    private Stack<CallContext> signatureStack;
    
    
    
    public TypeResolver(Namespace namespace) {
        // create temporary namespace for executing user
        this.rootNs = namespace.enter(false);
        this.nspace = this.rootNs;
        this.checked = new HashSet<Node>();
        this.signatureStack = new LinkedStack<CallContext>();
    }
    
    
    
    /**
     * Creates a new sub namespace of the current namespace and sets that new namespace
     * as the current one.
     * 
     * @return The created namespace.
     */
    private Namespace enter() {
        return this.nspace = this.nspace.enter(true);
    }
    
    
    
    /**
     * Sets the current namespace as the parent of the current namespace.
     * 
     * @return The parent of the former current namespace.
     */
    private Namespace leave() {
        return this.nspace = this.nspace.getParent();
    }
    
    
    
    /**
     * Tests whether the given node has already been checked. If it was not already 
     * checked, it will be marked checked by the time you call this method.
     * 
     * @param node The node to check.
     * @return Whether the nodes type has already been resolved.
     */
    private final boolean testIsChecked(Node node) {
        // TODO: remove, or what?
        this.checked.add(node);
        return false;
    }
    
    
    
    @Override
    public void visitNative(Native hc) throws ASTTraversalException {
        this.beforeNative(hc);
        hc.resolveType(this.nspace, this);
        this.afterNative(hc);
    }
    
    
    
    @Override
    public void visitParameter(Parameter param) throws ASTTraversalException {
        this.beforeParameter(param);
        
        final TypeDeclaration decl = Namespace.resolveType(param.getTypeName());
        param.setType(decl.getType());
        
        this.afterParameter(param);
    }
    
    
    
    @Override
    public void visitListParameter(ListParameter param) throws ASTTraversalException {
        this.beforeListParameter(param);
        
        final TypeDeclaration mainTypeDecl = Namespace.resolveType(
            param.getMainTypeName());
        
        if (mainTypeDecl.getType() != Type.LIST) {
            throw new ASTTraversalException(param.getMainTypeName().getPosition(), 
                "Nur Listen k�nnen Typ-Parameter haben.");
        }
        final TypeDeclaration subTypeDecl = Namespace.resolveType(param.getTypeName());
        param.setType(new ListType(subTypeDecl.getType()));
        
        this.afterListParameter(param);
    }
    
    
    
    @Override
    public void visitFunctionParameter(FunctionParameter param)
            throws ASTTraversalException {
        this.beforeFunctionParameter(param);
        
        final Iterator<ResolvableIdentifier> it = param.getSignature().iterator();
        
        // first element is the return type
        final TypeDeclaration returnTypeDecl = Namespace.resolveType(it.next());
        final Collection<Type> types = new ArrayList<Type>(param.getSignature().size());
        while (it.hasNext()) {
            final TypeDeclaration decl = Namespace.resolveType(it.next());
            types.add(decl.getType());
        }
        
        param.setType(new FunctionType(returnTypeDecl.getType(), types));
        
        this.afterFunctionParameter(param);
    }
    
    
    
    @Override
    public void visitFunctionLiteral(FunctionLiteral func)
            throws ASTTraversalException {
        
        if (this.testIsChecked(func)) {
            return;
        }
        
        this.beforeFunctionLiteral(func);

        // add formal parameters as empty expressions into new local namespace, then
        // context check the functions expression to resolve the return type
        
        // If this is the lhs of a function call, the called signature will be on top
        // of the signature stack. So we are able to use the actual called signature
        final CallContext c = this.signatureStack.isEmpty() 
                ? null 
                : this.signatureStack.pop();
        
        this.enter();
        final Iterator<Parameter> formalIt = func.getFormal().iterator();
        final Iterator<Type> typeIt = c == null 
                ? null 
                : c.signature.getParameters().iterator();
        final Iterator<Expression> actualIt = c == null ? null :
            c.actualParameters.iterator();
                
        while (formalIt.hasNext()) {
            final Parameter p = formalIt.next();
            // resolve parameter type
            p.visit(this);
            
            // use both the type of the actual signature, and the type of the formal 
            // signature, depending on what information we have. The actual signature
            // might have more concrete type information, but may as well be totally
            // wrong. If it was wrong, we can fallback to the actual signature
            
            // HACK or not? declaring both, the formal and actual signature here. The 
            //     second is a fallback if the actual signature did not match
            if (typeIt != null) {
                final Position pos = actualIt == null 
                    ? p.getPosition() : actualIt.next().getPosition();
                
                final VarDeclaration vd = new VarDeclaration(
                    pos, p.getName(), 
                    new Empty(typeIt.next(), pos, this.signatureStack));
                
                this.nspace.declare(vd);
            }
            
            
            final VarDeclaration vd = new VarDeclaration(p.getPosition(), p.getName(), 
                new Empty(p.getType(), p.getPosition(), this.signatureStack));
            
            this.nspace.declare(vd);
        }
        // now determine type of the function's expression
        func.getExpression().visit(this);
        this.leave();
        
        final FunctionType resultType = c == null 
            ? new FunctionType(func.getExpression().getType(), 
                    Parameter.asType(func.getFormal())) 
            : new FunctionType(func.getExpression().getType(), 
                    c.signature.getParameters());
            
        
        func.setType(resultType);
        func.setReturnType(resultType.getReturnType());
        this.afterFunctionLiteral(func);
    }
    
    
    
    @Override
    public void visitListLiteral(ListLiteral list) throws ASTTraversalException {
        if (this.testIsChecked(list)) {
            return;
        }
        
        this.beforeListLiteral(list);
        
        if (list.getContent().isEmpty()) {
            list.setType(Type.EMPTY_LIST);
        } else {
            // resolve expression types
            Type last = null; 
            for (final Expression exp : list.getContent()) {
                exp.visit(this);
                if (last != null && !last.check(exp.getType())) {
                    throw new ASTTraversalException(exp.getPosition(), 
                        "Listen d�rfen nur Elemente des selben Typs beinhalten");
                }
                last = exp.getType();
            }
            list.setType(new ListType(last));
        }
        
        this.afterListLiteral(list);
    }
    
    
    
    @Override
    public void visitAssignment(final Assignment assign) throws ASTTraversalException {
        if (this.testIsChecked(assign)) {
            return;
        }
        
        this.beforeAssignment(assign);
        // resolve assignments type
        assign.getExpression().visit(this);
        assign.setType(assign.getExpression().getType());
        
        /*final Empty exp = new Empty(
                assign.getExpression().getType(), assign.getExpression().getPosition(), 
                this.signatureStack); */
        
        final VarDeclaration vd = new VarDeclaration(assign.getPosition(), 
            assign.getName(), assign.getExpression());
        
        // exchange assignment with its sole expression
        // this needs to be done in case that further assignments are following. They 
        // would then contain this assignment too. 
        //assign.getParent().replaceChild(assign, assign.getExpression());
        
        // Transitive deep recursion check: check whether assigned expression contains
        //                                  transitive relations to itself.
        if (vd.getType() instanceof FunctionType) {
            final Visitor recursiveCallChecker = new DepthFirstVisitor() {
                
                @Override
                public void beforeVarAccess(VarAccess access) 
                        throws ASTTraversalException {
                    
                    final VarDeclaration test = (VarDeclaration) nspace.tryResolve(
                            access.getIdentifier(), vd.getType());
                    
                    if (test != null) {
                        if (vd.equals(test)) {
                            throw new ASTTraversalException(vd.getPosition(), 
                                "Rekursive Deklaration von '" + access.getIdentifier() + 
                                "'");
                        }
                        test.getExpression().visit(this);
                    }
                }
            };
            assign.getExpression().visit(recursiveCallChecker);
        }
        
        // declarations are always stored in the root namespace!
        this.rootNs.declare(vd);
        this.afterAssignment(assign);
    }
    
    
    
    @Override
    public void visitOperatorCall(OperatorCall call) throws ASTTraversalException {
        this.beforeOperatorCall(call);
        // treat as normal call
        this.visitCall(call);
        this.afterOperatorCall(call);
    }
    
    
    
    @Override
    public void visitCall(Call call) throws ASTTraversalException {
        if (this.testIsChecked(call)) {
            return;
        }
        
        this.beforeCall(call);
        
        // resolve actual parameter types
        for (final Expression exp : call.getParameters()) {
            exp.visit(this);
        }
        
        // create signature from actual parameter types.
        // signature does *not* obey return value as it is unknown by now.
        final FunctionType signature = call.createSignature();
        final CallContext c = new CallContext(signature, call.getParameters());
        
        // push actual signature.
        this.signatureStack.push(c);
        
        // check what type of call this is. Might be a lambda call or a VarAccess which
        // in turn references a function. For that purpose, we need to find the next 
        // VarAccess and tell it the actual signature.
        call.getLhs().visit(this);
        
        if (!call.getLhs().getType().check(signature)) {
            Type.typeError(signature, call.getLhs().getType(), 
                call.getParameterPosition());
        }
        
        // get lhs' type as FunctionType. This type already has a return type set,
        // which will be the return type of this call
        final FunctionType lhsType = (FunctionType) call.getLhs().getType();
        call.setType(lhsType.getReturnType());
        
        this.afterCall(call);
    }
    
    
    
    @Override
    public void visitVarAccess(VarAccess access) throws ASTTraversalException {
        if (access.getIdentifier().getDeclaration() != null) {
            // do not double check (accessed expression is already resolved
            return;
        }
        
        this.beforeVarAccess(access);
        
        // if this is the lhs of a call, the called signature will be on top of the
        // signature stack. Otherwise, we will look for any declaration, disregarding
        // its type
        if (!this.signatureStack.isEmpty()) {
            final CallContext cc = this.signatureStack.peek();

            VarDeclaration vd = this.nspace.resolveBySignature(access.getIdentifier(), 
                cc.actualParameters);
            
            vd.getExpression().visit(this);
            access.setType(vd.getExpression().getType());
            return;
        }

        
        final List<VarDeclaration> decls = this.nspace.resolveAll(access.getIdentifier());
        
        for (final VarDeclaration decl : decls) {
            access.addPossibleType(decl.getType());
        }
        
        /*
        final Type typeToResolve = this.signatureStack.isEmpty() 
            ? Type.ANY 
            : this.signatureStack.peek().signature;
            
        access.setTypeToResolve(typeToResolve);
        final VarDeclaration vd = this.nspace.resolveVar(
                access.getIdentifier(), typeToResolve);

        vd.getExpression().visit(this);
        access.setType(vd.getExpression().getType());*/
        
        
        this.afterVarAccess(access);
    }
    
    
    
    @Override
    public void visitAccess(NamespaceAccess access) throws ASTTraversalException {
        if (this.testIsChecked(access)) {
            return;
        }
        this.beforeAccess(access);
        // remember current nspace
        final Namespace backup = this.nspace;
        
        // get namespace which is accessed here and has the current namespace as 
        // parent. 
        final Expression lhs = access.getLhs();
        if (!(lhs instanceof VarAccess)) {
            throw new ASTTraversalException(access.getPosition(), 
                "Operanden m�ssen Bezeichner sein.");
        }
        final VarAccess va = (VarAccess) lhs;
        this.nspace = Namespace.forName(va.getIdentifier()).derive(this.nspace);
        access.getRhs().visit(this);
        this.nspace = backup;
        
        access.setType(access.getRhs().getType());
        
        this.afterAccess(access);
    }
}