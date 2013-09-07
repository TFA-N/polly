package de.skuzzle.polly.core.internal.httpv2;

import java.io.IOException;
import java.util.Arrays;

import de.skuzzle.polly.core.configuration.ConfigurationProviderImpl;
import de.skuzzle.polly.core.internal.ModuleStates;
import de.skuzzle.polly.core.internal.ShutdownManagerImpl;
import de.skuzzle.polly.core.internal.mypolly.MyPollyImpl;
import de.skuzzle.polly.core.moduleloader.AbstractProvider;
import de.skuzzle.polly.core.moduleloader.ModuleLoader;
import de.skuzzle.polly.core.moduleloader.SetupException;
import de.skuzzle.polly.core.moduleloader.annotations.Module;
import de.skuzzle.polly.core.moduleloader.annotations.Provide;
import de.skuzzle.polly.core.moduleloader.annotations.Require;
import de.skuzzle.polly.http.api.AddHandlerListener;
import de.skuzzle.polly.http.api.Controller;
import de.skuzzle.polly.http.api.DefaultServerFactory;
import de.skuzzle.polly.http.api.HttpServer;
import de.skuzzle.polly.http.api.HttpServletServer;
import de.skuzzle.polly.http.api.ServerFactory;
import de.skuzzle.polly.http.api.handler.DirectoryEventHandler;
import de.skuzzle.polly.http.internal.HttpServerCreator;
import de.skuzzle.polly.sdk.Configuration;
import de.skuzzle.polly.sdk.ConfigurationProvider;
import de.skuzzle.polly.sdk.Disposable;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.exceptions.DisposingException;
import de.skuzzle.polly.sdk.httpv2.MenuEntry;
import de.skuzzle.polly.sdk.httpv2.WebinterfaceManager;
import de.skuzzle.polly.sdk.time.Milliseconds;

@Module(
    requires = {
        @Require(component = ConfigurationProviderImpl.class),
        @Require(component = ShutdownManagerImpl.class),
        @Require(state = ModuleStates.PERSISTENCE_READY)
    },
    provides = @Provide(component = HttpManagerV2Impl.class)
)
public class WebinterfaceProvider extends AbstractProvider {
    
    public final static String HTTP_CONFIG = "http.cfg";
    private Configuration serverCfg;
    private HttpServletServer server;
    private HttpManagerV2Impl webinterface;
    
    
    public WebinterfaceProvider(ModuleLoader loader) {
        super("WEBINTERFACE_PROVIDER", loader, false);
    }

    
    
    @Override
    public void setup() throws SetupException {
        ConfigurationProvider configProvider = this.requireNow(
            ConfigurationProviderImpl.class, true);
    
        try {
            this.serverCfg = configProvider.open(HTTP_CONFIG);
        } catch (IOException e) {
            throw new SetupException(e);
        }
        
        int port = 81; //this.serverCfg.readInt(Configuration.HTTP_PORT);
        int sessionTimeout = (int) Milliseconds.fromMinutes(60);//this.serverCfg.readInt(Configuration.HTTP_SESSION_TIMEOUT);
        
        final ServerFactory sf = new DefaultServerFactory(port);
        this.server = HttpServerCreator.createServletServer(sf);
        this.server.setSessionLiveTime(sessionTimeout);
        this.server.setSessionType(HttpServer.SESSION_TYPE_COOKIE);
        this.server.addAnswerHandler(GsonHttpAnswer.class, new GsonHttpAnswerHandler());
        
        ShutdownManagerImpl sm = this.requireNow(ShutdownManagerImpl.class, true);
        sm.addDisposable(new Disposable() {
            
            @Override
            public boolean isDisposed() {
                return false;
            }
            
            
            
            @Override
            public void dispose() throws DisposingException {
                server.shutdown(1000);
            }
        });
        
        this.webinterface = new HttpManagerV2Impl(this.server);

        // Automatically collect all menu entries
        this.server.addAddHandlerListener(new AddHandlerListener() {
            @Override
            public void handlerAdded(Controller c, String url, String name, String[] values) {
                if (values.length < 1 || !values[0].equals(WebinterfaceManager.ADD_MENU_ENTRY)) {
                    return;
                } else if (values.length < 2) {
                    throw new RuntimeException("missing parameters");
                }
                
                final String category = values[1];
                final String description = values[2];
                final String[] permissions;
                if (values.length == 3) {
                    permissions = new String[0];
                } else {
                    permissions = Arrays.copyOfRange(values, 3, values.length);
                }
                webinterface.addMenuEntry(category, 
                    new MenuEntry(name, url, description, permissions));
            }
        });
        this.provideComponent(this.webinterface);
        
    }
    
    
    
    @Override
    public void run() throws Exception {
        final MyPolly myPolly = this.requireNow(MyPollyImpl.class, false);
        
        this.server.addController(new IndexController(myPolly, "webv2", this.webinterface));
        this.server.addController(new SessionController(myPolly, "webv2", this.webinterface));
        this.server.addController(new UserController(myPolly, "webv2", this.webinterface));
        
        this.server.addHttpEventHandler("/files", 
            new DirectoryEventHandler("webv2/files", false));
        
        this.server.start();
    }
}
