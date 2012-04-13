package commands;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import core.TVProgram;
import core.TVProgramProvider;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.Types.ListType;
import de.skuzzle.polly.sdk.Types.StringType;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.model.User;


public class TVNextCommand extends AbstractTvCommand {

    public TVNextCommand(MyPolly polly, TVProgramProvider tvProvider)
        throws DuplicatedSignatureException {
        super(polly, tvProvider, "tvnext");
        
        this.createSignature("Gibt die n�chste Sendung f�r den angegebenen Channel aus.", 
            new Parameter("Sender", Types.STRING));
        this.createSignature("Gibt die n�chste Sendung f�r alle angegebenen Channel aus.", 
            new Parameter("Senderliste", new ListType(Types.STRING)));
        this.setHelpText("Befehl um das n�chste TV Programm eines Senders auszugeben.");
    }
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel,
                Signature signature) throws CommandException {
        if (this.match(signature, 0)) {
            String c = signature.getStringValue(0);
            this.replyPrograms(channel, Collections.singletonList(this.tvProvider.getNext(c)));
        } else if (this.match(signature, 1)) {
            List<StringType> cs = signature.getListValue(StringType.class, 0);
            List<TVProgram> channels = new LinkedList<TVProgram>();
            
            for (StringType st : cs) {
                channels.add(this.tvProvider.getNext(st.getValue()));
            }
            this.replyPrograms(channel, channels);
        }
        return false;
    }

}
