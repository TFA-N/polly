package polly.eventhandler;

import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import de.skuzzle.polly.parsing.ParseException;
import de.skuzzle.polly.sdk.CommandManager;
import de.skuzzle.polly.sdk.eventlistener.IrcUser;
import de.skuzzle.polly.sdk.eventlistener.MessageEvent;
import de.skuzzle.polly.sdk.eventlistener.MessageListener;
import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.InsufficientRightsException;
import de.skuzzle.polly.sdk.exceptions.UnknownCommandException;
import de.skuzzle.polly.sdk.exceptions.UnknownSignatureException;
import de.skuzzle.polly.sdk.model.User;


import polly.core.users.UserManagerImpl;




public class MessageHandler implements MessageListener {

	private static Logger logger = Logger.getLogger(MessageHandler.class.getName());
    private CommandManager commands;
    private UserManagerImpl userManager;
    private ExecutorService executorThreadPool;
    
    public MessageHandler(CommandManager commandManager, UserManagerImpl userManager, 
            ExecutorService executorThreadPool) {
        this.commands = commandManager;
        this.userManager = userManager;
        this.executorThreadPool = executorThreadPool;
    }
    

    
    @Override
    public void publicMessage(MessageEvent e) {
        this.execute(e, false);
    }

    
    
    @Override
    public void privateMessage(MessageEvent e) {
        this.execute(e, true);
    }

    
    
    @Override
    public void actionMessage(MessageEvent e) {}
    
    
    
    private void execute(final MessageEvent e, final boolean isQuery) {
        final User executor = this.getUser(e.getUser());

        Runnable command = new Runnable() {
            @Override
            public void run() {
                try {
                    MessageHandler.this.commands.executeString(e.getMessage(), e
                        .getChannel(), isQuery, executor, e.getSource());
                } catch(CommandException e1) {
                    if (e1.getCause() instanceof ParseException) {
                        ParseException e2 = (ParseException) e1.getCause();
                        e.getSource().sendMessage(e.getChannel(), e2.getMessage());
                    } else {
                        e.getSource().sendMessage(e.getChannel(), 
                            "Fehler beim Ausf�hren des Befehls: " + e1.getMessage());
                    }
                    logger.debug("", e1);
                } catch (UnknownCommandException e1) {
                    e.getSource().sendMessage(e.getChannel(), "Unbekannter Befehl: " + 
                            e1.getMessage());
                } catch (UnknownSignatureException e1) {
                    e.getSource().sendMessage(e.getChannel(), "Unbekannte Signatur: " + 
                            e1.getSignature().toString());
                } catch (InsufficientRightsException e1) {
                    e.getSource().sendMessage(e.getChannel(), "Du kannst den Befehl '" + 
                            e1.getCommand().getCommandName() + "' nicht ausf�hren.");
                } catch (Exception e1) {
                    logger.error("Exception while executing command: " + 
                            e1.getMessage(), e1);
                    e.getSource().sendMessage(e.getChannel(), 
                            "Interner Fehler beim Ausf�hren des Befehls.");
                }
            }
        };
        
        this.executorThreadPool.execute(command);
    }
    
    
    
    private User getUser(IrcUser user) {
        User u = this.userManager.getUser(user);
        if (u == null) {
            u = new polly.data.User("~UNKNOWN", "blabla", 0);
            u.setCurrentNickName(user.getNickName());
        }
        return u;
    }



    @Override
    public void noticeMessage(MessageEvent ignore) {}
}
