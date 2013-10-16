package commands;

import polly.core.MSG;
import polly.core.MyPlugin;
import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.CommandHistoryEntry;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.User;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.InsufficientRightsException;


public class DitoCommand extends Command {

    public DitoCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "dito"); //$NON-NLS-1$
        this.createSignature(MSG.ditoSig0Desc.s, MyPlugin.DITO_PERMISSION);
        this.setHelpText(MSG.ditoHelp.s);
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
            Signature signature) throws CommandException, InsufficientRightsException {
        
        if (this.match(signature, 0)) {
            CommandHistoryEntry che = 
                this.getMyPolly().commands().getLastCommand(channel);
            
            if (che == null) {
                this.reply(channel, MSG.ditoNoCommand.s);
            } else {
                boolean qry = channel.equals(executer.getCurrentNickName());
                
                Signature sig = che.getSignature();
                for (int i = 0; i < sig.getParameters().size(); ++i) {
                    Types type = sig.getParameters().get(i);
                    
                    if (type instanceof Types.UserType) {
                        Types.UserType ut = (Types.UserType) type;
                        
                        if (ut.getValue().equals(che.getExecuterName())) {
                            // this was a 'me' as parameter, so replace it with 
                            // current executor
                            sig.getParameters().set(i, new Types.UserType(
                                executer.getCurrentNickName()));
                        }
                    }
                }
                che.getCommand().doExecute(executer, channel, qry, che.getSignature());
            }
        }
        return false;
    }

    
    
    @Override
    public boolean trackInHistory() {
        return false;
    }
}
