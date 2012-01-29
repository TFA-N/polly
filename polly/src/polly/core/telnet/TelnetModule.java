package polly.core.telnet;

import java.io.IOException;
import polly.configuration.PollyConfiguration;
import polly.core.ModuleStates;
import polly.core.ShutdownManagerImpl;
import polly.core.irc.IrcManagerImpl;
import polly.eventhandler.MessageHandler;
import polly.moduleloader.AbstractModule;
import polly.moduleloader.ModuleLoader;
import polly.moduleloader.SetupException;
import polly.moduleloader.annotations.Module;
import polly.moduleloader.annotations.Require;
import polly.moduleloader.annotations.Provide;;

@Module(
    requires = {
        @Require(component = PollyConfiguration.class),
        @Require(component = ShutdownManagerImpl.class),
        @Require(component = IrcManagerImpl.class),
        @Require(component = MessageHandler.class)
    },
    provides = {
        @Provide(component = TelnetServer.class),
        @Provide(state = ModuleStates.TELNET_READY)
    })
public class TelnetModule extends AbstractModule {

    private PollyConfiguration config;
    private IrcManagerImpl ircManager;
    private MessageHandler messageHandler;
    private ShutdownManagerImpl shutdownManager;
    private TelnetServer server;
    
    
    public TelnetModule(ModuleLoader loader) {
        super("TELNET", loader, false);
    }
    

    
    @Override
    public void beforeSetup() {
        this.config = this.requireNow(PollyConfiguration.class);
        this.ircManager = this.requireNow(IrcManagerImpl.class);
        this.messageHandler = this.requireNow(MessageHandler.class);
        this.shutdownManager = this.requireNow(ShutdownManagerImpl.class);
    }
    
    
    
    @Override
    public void setup() throws SetupException {
        if (this.config.enableTelnet()) {
            try {
                this.server = new TelnetServer(
                    this.config, this.ircManager, this.messageHandler);
                
                this.shutdownManager.addDisposable(this.server);
                
                this.provideComponent(this.server);
            } catch (IOException e) {
                throw new SetupException(e);
            } 
        }
    }
    
    


    public void run() throws Exception {
        this.server.start();
        this.addState(ModuleStates.TELNET_READY);
    }

}
