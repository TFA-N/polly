package commands;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.UserManager;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.model.User;

public class ClumBombCommand extends Command {

	public ClumBombCommand(MyPolly polly) throws DuplicatedSignatureException {
		super(polly, "clumbomb");
		this.createSignature("Highlightbombe!", 
		    new Parameter("Benutzer",Types.newUser()), 
		    new Parameter("Anzahl", Types.newNumber()));
		this.setRegisteredOnly();
		this.setUserLevel(UserManager.MEMBER);
	}

	
	
	@Override
	protected boolean executeOnBoth(User executer, String channel,
	        Signature signature) {
	    return true;
	}

	
	
	@Override
	protected void executeOnChannel(User executer, String channel,
			Signature signature) {
		
		if (this.match(signature, 0)) {
			String user = signature.getStringValue(0);
			int i = (int) signature.getNumberValue(1);
			i = Math.min(10, i);
			
			for (int j = 0; j < i; j++) {
				this.reply(channel, user);
			}
		}
	}
}