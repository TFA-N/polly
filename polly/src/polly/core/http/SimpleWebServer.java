package polly.core.http;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;


import com.sun.net.httpserver.HttpServer;

import de.skuzzle.polly.sdk.http.HttpAction;
import de.skuzzle.polly.sdk.http.HttpEvent;
import de.skuzzle.polly.sdk.http.HttpEventListener;
import de.skuzzle.polly.sdk.http.HttpManager;
import de.skuzzle.polly.sdk.http.HttpSession;
import de.skuzzle.polly.sdk.http.HttpTemplateContext;
import de.skuzzle.polly.sdk.roles.RoleManager;
import de.skuzzle.polly.tools.concurrent.ThreadFactoryBuilder;
import de.skuzzle.polly.tools.events.Dispatchable;
import de.skuzzle.polly.tools.events.EventProvider;
import de.skuzzle.polly.tools.events.SynchronousEventProvider;



public class SimpleWebServer implements HttpManager {
    
    
    private final static Logger logger = Logger.getLogger(SimpleWebServer.class
        .getName());
    
    
    
    
    private HttpServer server;
    private int port;
    private boolean running;
    private Map<InetAddress, HttpSession> sessions;
    private EventProvider eventProvider;
    private Map<String, HttpAction> actions;
    private ArrayList<String> menu;
    private File templateRoot;
    private int sessionTimeOut;
    private RoleManager roleManager;
    
    
    
    public SimpleWebServer(RoleManager roleManager, File templateRoot, 
            int port, int sessionTimeOut) {
        this.roleManager = roleManager;
        this.templateRoot = templateRoot;
        this.port = port;
        this.sessionTimeOut = sessionTimeOut;
        this.sessions = new HashMap<InetAddress, HttpSession>();
        this.eventProvider = new SynchronousEventProvider();
        this.actions = new HashMap<String, HttpAction>();
        this.menu = new ArrayList<String>();
    }
    
    
    
    public File getTemplateRoot() {
        return this.templateRoot;
    }
    
    
    
    public void startServer() throws IOException {
        if (this.isRunning()) {
            return;
        }
        logger.info("Starting webserver at port " + this.port);
        this.server = HttpServer.create(new InetSocketAddress(this.port), 5);
        this.server.createContext("/", new ResponseHandler(this));
        this.server.setExecutor(
            Executors.newCachedThreadPool(
                new ThreadFactoryBuilder("HTTP_SERVER_%n%")));
        this.server.start();
        this.running = true;
        logger.info("Webserver running.");
    }
    
    
    
    protected HttpSession getSession(InetAddress remoteIp) {
        synchronized (this.sessions) {
            HttpSession session = this.sessions.get(remoteIp);
            if (session == null) {
                session = new HttpSession(generateSessionId(remoteIp), remoteIp);
                this.sessions.put(remoteIp, session);
            }
            return session;
        }
    }
    
    
    
    protected void closeSession(HttpSession session) {
        synchronized (this.sessions) {
            logger.warn("Killing " + session);
            this.sessions.remove(session.getRemoteIp());
        }
    }
    
    
    
    private final static Random RANDOM = new Random();
    
    private static String generateSessionId(InetAddress remoteIp) {
        long id = RANDOM.nextLong() * System.currentTimeMillis() * remoteIp.hashCode();
        return Long.toHexString(id);
    }
    
    
    
    public boolean isRunning() {
        return this.running;
    }
    
    
    
    protected HttpTemplateContext executeAction(HttpEvent e) {
        String uri = e.getRequestUri();
        HttpAction action = this.actions.get(uri);
        
        HttpTemplateContext actionContext = null;
        if (action == null) {
            return null;
        } else {
            if (this.roleManager.canAccess(e.getSession().getUser(), action)) {
                actionContext = action.execute(e);
            } else {
                return this.errorTemplate("Permission denied", 
                    "You have insufficient permissions to acces this page/action." +
                    "<br/><br/>Missing permission(s): " + action.getRequiredPermission(), 
                    e.getSession());
            }
        }
        this.putRootContext(actionContext, e.getSession());
        actionContext.put(HttpInterface.CONTENT, 
                this.getPage(actionContext.getTemplate()).getPath());
        
        return actionContext;
    }
    
    
    
    @Override
    public HttpTemplateContext errorTemplate(String errorHeading, 
            String errorDescription, HttpSession session) {
        HttpTemplateContext c = new HttpTemplateContext(HttpInterface.PAGE_ERROR);
        c.put(HttpInterface.ERROR_HEADING, errorHeading);
        c.put(HttpInterface.ERROR_DESCRIPTION, errorDescription);
        c.put(HttpInterface.CONTENT, this.getPage(HttpInterface.PAGE_ERROR).getPath());
        this.putRootContext(c, session);
        return c;
    }
    
    
    
    protected File getPage(String name) {
        return new File(this.templateRoot, name);
    }
    
    
    
    @Override
    public void addHttpAction(HttpAction action) {
        this.actions.put(action.getName(), action);
    }
    
    
    
    protected void putRootContext(HttpTemplateContext c, HttpSession session) {
        c.put("menu", this.menu);
        c.put("title", "Polly Webinterface");
        c.put("heading", "Polly Webinterface");
        c.put("me", session.getUser());
    }


    
    @Override
    public void addMenuUrl(String name) {
        this.menu.add(name);
    }
    
    
    
    @Override
    public void removeMenuUrl(String name) {
        this.menu.remove(name);
    }
    
    
    
    @Override
    public void startWebServer() {
    }



    @Override
    public void stopWebServer() {
    }
    
    
    
    
    protected void fireHttpAction(final HttpEvent e) {
        final List<HttpEventListener> listeners = 
            this.eventProvider.getListeners(HttpEventListener.class);
        
        this.eventProvider.dispatchEvent(new Dispatchable<HttpEventListener, 
                    HttpEvent>(listeners, e) {
            @Override
            public void dispatch(HttpEventListener listener, HttpEvent event) {
                // Check if url matches the url pattern of the listener
                if (event.getRequestUri().matches(listener.getActionUrl())) {
                    listener.httpAction(e);
                }
            }
        });
    }



    @Override
    public void addHttpEventListener(HttpEventListener listener) {
        this.eventProvider.addListener(HttpEventListener.class, listener);
    }

    
    
    @Override
    public void removeHttpEventListener(HttpEventListener listener) {
        this.eventProvider.addListener(HttpEventListener.class, listener);
    }



    public int getSessionTimeOut() {
        return this.sessionTimeOut;
    }
}