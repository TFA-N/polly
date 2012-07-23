package polly.core.irc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import de.skuzzle.polly.sdk.AbstractDisposable;
import de.skuzzle.polly.sdk.Configuration;
import de.skuzzle.polly.sdk.Disposable;
import de.skuzzle.polly.sdk.IrcManager;
import de.skuzzle.polly.sdk.eventlistener.ChannelEvent;
import de.skuzzle.polly.sdk.eventlistener.ChannelModeEvent;
import de.skuzzle.polly.sdk.eventlistener.ChannelModeListener;
import de.skuzzle.polly.sdk.eventlistener.ConnectionEvent;
import de.skuzzle.polly.sdk.eventlistener.ConnectionListener;
import de.skuzzle.polly.sdk.eventlistener.IrcUser;
import de.skuzzle.polly.sdk.eventlistener.JoinPartListener;
import de.skuzzle.polly.sdk.eventlistener.MessageEvent;
import de.skuzzle.polly.sdk.eventlistener.MessageListener;
import de.skuzzle.polly.sdk.eventlistener.MessageSendListener;
import de.skuzzle.polly.sdk.eventlistener.NickChangeEvent;
import de.skuzzle.polly.sdk.eventlistener.NickChangeListener;
import de.skuzzle.polly.sdk.eventlistener.OwnMessageEvent;
import de.skuzzle.polly.sdk.eventlistener.QuitEvent;
import de.skuzzle.polly.sdk.eventlistener.QuitListener;
import de.skuzzle.polly.sdk.eventlistener.SpotEvent;
import de.skuzzle.polly.sdk.eventlistener.UserSpottedListener;
import de.skuzzle.polly.sdk.exceptions.DisposingException;
import de.skuzzle.polly.tools.iterators.WrapIterator;

import polly.events.Dispatchable;
import polly.events.EventProvider;


/**
 * 
 * @author Simon
 * @version 27.07.2011 ae73250
 */
public class IrcManagerImpl extends AbstractDisposable implements IrcManager, Disposable {
    
    private static Logger logger = Logger.getLogger(IrcManagerImpl.class.getName());
    
    private PircBot bot = new PircBot() {
        @Override
        protected void onConnect() {
            IrcManagerImpl.this.fireConnectionEstablished(
                new ConnectionEvent(IrcManagerImpl.this));
        };
        
        
        
        protected void onDisconnect() {
            IrcManagerImpl.this.onlineUsers.clear();
            IrcManagerImpl.this.topics.clear();
            IrcManagerImpl.this.retry();
        };

        
        
        @Override
        protected void onNickChange(String oldNick, String login, String hostname,
                String newNick) {
            String oldIrcName = IrcManagerImpl.this.stripNickname(oldNick);
            String newIrcName = IrcManagerImpl.this.stripNickname(newNick);
            
            IrcUser oldUser = new IrcUser(oldIrcName, login, hostname);
            IrcUser newUser = new IrcUser(newIrcName, login, hostname);
            NickChangeEvent e = new NickChangeEvent(
                    IrcManagerImpl.this, oldUser, newUser);
            
            synchronized (IrcManagerImpl.this.onlineUsers) {
                IrcManagerImpl.this.onlineUsers.remove(oldIrcName);
                IrcManagerImpl.this.onlineUsers.add(newIrcName);
            }
            IrcManagerImpl.this.fireNickChange(e);
        }
        
        
        
        @Override
        protected void onJoin(String channel, String sender, String login,
                String hostname) {

            String nickName = IrcManagerImpl.this.stripNickname(sender);
            IrcUser user = new IrcUser(nickName, login, hostname);
            ChannelEvent e = new ChannelEvent(IrcManagerImpl.this, user, channel);
            
            if (!IrcManagerImpl.this.onlineUsers.contains(nickName)) {
                SpotEvent e1 = new SpotEvent(IrcManagerImpl.this, user, channel, 
                    SpotEvent.USER_JOINED);
                IrcManagerImpl.this.fireUserSpotted(e1);
            }
            IrcManagerImpl.this.onlineUsers.add(nickName);
            IrcManagerImpl.this.fireJoin(e);
        }
        
        
        
        @Override
        protected synchronized void onPart(String channel, String sender, String login,
                String hostname) {
            
            String nickName = IrcManagerImpl.this.stripNickname(sender);
            IrcUser user = new IrcUser(nickName, login, hostname);
            ChannelEvent e = new ChannelEvent(IrcManagerImpl.this, user, channel);
            
            /* ISSUE: 0000002 && 0000026*/
            boolean known = false;
            for (String c : this.getChannels()) {
                
                // HACK: continue on own channel as pircbot may not have removed the user
                //       from its data structure
                if (c.equals(channel)) {
                    continue;
                }
                
                for (User u : this.getUsers(c)) {
                    String uStripped = IrcManagerImpl.this.stripNickname(u.getNick());
                    
                    if (uStripped.equals(nickName)) {
                        known = true;
                        break;
                    }
                }
            }
            if (!known) {
                SpotEvent e1 = new SpotEvent(IrcManagerImpl.this, user, channel, 
                        SpotEvent.USER_PART);
                IrcManagerImpl.this.onlineUsers.remove(sender);
                IrcManagerImpl.this.fireUserLost(e1);
            }
            
            // XXX: not sure if this makes sense or not
            IrcManagerImpl.this.topics.remove(channel);
            IrcManagerImpl.this.firePart(e);
        }
        
        
        
        @Override
        protected void onQuit(String sourceNick, String sourceLogin,
                String sourceHostname, String reason) {
            
            String nickName = IrcManagerImpl.this.stripNickname(sourceNick);
            IrcUser user = new IrcUser(nickName, sourceLogin, sourceHostname);
            QuitEvent e = new QuitEvent(IrcManagerImpl.this, user, reason);
            SpotEvent e1 = new SpotEvent(e);
            
            IrcManagerImpl.this.onlineUsers.remove(nickName);
            IrcManagerImpl.this.fireQuit(e);
            IrcManagerImpl.this.fireUserLost(e1);
        }
        
        
        
        @Override
        protected void onMessage(String channel, String sender, String login,
                String hostname, String message) {
            
            String nickName = IrcManagerImpl.this.stripNickname(sender);
            IrcUser user = new IrcUser(nickName, login, hostname);
            MessageEvent e = new MessageEvent(IrcManagerImpl.this, user, 
                    channel, message);
            
            IrcManagerImpl.this.firePublicMessageEvent(e);
        }
        
        
        
        protected void onNotice(String sourceNick, String sourceLogin, 
            String sourceHostname, String target, String notice) {
            
            String nickName = IrcManagerImpl.this.stripNickname(sourceNick);
            IrcUser user = new IrcUser(nickName, sourceLogin, sourceHostname);
            MessageEvent e = new MessageEvent(IrcManagerImpl.this, user, 
                    target, notice);
            
            IrcManagerImpl.this.fireNoticeMessageEvent(e);
        };
        
        
        
        @Override
        protected void onPrivateMessage(String sender, String login, String hostname,
                String message) {
            
            String nickName = IrcManagerImpl.this.stripNickname(sender);
            IrcUser user = new IrcUser(nickName, login, hostname);
            MessageEvent e = new MessageEvent(IrcManagerImpl.this, user, 
                    nickName, message);
            
            IrcManagerImpl.this.firePrivateMessageEvent(e);
        }
        
        
        
        @Override
        protected void onAction(String sender, String login, 
                String hostname, String target, String action) {
            String nickName = IrcManagerImpl.this.stripNickname(sender);
            IrcUser user = new IrcUser(nickName, login, hostname);

            MessageEvent e = new MessageEvent(IrcManagerImpl.this, user,
                    target, action);
            
            IrcManagerImpl.this.fireActionMessageEvent(e);
        }
        
        
        
        @Override
        protected void onMode(String channel, String sourceNick, String sourceLogin, 
                String sourceHostname, String mode) {
            
            String nickName = IrcManagerImpl.this.stripNickname(sourceNick);
            IrcUser user = new IrcUser(nickName, sourceLogin, sourceHostname);
            ChannelModeEvent e = new ChannelModeEvent(IrcManagerImpl.this, user, 
                channel, mode);
            
            IrcManagerImpl.this.fireChannelModeEvent(e);
        };
        
        
        
        protected void onUserList(String channel, org.jibble.pircbot.User[] users) {
            for (int i = 0; i < users.length; ++i) {
                String nickName = IrcManagerImpl.this.stripNickname(users[i].getNick());
                
                synchronized (IrcManagerImpl.this.onlineUsers) {
                    if (IrcManagerImpl.this.onlineUsers.add(nickName)) {
                        IrcUser user = new IrcUser(nickName, channel, "");
                        SpotEvent e = new SpotEvent(IrcManagerImpl.this, user, channel, 
                                SpotEvent.USER_JOINED);
                        IrcManagerImpl.this.fireUserSpotted(e);
                    }
                }
            }
        }
        
        
        
        protected void onChannelInfo(String channel, int userCount, String topic) {
            IrcManagerImpl.this.topics.put(channel, topic);
        }
    };
    
    
    private EventProvider eventProvider;
    private Set<String> onlineUsers;
    private Map<String, String> topics;
    private Configuration config;
    private boolean disconnect;
    private BotConnectionSettings recent;
    private MessageScheduler messageScheduler;
    
    
    
    public IrcManagerImpl(String ircName, EventProvider eventProvider, 
            Configuration config, String encodingName) {
        this.config = config;
        this.onlineUsers = Collections.synchronizedSet(new TreeSet<String>());
        this.topics = new HashMap<String, String>();
        this.eventProvider = eventProvider;
        this.bot.changeNick(ircName);
        try {
            this.bot.setEncoding(encodingName);
        } catch (UnsupportedEncodingException e) {
            logger.error("Encoding exception", e);
        }
        
        // Message thread has its own delay
        this.bot.setMessageDelay(0);
        
        /* 
         * IRC Verbose output
         */
        this.bot.setVerbose(false);
        
        this.messageScheduler = new RoundRobinScheduler(this, 
            config.readInt(Configuration.MESSAGE_DELAY));
        this.messageScheduler.start();
    }
    
    
    
    private String stripNickname(String nickName) {
        char c = nickName.toCharArray()[0];
        switch (c) {
        case '*':
        case '!':
        case ':':
        case '%':
            nickName = nickName.substring(1);
        }
        return nickName;
    }
    
    

	public void connect(BotConnectionSettings e) 
            throws NickAlreadyInUseException, IOException, IrcException {

	    if (this.isConnected()) {
	        return;
	    }
        this.bot.connect(e.getHostName(), e.getPort());
        
        this.bot.sendMessage("nickserv", "ghost " + e.getNickName() + " " + e.getIdentity());
        this.setAndIdentifyDefaultNickname();
        this.joinChannels(e.getChannels().toArray(new String[e.getChannels().size()]));
        this.recent = e;
        this.sendRawCommand("MODE " + e.getNickName() + " " + e.getModes());
    }
    
    

    @Override
	public void quit(String message) {
        if (this.isConnected()) {
            this.disconnect = true;
            this.bot.quitServer(message);
        }
    }
    
    

    @Override
	public void quit() {
    	this.quit("");
    }
    
    

    @Override
	public boolean isOnline(String nickName) {
        synchronized (this.onlineUsers) {
            return this.onlineUsers.contains(nickName);
        }
    }
    
    
    
    @Override
    public boolean isOnlineIgnoreCase(String nickName) {
        synchronized (this.onlineUsers) {
            for (String s : this.onlineUsers) {
                if (s.equalsIgnoreCase(nickName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    
    @Override
    public List<String> getChannelUser(String channel) {
        Set<String> result = new HashSet<String>();
        User[] users = this.bot.getUsers(channel);
        for (User user : users) {
            result.add(this.stripNickname(user.getNick()));
        }
        // ISSUE: 0000037
        // HACK: first, add user to set, then create list from set so that it contains
        //       no duplicates
        // CONSIDER: Make return Value Set<String>
        return new LinkedList<String>(result);
    }
    
    

    @Override
	public Set<String> getOnlineUsers() {
        return this.onlineUsers;
    }
    
    

    @Override
	public void disconnect() {
        this.disconnect = true;
        this.bot.disconnect();
    }
    
    
    
    @Override
	public boolean isConnected() {
        return this.bot.isConnected();
    }
    
    
    
    @Override
	public List<String> getChannels() {
        return Arrays.asList(this.bot.getChannels());
    }
    
    
    
    @Override
	public boolean isOnChannel(String channel, String nickName) {       
        User[] users = this.bot.getUsers(channel);
        for (User user : users) {
            if (this.stripNickname(user.getNick()).equalsIgnoreCase(nickName)) {
                return true;
            }
        }
        return false;
    }
    
    
    
    @Override
    public void rejoinDefaultChannels() {
        this.joinChannels(this.recent.getChannels().toArray(
            new String[this.recent.getChannels().size()]));
    }
    
    
    
    @Override
	public void joinChannel(String channel, String password) {
        if (!channel.startsWith("#")) {
            throw new IllegalArgumentException("Channel must be preceded with '#'");
        }
        if (this.isConnected()) {
            this.bot.joinChannel(channel, password);
        }
    }
    
    
    
    @Override
    public void joinChannels(String...channels) {
        for (String channel : channels) {
            this.joinChannel(channel, "");
        }
    }
    
    
    
    @Override
	public void partChannel(String channel, String message) {
        if (this.isConnected()) {
            this.bot.partChannel(channel, message);
        }
    }
    
    
    
    @Override
	public void kick(String channel, String nickName, String reason) {
    	logger.info("Kicking user " + nickName + " from " + channel + 
    			". Reason: " + reason);
    	this.bot.kick(channel, nickName, reason);
    }
    
    
    
    @Override
	public void op(String channel, String nickName) {
    	this.bot.op(channel, nickName);
    }
    
    
    
    @Override
	public void deop(String channel, String nickName) {
    	this.bot.deOp(channel, nickName);
    }
    
    
    
    @Override
    public void sendMessage(String channel, String message, Object source) {
        this.messageScheduler.addMessage(channel, message, source);
        
        OwnMessageEvent e = new OwnMessageEvent(this, 
            new IrcUser(this.getNickname(), "", ""), channel, message, source);
        this.fireMessageSend(e);
    }
   
    
    
    @Override
	public void sendMessage(String channel, String message) {
        if (this.isConnected()) {
            for (String line : new WrapIterator(message, 
                this.config.readInt(Configuration.LINE_LENGTH))) {
                this.bot.sendMessage(channel, line);
            }
        }
    }
    
    
    
    @Override
	public synchronized void sendAction(String channel, String message) {
        if (this.isConnected()) {
            for (String line : new WrapIterator(message, 
                        this.config.readInt(Configuration.LINE_LENGTH))) {
                this.bot.sendAction(channel, line);
            }
        }
    }
    
    
    
    @Override
    public void sendRawCommand(String command) {
        if (this.isConnected()) {
            this.bot.sendRawLineViaQueue(command);
        }
    }
    
    
    
    @Override
    public void setTopic(String channel, String topic) {
        if (this.isConnected()) {
            this.bot.setTopic(channel, topic);
        }
    }
    
    
    
    @Override
    public String getTopic(String channel) {
        if (this.isConnected()) {
            return this.topics.get(channel) == null ? "" : this.topics.get(channel);
        }
        return "";
    }
    
    
    
    @Override
    public String getNickname() {
        return this.bot.getNick();
    }
    
    
    
    @Override
    public void setNickname(String nickname) {
        this.bot.changeNick(nickname);
    }

    
    @Override
    public void setAndIdentifyDefaultNickname() {
        this.bot.changeNick(this.config.readString(Configuration.NICKNAME));
        this.bot.identify(this.config.readString(Configuration.IDENT));
    }
    
    
    public void retry() {
        this.fireConnectionLost(new ConnectionEvent(this, disconnect));
        
        if (this.disconnect) {
            return;
        }
        
        int attempts = 1;
        logger.warn("IRC Connection lost. Trying to reconnect...");
        while (!this.isConnected()) {
            try {
                this.connect(this.recent);
                this.disconnect = false;
                logger.info("IRC Connection reestablished.");
            } catch (Exception e) {
                logger.warn("Reconnect failed. Starting attempt " + ++attempts +"...", e);
                try {
                    Thread.sleep(this.config.readInt(Configuration.RECONNECT_DELAY));
                } catch (InterruptedException e1) {
                    logger.error("", e1);
                }
            }
            
        }
    }
    
    
    
    @Override
    public void addMessageSendListener(MessageSendListener listener) {
        this.eventProvider.addListener(MessageSendListener.class, listener);
    }
    
    
    
    @Override
    public void removeMessageSendListener(MessageSendListener listener) {
        this.eventProvider.removeListener(MessageSendListener.class, listener);
    }
    
    

    @Override
	public void addNickChangeListener(NickChangeListener listener) {
        this.eventProvider.addListener(NickChangeListener.class, listener);
    }
    
    

    @Override
	public void removeNickChangeListener(NickChangeListener listener) {
        this.eventProvider.removeListener(NickChangeListener.class, listener);
    }
    
    

    @Override
	public void addJoinPartListener(JoinPartListener listener) {
        this.eventProvider.addListener(JoinPartListener.class, listener);
    }
    
    

    @Override
	public void removeJoinPartListener(JoinPartListener listener) {
        this.eventProvider.removeListener(JoinPartListener.class, listener);
    }
    
    

    @Override
	public void addQuitListener(QuitListener listener) {
        this.eventProvider.addListener(QuitListener.class, listener);
    }
    
    

    @Override
	public void removeQuitListener(QuitListener listener) {
        this.eventProvider.removeListener(QuitListener.class, listener);
    }
    
    

    @Override
	public void addMessageListener(MessageListener listener) {
        this.eventProvider.addListener(MessageListener.class, listener);
    }
    
    

    @Override
	public void removeMessageListener(MessageListener listener) {
        this.eventProvider.removeListener(MessageListener.class, listener);
    }
    
    
    
    @Override
    public void addChannelModeListener(ChannelModeListener listener) {
        this.eventProvider.addListener(ChannelModeListener.class, listener);
    }
    
    
    
    @Override
    public void removeChannelModeListener(ChannelModeListener listener) {
        this.eventProvider.removeListener(ChannelModeListener.class, listener);
    }
    
    
    
    @Override
    public void addUserSpottedListener(UserSpottedListener listener) {
        this.eventProvider.addListener(UserSpottedListener.class, listener);
    }
    
    
    
    @Override
    public void removeUserSpottedListener(UserSpottedListener listener) {
        this.eventProvider.removeListener(UserSpottedListener.class, listener);
    }
    
    
    @Override
    public void addConnectionListener(ConnectionListener listener) {
        this.eventProvider.addListener(ConnectionListener.class, listener);
    }
    
    
    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        this.eventProvider.removeListener(ConnectionListener.class, listener);
    }
    
    
    
    protected void fireMessageSend(final OwnMessageEvent e) {
        final List<MessageSendListener> listeners = 
            this.eventProvider.getListeners(MessageSendListener.class);
    
        Dispatchable<MessageSendListener, OwnMessageEvent> d = 
            new Dispatchable<MessageSendListener, OwnMessageEvent>(listeners, e) {
                @Override
                public void dispatch(MessageSendListener listener, OwnMessageEvent event) {
                    listener.messageSent(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
   
    
    
    protected void fireNickChange(final NickChangeEvent e) {
        final List<NickChangeListener> listeners = 
            this.eventProvider.getListeners(NickChangeListener.class);
    
        Dispatchable<NickChangeListener, NickChangeEvent> d = 
            new Dispatchable<NickChangeListener, NickChangeEvent>(listeners, e) {
                @Override
                public void dispatch(NickChangeListener listener, NickChangeEvent event) {
                    listener.nickChanged(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireJoin(final ChannelEvent e) {
        final List<JoinPartListener> listeners = 
            this.eventProvider.getListeners(JoinPartListener.class);
        
        Dispatchable<JoinPartListener, ChannelEvent> d = 
            new Dispatchable<JoinPartListener, ChannelEvent>(listeners, e) {
                @Override
                public void dispatch(JoinPartListener listener, ChannelEvent event) {
                    listener.channelJoined(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void firePart(final ChannelEvent e) {
        final List<JoinPartListener> listeners = 
            this.eventProvider.getListeners(JoinPartListener.class);
    
        Dispatchable<JoinPartListener, ChannelEvent> d = 
            new Dispatchable<JoinPartListener, ChannelEvent>(listeners, e) {
                @Override
                public void dispatch(JoinPartListener listener, ChannelEvent event) {
                    listener.channelParted(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireQuit(final QuitEvent e) {
        final List<QuitListener> listeners = 
            this.eventProvider.getListeners(QuitListener.class);
        
        Dispatchable<QuitListener, QuitEvent> d = 
            new Dispatchable<QuitListener, QuitEvent>(listeners, e) {
                @Override
                public void dispatch(QuitListener listener, QuitEvent event) {
                    listener.quited(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void firePublicMessageEvent(final MessageEvent e) {
        final List<MessageListener> listeners = 
            this.eventProvider.getListeners(MessageListener.class);

        Dispatchable<MessageListener, MessageEvent> d = 
            new Dispatchable<MessageListener, MessageEvent>(listeners, e) {
                @Override
                public void dispatch(MessageListener listener, MessageEvent event) {
                    listener.publicMessage(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void firePrivateMessageEvent(final MessageEvent e) {
        final List<MessageListener> listeners = 
            this.eventProvider.getListeners(MessageListener.class);
        
        Dispatchable<MessageListener, MessageEvent> d = 
            new Dispatchable<MessageListener, MessageEvent>(listeners, e) {
                @Override
                public void dispatch(MessageListener listener, MessageEvent event) {
                    listener.privateMessage(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireActionMessageEvent(final MessageEvent e) {
        final List<MessageListener> listeners = 
            this.eventProvider.getListeners(MessageListener.class);
        
        Dispatchable<MessageListener, MessageEvent> d = 
            new Dispatchable<MessageListener, MessageEvent>(listeners, e) {
                @Override
                public void dispatch(MessageListener listener, MessageEvent event) {
                    listener.actionMessage(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireNoticeMessageEvent(final MessageEvent e) {
        final List<MessageListener> listeners = 
            this.eventProvider.getListeners(MessageListener.class);
        
        Dispatchable<MessageListener, MessageEvent> d = 
            new Dispatchable<MessageListener, MessageEvent>(listeners, e) {
                @Override
                public void dispatch(MessageListener listener, MessageEvent event) {
                    listener.noticeMessage(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireChannelModeEvent(final ChannelModeEvent e) {
        final List<ChannelModeListener> listeners = 
            this.eventProvider.getListeners(ChannelModeListener.class);
        
        Dispatchable<ChannelModeListener, ChannelModeEvent> d = 
            new Dispatchable<ChannelModeListener, ChannelModeEvent>(listeners, e) {
                @Override
                public void dispatch(ChannelModeListener listener, ChannelModeEvent event) {
                    listener.channelModeChanged(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireUserSpotted(final SpotEvent e) {
        final List<UserSpottedListener> listeners = 
            this.eventProvider.getListeners(UserSpottedListener.class);
        
        Dispatchable<UserSpottedListener, SpotEvent> d = 
            new Dispatchable<UserSpottedListener, SpotEvent>(listeners, e) {
                @Override
                public void dispatch(UserSpottedListener listener, SpotEvent event) {
                    listener.userSpotted(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireUserLost(final SpotEvent e) {
        final List<UserSpottedListener> listeners = 
            this.eventProvider.getListeners(UserSpottedListener.class);
        
        Dispatchable<UserSpottedListener, SpotEvent> d = 
            new Dispatchable<UserSpottedListener, SpotEvent>(listeners, e) {
                @Override
                public void dispatch(UserSpottedListener listener, SpotEvent event) {
                    listener.userLost(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireConnectionEstablished(ConnectionEvent e) {
        final List<ConnectionListener> listeners = 
            this.eventProvider.getListeners(ConnectionListener.class);
        
        Dispatchable<ConnectionListener, ConnectionEvent> d = 
            new Dispatchable<ConnectionListener, ConnectionEvent>(listeners, e) {
                @Override
                public void dispatch(ConnectionListener listener, ConnectionEvent event) {
                    listener.ircConnectionEstablished(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }
    
    
    
    protected void fireConnectionLost(ConnectionEvent e) {
        final List<ConnectionListener> listeners = 
            this.eventProvider.getListeners(ConnectionListener.class);
        
        Dispatchable<ConnectionListener, ConnectionEvent> d = 
            new Dispatchable<ConnectionListener, ConnectionEvent>(listeners, e) {
                @Override
                public void dispatch(ConnectionListener listener, ConnectionEvent event) {
                    listener.ircConnectionLost(event);
                }
        };
        this.eventProvider.dispatchEvent(d);
    }


    @Override
    protected void actualDispose() throws DisposingException {
        if (this.isConnected()) {
            logger.debug("Closing irc connection using default quit message.");
            this.quit();
        } else {
            logger.debug("Irc connection already closed.");
        }
        logger.trace("Shutting down message scheduler");
        this.messageScheduler.dispose();
        logger.trace("Shutting down irc bot.");
        this.bot.dispose();
    }
}
