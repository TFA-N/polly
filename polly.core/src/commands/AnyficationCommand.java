package commands;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import de.skuzzle.polly.sdk.DelayedCommand;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Parameter;
import de.skuzzle.polly.sdk.Signature;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.UserManager;
import de.skuzzle.polly.sdk.Types.TimespanType;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.DisposingException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.InsufficientRightsException;
import de.skuzzle.polly.sdk.model.User;


public class AnyficationCommand extends DelayedCommand {
    
    private final static int DELAY = 10 * 1000 * 60; // 10 minutes
    private final static int ANYFICATION_TIME = 2 * 1000 * 60; // 2 minutes
    
    
    private Timer anyficationTimer;
    private Set<String> channels;
    
    
    
    public AnyficationCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "anyfication", DELAY);
        this.createSignature("Anyfication!", 
            new Parameter("Prefix", Types.STRING));
        this.createSignature("Lordification innerhalb der angegebenen zeit!",
            new Parameter("Prefix", Types.STRING), 
            new Parameter("Zeitspanne", Types.TIMESPAN));
        this.setUserLevel(UserManager.ADMIN);
        this.anyficationTimer = new Timer("LORDIFICATION_TIMER", true);
        this.channels = new HashSet<String>();
    }
    
    
    
    @Override
    protected boolean executeOnBoth(User executer, String channel, Signature signature) 
            throws CommandException, InsufficientRightsException {
        return true;
    }
    
    
    
    @Override
    protected synchronized void executeOnChannel(User executer, final String channel, 
            Signature signature) throws CommandException, InsufficientRightsException {
        
        int timeSpan = ANYFICATION_TIME;
        final String prefix = signature.getStringValue(0);
        if (this.match(signature, 1)) {
            TimespanType t = (TimespanType) signature.getValue(1);
            timeSpan = (int) t.getSpan() * 1000;
        }
        if (this.channels.contains(channel)) {
            return;
        }
        
        StringBuilder b = new StringBuilder();
        Iterator<String> it = this.getMyPolly().irc().getChannelUser(channel).iterator();
        while (it.hasNext()) {
            b.append(it.next());
            if (it.hasNext()) {
                b.append(", ");
            }
        }
        b.append(": ihr habt " + 
            this.getMyPolly().formatting().formatTimeSpan(timeSpan / 1000) + 
            " Zeit um euch zu lordifizieren.");
        this.reply(channel, b.toString());
        this.getMyPolly().irc().setNickname(prefix + "-Polly");
        
        this.channels.add(channel);
        this.anyficationTimer.schedule(new TimerTask() {
            String chan = channel;
            
            @Override
            public void run() {
                channels.remove(chan);
                List<String> users = getMyPolly().irc().getChannelUser(chan);
                for (String user : users) {
                    if (!user.startsWith(prefix)) {
                        getMyPolly().irc().kick(chan, user, "Nicht lordifiziert!");
                    }
                }
                getMyPolly().irc().setAndIdentifyDefaultNickname();
            }
        }, timeSpan);
    }
    
    
    
    @Override
    protected void actualDispose() throws DisposingException {
        this.channels.clear();
        this.anyficationTimer.cancel();
    }
}