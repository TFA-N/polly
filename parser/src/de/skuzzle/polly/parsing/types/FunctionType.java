package de.skuzzle.polly.parsing.types;

import java.util.Collection;
import java.util.Iterator;

import de.skuzzle.polly.parsing.tree.literals.IdentifierLiteral;


public class FunctionType extends Type {

    private static final long serialVersionUID = 1L;
    
    private final Type returnType;
    private final Collection<Type> parameters;
    
    
    
    public FunctionType(Type returnType, Collection<Type> parameters) {
        super(new IdentifierLiteral("function"), false);
        this.returnType = returnType;
        this.parameters = parameters;
    }
    
    
    
    private boolean check(Type other, boolean includeReturnType) {
        if (!(other instanceof FunctionType)) {
            return false;
        }
        
        // check if return type and parameter types match
        final FunctionType ft = (FunctionType) other;
        if (includeReturnType && !this.returnType.check(ft.returnType)) {
            return false;
        }
        
        if (this.parameters.size() != ft.parameters.size()) {
            return false;
        }
        Iterator<Type> thisIt = this.parameters.iterator();
        Iterator<Type> otherIt = ft.parameters.iterator();
        
        while(thisIt.hasNext()) {
            if (!thisIt.next().check(otherIt.next())) {
                return false;
            }
        }
        return true;
    }
    
    
    
    @Override
    public boolean check(Type other) {
        return this.check(other, true);
    }
    
    
    
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("Function: return: ");
        b.append(this.returnType);
        b.append(" signature: ");
        for (final Type type : this.parameters) {
            b.append(type);
            b.append(" ");
        }
        return b.toString();
    }
}
