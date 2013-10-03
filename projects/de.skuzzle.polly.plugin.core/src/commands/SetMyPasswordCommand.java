package commands;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.PersistenceManagerV2;
import de.skuzzle.polly.sdk.PersistenceManagerV2.Atomic;
import de.skuzzle.polly.sdk.PersistenceManagerV2.Write;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.User;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;



public class SetMyPasswordCommand extends Command {

    public SetMyPasswordCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "setmypw");
        this.createSignature("Setzt dein Passwort neu. Gib dein altes Passwort und " +
        		"den gew�nschtes Passwort an.", 
        		new Parameter("Altes Passwort", Types.STRING), 
        		new Parameter("Neues Passwort", Types.STRING));
        this.setRegisteredOnly();
        this.setHelpText("Befehl um dein Passwort zu �ndern.");
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
        this.reply(channel, "Dieser Befehl ist nur im Query ausf�hrbar. " +
        		"Du solltest zudem ein anderes Passwort w�hlen.");
    }
    
    
    
    @Override
    protected void executeOnQuery(final User executer, Signature signature) 
            throws CommandException {
        if (this.match(signature, 0)) {
            String oldPw = signature.getStringValue(0);
            final String newPw = signature.getStringValue(1);
            
            if (!executer.checkPassword(oldPw)) {
                this.reply(executer, "Das aktuelle Passwort stimmt nicht mit dem " +
                		"angegebenen überein!");
                return;
            }
            
            final PersistenceManagerV2 persistence = this.getMyPolly().persistence();
            try {
                persistence.writeAtomic(new Atomic() {
                    
                    @Override
                    public void perform(Write write) {
                        executer.setPassword(newPw);
                    }
                });
            } catch (DatabaseException e) {
                throw new CommandException(e);
            }
        }
    }
}
