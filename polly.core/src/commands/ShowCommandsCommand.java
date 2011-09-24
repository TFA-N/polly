package commands;

import java.util.Collection;
import java.util.Iterator;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.model.User;
import de.skuzzle.polly.sdk.Types.NumberType;



public class ShowCommandsCommand extends Command {

    public ShowCommandsCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "cmds");
        this.createSignature("Zeigt alle f�r dich ausf�hrbaren Befehle an.");
        this.createSignature("Zeigt alle Befehle f�r das angegebene User-Level an.", 
                new NumberType());
        this.setHelpText("Listet die verf�gbaren Befehle auf.");
    }
    
    
    
    private boolean canExecute(User user, int userLevel, Command cmd) {
        boolean userOnline = this.getMyPolly().users().isSignedOn(user);
        boolean result = cmd.getUserLevel() <= userLevel;
        
        result = userOnline ? result : result && !cmd.isRegisteredOnly();
        return result;
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
            Signature signature) {

        int level = executer.getUserLevel();
        if (this.match(signature, 1)) {
            level = (int) signature.getNumberValue(0);
        }
        
        StringBuilder b = new StringBuilder();
        Collection<Command> cmds = this.getMyPolly().commands().getRegisteredCommands();
        
        Iterator<Command> it = cmds.iterator();
        if (!it.hasNext()) {
            return false;
        }
        Command cmd = it.next();
        // ISSUE: 0000041
        //        fixed with this boolean trigger to add comma only if needed 
        boolean addComma = false;
        while (cmd != null) {
            if (this.canExecute(executer, level, cmd)) {
                b.append(cmd.getCommandName());
                addComma = true;
            }
            
            if (it.hasNext()) {
                cmd = it.next();
                if (this.canExecute(executer, level, cmd) && addComma) {
                    b.append(", ");
                }
            } else { 
                cmd = null;
            }
        }
        
        this.reply(channel, "Verf�gbare Befehle: " + b.toString());
        return false;
    }

}
