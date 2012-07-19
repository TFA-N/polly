package commands;

import java.util.Collections;
import java.util.List;

import polly.core.MyPlugin;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.UserManager;
import de.skuzzle.polly.sdk.Types.ChannelType;
import de.skuzzle.polly.sdk.Types.ListType;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.model.User;

public class JoinCommand extends Command {

    
    
    public JoinCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "join");
        this.createSignature("L�sst polly den angegebenen Channel betreten.", 
            MyPlugin.JOIN_PERMISSION,
                new Parameter("Channel", Types.CHANNEL));
        this.createSignature("L�sst polly alle Channels in der Liste betreten.",
            MyPlugin.JOIN_PERMISSION,
                new Parameter("Channelliste", new ListType(Types.CHANNEL)));
        this.createSignature("L�sst polly einen Channel mit Passwort betreten",
            MyPlugin.JOIN_PERMISSION,
            new Parameter("Channel", Types.CHANNEL),
            new Parameter("Passwort", Types.STRING));
        this.setRegisteredOnly();
        this.setUserLevel(UserManager.ADMIN);
        this.setHelpText("Befehl zum joinen von Channels.");
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
            Signature signature) {

        List<ChannelType> channels = null;
        if (this.match(signature, 0)) {
            ChannelType ct = new ChannelType(signature.getStringValue(0));
            channels = Collections.singletonList(ct);
        } else if (this.match(signature, 1)) {
            channels = signature.getListValue(ChannelType.class, 0);
        } else if (this.match(signature, 2)) {
            String c = signature.getStringValue(0);
            String pw = signature.getStringValue(1);
            
            this.getMyPolly().irc().joinChannel(c, pw);
            return false;
        }
        
        if (channels == null) {
            throw new RuntimeException("This should not have happened. Command was " +
            		"called with illegal signature");
        }
        
        for (ChannelType ct : channels) {
            this.getMyPolly().irc().joinChannel(ct.getValue(), "");
        }
        
        return false;
    }

}
