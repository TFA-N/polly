package de.skuzzle.polly.core.parser.ast.expressions.literals;

import de.skuzzle.polly.core.parser.Position;
import de.skuzzle.polly.core.parser.ast.declarations.types.Type;
import de.skuzzle.polly.core.parser.ast.expressions.Expression;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.core.parser.ast.visitor.Transformation;
import de.skuzzle.polly.tools.Equatable;


public class StringLiteral extends Literal {
    
    private final String value;
    
    
    public StringLiteral(Position position, String value) {
        super(position, Type.STRING);
        this.value = value;
    }
    
    
    
    protected StringLiteral(Position position, String value, Type type) {
        super(position, type);
        this.value = value;
    }
    
    
    
    public String getValue() {
        return this.value;
    }

    

    @Override
    public Literal castTo(Type type) throws ASTTraversalException {
        if (type == Type.CHANNEL) {
            return new ChannelLiteral(this.getPosition(), this.value);
        } else if (type == Type.USER) {
            return new UserLiteral(this.getPosition(), this.value);
        }
        return super.castTo(type);
    }

    
    
    @Override
    public String format(LiteralFormatter formatter) {
        return formatter.formatString(this);
    }
    
    
    
    @Override
    public Expression transform(Transformation transformation) 
            throws ASTTraversalException {
        return transformation.transformString(this);
    }
    
    
    
    @Override
    public int compareTo(Literal o) {
        if (o instanceof StringLiteral) {
            final StringLiteral other = (StringLiteral) o;
            return this.getValue().compareTo(other.getValue());
        }
        return super.compareTo(o);
    }
    
    
    
    @Override
    public String toString() {
        return this.value;
    }
    
    
    
    @Override
    public Class<?> getEquivalenceClass() {
        return StringLiteral.class;
    }
    
    
    
    @Override
    public boolean actualEquals(Equatable o) {
        final StringLiteral other = (StringLiteral) o;
        return this.value.equals(other.value);
    }
}
