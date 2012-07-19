package polly.logging;


import commands.ChannelLogCommand;
import commands.ReplayCommand;
import commands.SeenCommand;
import commands.UserLogCommand;

import core.ForwardHighlightHandler;
import core.IrcLogCollector;
import core.PollyLoggingManager;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.PollyPlugin;
import de.skuzzle.polly.sdk.constraints.Constraints;
import de.skuzzle.polly.sdk.eventlistener.MessageListener;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DisposingException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.IncompatiblePluginException;
import de.skuzzle.polly.sdk.exceptions.RoleException;
import de.skuzzle.polly.sdk.roles.RoleManager;
import entities.LogEntry;



public class MyPlugin extends PollyPlugin {

    public final static String LOGGING_ROLE           = "polly.roles.LOGGING";
    public final static String USER_LOG_PERMISSION    = "polly.permission.USER_LOG";
    public final static String CHANNEL_LOG_PERMISSION = "polly.permission.CHANNEL_LOG";
    public final static String REPLAY_PERMISSION      = "polly.permission.REPLAY";
    public final static String SEEN_PERMISSION        = "polly.permission.SEEN";
    
    
    public final static String LOG_CACHE_SIZE     = "logCacheSize";
    public final static String LOG_PASTE_TRESHOLD = "logPasteTreshold";
    public final static String LOG_MAX_LOGS       = "logMaxLogs";
    
    public final static int DEFAULT_LOG_CACHE_SIZE = 100;
    public final static int DEFAULT_LOG_TRESHOLD   = 10;
    public final static int DEFAULT_MAX_LOGS       = 100;
    
    public final static String FORWARD_HIGHLIGHTS         = "FORWARD_HIGHLIGHTS";
    public final static String DEFAULT_FORWARD_HIGHLIGHTS = "false";
    
    private IrcLogCollector logCollector;
    private PollyLoggingManager logManager;
    private MessageListener highlightForwarder;
    
    
    public MyPlugin(MyPolly myPolly) throws IncompatiblePluginException, 
                DuplicatedSignatureException {
        
        super(myPolly);

        myPolly.persistence().registerEntity(LogEntry.class);
        
        
        int cacheSize = myPolly.configuration().readInt(
                LOG_CACHE_SIZE, DEFAULT_LOG_CACHE_SIZE);
        int pasteTreshold = myPolly.configuration().readInt(
            LOG_PASTE_TRESHOLD, DEFAULT_LOG_TRESHOLD);
        int maxLogs = myPolly.configuration().readInt(LOG_MAX_LOGS, DEFAULT_MAX_LOGS);
        

        this.logManager = new PollyLoggingManager(
            myPolly, cacheSize, pasteTreshold, maxLogs);
        
        this.logCollector = new IrcLogCollector(this.logManager);
        
        this.addDisposable(this.logManager);
        
        myPolly.irc().addJoinPartListener(this.logCollector);
        myPolly.irc().addMessageListener(this.logCollector);
        myPolly.irc().addQuitListener(this.logCollector);
        myPolly.irc().addNickChangeListener(this.logCollector);
        
        this.addCommand(new UserLogCommand(myPolly, this.logManager));
        this.addCommand(new ChannelLogCommand(myPolly, this.logManager));
        this.addCommand(new SeenCommand(myPolly, this.logManager));
        this.addCommand(new ReplayCommand(myPolly, this.logManager));
        
        this.highlightForwarder = new ForwardHighlightHandler(myPolly, this.logManager);
        myPolly.irc().addMessageListener(this.highlightForwarder);
    }
    
    
    
    @Override
    public void onLoad() {
        try {
            this.getMyPolly().users().addAttribute(FORWARD_HIGHLIGHTS, 
                DEFAULT_FORWARD_HIGHLIGHTS, Constraints.BOOLEAN);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }
    
    
    
    @Override
    public void assignPermissions(RoleManager roleManager)
            throws RoleException, DatabaseException {
        
        roleManager.createRole(LOGGING_ROLE);
        roleManager.assignPermission(LOGGING_ROLE, USER_LOG_PERMISSION);
        roleManager.assignPermission(LOGGING_ROLE, CHANNEL_LOG_PERMISSION);
        roleManager.assignPermission(LOGGING_ROLE, REPLAY_PERMISSION);
        roleManager.assignPermission(LOGGING_ROLE, SEEN_PERMISSION);
        
        super.assignPermissions(roleManager);
    }
    
    
    
    @Override
    protected void actualDispose() throws DisposingException {
        super.actualDispose();
        
        this.getMyPolly().irc().removeJoinPartListener(this.logCollector);
        this.getMyPolly().irc().removeMessageListener(this.logCollector);
        this.getMyPolly().irc().removeQuitListener(this.logCollector);
        this.getMyPolly().irc().removeNickChangeListener(this.logCollector);
        this.getMyPolly().irc().removeMessageListener(this.highlightForwarder);
    }
}
