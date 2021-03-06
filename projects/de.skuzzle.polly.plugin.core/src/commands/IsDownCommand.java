package commands;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import polly.core.MSG;
import polly.core.MyPlugin;
import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.User;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.InsufficientRightsException;



public class IsDownCommand extends Command {

    public IsDownCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "isdown"); //$NON-NLS-1$
        this.createSignature(MSG.isDownSig0Desc, 
    		MyPlugin.ISDOWN_PERMISSION,
    		new Parameter(MSG.isDownSig0Url, Types.STRING));
        this.createSignature(MSG.isDownSig1Desc, 
    		MyPlugin.ISDOWN_PERMISSION,
    		new Parameter(MSG.isDownSig1Url, Types.STRING),
    		new Parameter(MSG.isDownSig1Timeout, Types.NUMBER));
        this.setHelpText(MSG.isDownHelp);
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel, Signature signature) 
            throws CommandException, InsufficientRightsException {
        
        String url = signature.getStringValue(0);
        url = url.startsWith("http://") ? url : "http://" + url; //$NON-NLS-1$ //$NON-NLS-2$
        
        int timeout = 5000;
        if (this.match(signature, 1)) {
            timeout = (int) signature.getNumberValue(1);
        }
        
        try {
            URL u = new URL(url);
            URLConnection c = u.openConnection();
            c.setConnectTimeout(timeout);
            c.connect();
            this.reply(channel, MSG.bind(MSG.isDownReachable, url));
        } catch (MalformedURLException e) {
            this.reply(channel, MSG.bind(MSG.isDownInvalidUrl, url));
        } catch (UnknownHostException e) {
            this.reply(channel, MSG.bind(MSG.isDownUnknownHost, url));
        } catch (IOException e) {
            e.printStackTrace();
            this.reply(channel, MSG.bind(MSG.isDownNotReachable, url));
        }
        return false;
    }

}
