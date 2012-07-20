package commands;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.UserManager;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.UserExistsException;
import de.skuzzle.polly.sdk.model.User;



public class RegisterCommand extends Command {

    public RegisterCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "register");
        this.createSignature("Gib deinen gew�nschten Benutzernamen " +
        		"(am besten me) und dein gew�nschtes Passwort ein.", 
    		new Parameter("Username", Types.USER), 
		    new Parameter("Passwort", Types.STRING));
        this.createSignature("Gib dein gew�nschtes Passwort ein. Als Benutzername wird " +
        		"dein aktueller Nickname genutzt", 
    		new Parameter("Passwort", Types.STRING));
        this.setHelpText("Befehl um dich bei Polly zu registrieren.");
        this.setUserLevel(UserManager.UNKNOWN);
        this.setQryCommand(true);
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
            Signature signature) {
        return true;
    }
    
    
    
    @Override
    protected void executeOnChannel(User executer, String channel,
            Signature signature) {
        this.reply(channel, "Dieser Befehl ist nur im Query ausf�hrbar. Zudem solltest " +
        		"du jetzt ein anderes Passwort w�hlen.");
    }
    
    
    
    @Override
    protected void executeOnQuery(User executer, Signature signature) {
        if (this.getMyPolly().users().isSignedOn(executer)) {
            this.reply(executer, "Du bist bereits angemeldet.");
            return;
        }
        
        String userName = "";
        String password = "";
        if (this.match(signature, 0)) {
            userName = signature.getStringValue(0);
            password = signature.getStringValue(1);
        } else if (this.match(signature, 1)) {
            userName = executer.getCurrentNickName();
            password = signature.getStringValue(0);
        }
        try {
            this.getMyPolly().users().addUser(
                    userName, password);
            this.reply(executer, "Registrierung erfolgreich. Du kannst dich jetzt " +
            		"mit \":auth @" + userName + " " + password + "\" anmelden.");
        } catch (UserExistsException e) {
            this.reply(executer, "Der Benutzer '" + userName + 
                    "' existiert bereits.");
        } catch (DatabaseException e) {
            this.reply(executer, "Interner Datenbankfehler!");
            e.printStackTrace();
        }
    }
}
