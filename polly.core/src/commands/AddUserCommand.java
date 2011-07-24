package commands;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.UserManager;
import de.skuzzle.polly.sdk.Types.UserType;
import de.skuzzle.polly.sdk.Types.StringType;
import de.skuzzle.polly.sdk.Types.NumberType;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.UserExistsException;
import de.skuzzle.polly.sdk.model.User;

public class AddUserCommand extends Command {

    public AddUserCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "adduser");
        this.createSignature("F�gt einen neuen User hinzu.", 
                new UserType(), new StringType(), new NumberType());
        this.setRegisteredOnly();
        this.setUserLevel(UserManager.ADMIN);
        this.setHelpText("Befehl zum registrieren neuer Benutzer bei Polly.");
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
            Signature signature) {
        return true;
    }

    
    
    @Override
    protected void executeOnChannel(User executer, String channel,
            Signature signature) {
        this.reply(channel, "Dieser Befehl kann nur im Query ausgef�hrt werden.");
    }
    
    
    
    @Override
    protected void executeOnQuery(User executer, Signature signature) {
        if (this.match(signature, 0)) {
            String userName = signature.getStringValue(0);
            String password = signature.getStringValue(1);
            int userLevel = (int) signature.getNumberValue(2);
            
            try {
                this.getMyPolly().users().addUser(userName, password, userLevel);
                this.reply(executer, "Benutzer '" + userName + "' angelegt.");
            } catch (UserExistsException e) {
                this.reply(executer, "Benutzer '" + userName + "' existiert bereits.");
            } catch (DatabaseException e)  {
                this.reply(executer, "Interner Datenbankfehler: " + e.getMessage());
            }
        }
    }
}
