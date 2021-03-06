package de.skuzzle.polly.core.parser.ast.expressions.literals;

import de.skuzzle.polly.core.parser.Position;
import de.skuzzle.polly.core.parser.ast.declarations.types.Type;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversalException;
import de.skuzzle.polly.core.parser.ast.visitor.Transformation;

/**
 * Represents a help literal. This is a single '?' and used to provide a help text to the 
 * user for polly commands.
 * 
 * @author Simon Taddiken
 */
public class HelpLiteral extends Literal {

    /**
     * Creates a new HelpLiteral.
     * 
     * @param position The position of this literal within the source.
     */
    public HelpLiteral(Position position) {
        super(position, Type.HELP);
    }

    
    
    @Override
    public String format(LiteralFormatter formatter) {
        return formatter.formatHelp(this);
    }

    
    
    @Override
    public HelpLiteral transform(Transformation transformation) throws ASTTraversalException {
        return transformation.transformHelp(this);
    }

}
