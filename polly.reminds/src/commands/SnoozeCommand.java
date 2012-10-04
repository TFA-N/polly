package commands;

import java.util.Date;

import polly.reminds.MyPlugin;

import core.RemindManager;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.UnknownAttributeException;
import de.skuzzle.polly.sdk.model.User;

public class SnoozeCommand extends AbstractRemindCommand {

    
    public SnoozeCommand(MyPolly polly, RemindManager remindManager) 
            throws DuplicatedSignatureException {
        super(polly, remindManager, "snooze");
        this.createSignature("Verl�ngert die Erinnerung die dir zuletzt zugestellt wurde",
            MyPlugin.SNOOZE_PERMISSION,
            new Parameter("Neue Zeit", Types.DATE));
        this.createSignature(
            "Verl�ngert die Erinnerung die dir zuletzt zugestellt wurde.");
        this.setRegisteredOnly();
        this.setHelpText("Verl�ngert Erinnerungen");
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
            Signature signature) throws CommandException {
        
        if (this.match(signature, 0)) {
            Date dueDate = signature.getDateValue(0);
            
            try {
                this.remindManager.snooze(executer, dueDate);
                this.reply(channel, "Erinnerung wurde verl�ngert.");
            } catch (DatabaseException e) {
                throw new CommandException(e);
            }
        } else if (this.match(signature, 1)) {
            try {
                long millis = System.currentTimeMillis();
                millis += Long.parseLong(
                    executer.getAttribute(MyPlugin.DEFAULT_REMIND_TIME));
                Date dueDate = new Date(millis);
                
                this.remindManager.snooze(executer, dueDate);
                this.reply(channel, "Erinnerung wurde verl�ngert.");
                
            } catch (UnknownAttributeException e) {
                throw new CommandException(e);
            } catch (DatabaseException e) {
                throw new CommandException(e);
            }
        }
        return false;
    }

}
