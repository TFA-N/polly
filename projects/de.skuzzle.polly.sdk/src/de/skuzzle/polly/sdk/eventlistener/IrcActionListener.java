package de.skuzzle.polly.sdk.eventlistener;

import java.util.EventListener;

import de.skuzzle.polly.tools.events.Dispatch;

/**
 * This listener gets notified when a user has been spotted for the first time or when
 * a user left pollys sight.
 * 
 * @author Simon
 * @since 0.6.0
 */
public interface IrcActionListener extends EventListener {
    
    
    public final static Dispatch<IrcActionListener, ChannelEvent> USER_SPOTTED = 
            new Dispatch<IrcActionListener, ChannelEvent>() {
        @Override
        public void dispatch(IrcActionListener listener, ChannelEvent event) {
            listener.userSpotted(event);
        }
    };
    
    
    public final static Dispatch<IrcActionListener, IrcEvent> USER_LOST = 
            new Dispatch<IrcActionListener, IrcEvent>() {
        @Override
        public void dispatch(IrcActionListener listener, IrcEvent event) {
            listener.userLost(event);
        }
    };
    
    
    
    /**
     * This event is fired when a user comes into pollys sight the first time since
     * he left the sight.
     * 
     * @param e Contains detailed information about the event.
     */
    public abstract void userSpotted(ChannelEvent e);
    
    
    
    /**
     * This event is fired when a user left pollys sight. E.g. he quits or parts the last
     * channel he had in common with polly.
     * 
     * @param e Contains detailed information about the event.
     */
    public abstract void userLost(IrcEvent e);
}