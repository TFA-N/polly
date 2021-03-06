package de.skuzzle.polly.core.parser.ast.declarations;

import java.util.List;

import de.skuzzle.polly.core.parser.Position;
import de.skuzzle.polly.core.parser.ast.visitor.ASTTraversalException;


public class DeclarationException extends ASTTraversalException {
    
    /** Max similar suggestions to show in error message */
    public final static int MAX_SUGGESTIONS = 3;

    private static final long serialVersionUID = 1L;
    
    private final List<String> similar;
    private final String message;
    
    
    
    public DeclarationException(Position position, String message, List<String> similar) {
        super(position, message);
        this.similar = similar;
        
        final StringBuilder b = new StringBuilder();
        if (similar.isEmpty()) {
            this.message = message;
            return;
        }
        b.append(message);
        b.append(" Meintest du vielleicht: ");
        int max = Math.min(MAX_SUGGESTIONS, similar.size());
        for (int i = 0; i < max; ++i) {
            b.append("'");
            b.append(similar.get(i));
            b.append("'");
            if (i == max - 2) {
                b.append(" oder ");
            } else if (i < max - 1) {
                b.append(", ");
            }
        }
        b.append("? In Eingabe an Position: ");
        b.append(position);
        this.message = b.toString();
    }
    
    
    
    public List<String> getSimilar() {
        return this.similar;
    }
    
    
    
    @Override
    public String getMessage() {
        return this.message;
    }
}
