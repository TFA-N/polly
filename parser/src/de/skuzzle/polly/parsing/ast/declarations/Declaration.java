package de.skuzzle.polly.parsing.ast.declarations;

import de.skuzzle.polly.parsing.Position;
import de.skuzzle.polly.parsing.ast.Node;
import de.skuzzle.polly.parsing.ast.expressions.Identifier;
import de.skuzzle.polly.parsing.types.Type;


public abstract class Declaration extends Node implements Comparable<Declaration> {


    private static final long serialVersionUID = 1L;
    
    private final Identifier name;
    private boolean isGlobal;
    private boolean isTemp;
    private boolean mustCopy;
    
    
    public Declaration(Position position, Identifier name) {
        super(position);
        this.name = name;
    }
    
    
    
    public boolean mustCopy() {
        return this.mustCopy;
    }


    
    public void setMustCopy(boolean mustCopy) {
        this.mustCopy = mustCopy;
    }


    
    public abstract Type getType();
    
    
    
    public Identifier getName() {
        return this.name;
    }


    
    public boolean isGlobal() {
        return this.isGlobal;
    }


    
    public void setGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }


    
    public boolean isTemp() {
        return this.isTemp;
    }


    
    public void setTemp(boolean isTemp) {
        this.isTemp = isTemp;
    }
    
    
    
    @Override
    public int compareTo(Declaration o) {
        
        // order by length, then lexically
        
        final String thisId = this.getName().getId();
        final String otherId = o.getName().getId();
        final int lengthComp = Integer.compare(thisId.length(), otherId.length());
        
        return lengthComp != 0 ? lengthComp : 
            this.name.getId().compareTo(o.getName().getId());
    }
}