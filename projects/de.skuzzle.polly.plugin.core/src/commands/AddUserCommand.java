package commands;

import polly.core.MyPlugin;
import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.User;
import de.skuzzle.polly.sdk.UserManager;
import de.skuzzle.polly.sdk.Types.NumberType;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.InvalidUserNameException;
import de.skuzzle.polly.sdk.exceptions.UserExistsException;

public class AddUserCommand extends Command {

    public AddUserCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "adduser");
        this.createSignature("F�gt einen neuen User hinzu.", 
            MyPlugin.ADD_USER_PERMISSION,
            new Parameter("Username", Types.USER),
            new Parameter("Passwort", Types.STRING));
        this.setRegisteredOnly();
        this.setUserLevel(UserManager.ADMIN);
        this.setHelpText("Befehl zum registrieren neuer Benutzer bei Polly.");
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
        this.reply(channel, "Dieser Befehl kann nur im Query ausgef�hrt werden.");
    }
    
    
    
    @Override
    public void renewConstants() {
        this.registerConstant("ADMIN", new NumberType(UserManager.ADMIN));
        this.registerConstant("MEMBER", new NumberType(UserManager.MEMBER));
        this.registerConstant("REG", new NumberType(UserManager.REGISTERED));
        this.registerConstant("UNKNOWN", new NumberType(UserManager.UNKNOWN));
    }
    
    
    
    @Override
    protected void executeOnQuery(User executer, Signature signature) {
        if (this.match(signature, 0)) {
            String userName = signature.getStringValue(0);
            String password = signature.getStringValue(1);
            
            try {
                this.getMyPolly().users().addUser(userName, password);
                this.reply(executer, "Benutzer '" + userName + "' angelegt.");
            } catch (UserExistsException e) {
                this.reply(executer, "Benutzer '" + userName + "' existiert bereits.");
            } catch (DatabaseException e)  {
                this.reply(executer, "Interner Datenbankfehler: " + e.getMessage());
            } catch (InvalidUserNameException e) {
                this.reply(executer, "'" + userName + "' is no valid user name");
            }
        }
    }
}
