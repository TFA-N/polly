package commands;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.UserManager;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.ConstraintException;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.UnknownAttributeException;
import de.skuzzle.polly.sdk.model.User;


public class SetAttributeCommand extends Command {

    public SetAttributeCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "setattr");
        this.createSignature("Setzt das Attribut des angegebenen Benutzers neu. " +
        		"Diese Befehel ist nur f�r Admins", 
            new Parameter("User", Types.newUser()), 
            new Parameter("Attributname", Types.newString()), 
            new Parameter("Attributwert", Types.newString()));
        this.createSignature("Setzt das Attribut auf den angegebenen Wert.", 
            new Parameter("Attributname", Types.newString()), 
            new Parameter("Attributwert", Types.newString()));
        this.setRegisteredOnly();
        this.setHelpText("Setzt ein Attribut auf den angegebenen Wert. Verf�gbare " +
        		"Attribute k�nnen mit :listattr angezeigt werden.");
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
            Signature signature) throws CommandException {
        
        if (this.match(signature, 0)) {
            if (executer.getUserLevel() < UserManager.ADMIN) {
                throw new CommandException(
                    "Du kannst keine Attribute f�r andere Benutzer �ndern");
            }
            
            String user = signature.getStringValue(0);
            final String attribute = signature.getStringValue(1);
            final String value = signature.getStringValue(2);
            
            final User dest = this.getMyPolly().users().getUser(user);
            this.setAttribute(dest, user, attribute, value, channel);
        } else if (this.match(signature, 1)) {
            final String attribute = signature.getStringValue(0);
            final String value = signature.getStringValue(1);
            
            this.setAttribute(executer, executer.getName(), attribute, value, 
                channel);
        }
        return false;
    }
    
    
    
    private void setAttribute(User dest, String userName, String attribute, 
            String value, String channel) throws CommandException {
        
        if (dest == null) {
            throw new CommandException("Unbekannter Benutzer: " + userName);
        } 
        
        try {
            dest.getAttribute(attribute);
        } catch (UnknownAttributeException e) {
            throw new CommandException("Unbekanntes Attribut: " + attribute);
        }
        
        try {
            this.getMyPolly().users().setAttributeFor(dest, attribute, value);
            this.reply(channel, "Neuer Wert wurde gespeichert.");
        } catch (DatabaseException e) {
            throw new CommandException(e);
        } catch (ConstraintException e) {
            throw new CommandException(e.getMessage());
        }
    }

}
