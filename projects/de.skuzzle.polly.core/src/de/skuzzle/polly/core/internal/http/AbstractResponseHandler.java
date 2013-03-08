package de.skuzzle.polly.core.internal.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.skuzzle.polly.core.util.InputStreamCounter;
import de.skuzzle.polly.sdk.http.Cookie;
import de.skuzzle.polly.sdk.http.HttpParameter;
import de.skuzzle.polly.sdk.http.HttpSession;
import de.skuzzle.polly.sdk.http.HttpParameter.ParameterType;
import de.skuzzle.polly.sdk.time.Milliseconds;
import de.skuzzle.polly.sdk.time.Time;


public abstract class AbstractResponseHandler implements HttpHandler {

    private final static Logger logger = Logger
        .getLogger(AbstractResponseHandler.class.getName());
    
    
    /**
     * Regex for splitting GET style parameters from the request uri
     */
    private final static Pattern GET_PARAMETERS = Pattern.compile(
        "(\\w+)=([^&]+)");
    
    
    
    protected void parseParameters(String in, Map<String, HttpParameter> params, 
            ParameterType type) {
        Matcher m = GET_PARAMETERS.matcher(in);
        
        while (m.find()) {
            String key = in.substring(m.start(1), m.end(1));
            String value = in.substring(m.start(2), m.end(2));
            try {
                value = URLDecoder.decode(value, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (params.containsKey(key)) {
                HttpParameter current = params.get(key);
                params.put(key, new HttpParameter(
                        key, current.getValue() + ";" + value, type));
            } else {
                params.put(key, new HttpParameter(key, value, type));
            }
        }
    }



    protected void parsePostParameters(HttpExchange t, Map<String, HttpParameter> result, 
            HttpSession session) throws IOException {
        InputStreamCounter c = new InputStreamCounter(t.getRequestBody());
        BufferedReader r = null;
        try {
            r = new BufferedReader(
                new InputStreamReader(c, this.webServer.getEncoding()));
            String line = null;
            while ((line = r.readLine()) != null) {
                if (!line.equals("")) {
                    parseParameters(line, result, ParameterType.POST);
                }
            }
        } finally {
            if (r != null) {
                r.close();
            }
        }
        session.updateDownload(c.getBytes());
        this.counter.updateDownload(c.getBytes());
    }
    
    
    
    protected Map<String, String> parseCookies(HttpExchange t) {
        List<String> cookies = t.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return Collections.emptyMap();
        }
        final Map<String, String> result = new HashMap<String, String>();
        for (final String cookie : cookies) {
            final String[] s = cookie.split("=");
            if (s.length != 2) {
                logger.warn("Errornous cookie: " + cookie);
                continue;
            }
            result.put(s[0], s[1]);
        }
        return result;
    }
    
    
    
    protected HttpManagerImpl webServer;
    protected TrafficCounter counter;
    
    
    
    public AbstractResponseHandler(HttpManagerImpl webServer, TrafficCounter counter) {
        this.webServer = webServer;
        this.counter = counter;
    }
    

    
    @Override
    public final void handle(HttpExchange t) throws IOException {
        this.webServer.cleanUpSessions();
        final Map<String, String> cookies = this.parseCookies(t);
        
        String sessionId = cookies.get("sessionid");
        HttpSession session = null;
        
        // if user occurred the first time, he gets assigned a session id. If he is 
        // already known, we catch up on his existing id.
        if (sessionId == null) {
            session = this.webServer.newSession(t.getRemoteAddress().getAddress());
            t.getResponseHeaders().add("Set-Cookie", 
                new Cookie("sessionid", session.getId(), 
                    Milliseconds.toSeconds(this.webServer.getSessionTimeOut())).toString());
        } else {
            session = this.webServer.findSession(sessionId);
            
            // CONSIDER: delete old cookie and create new one
            if (session == null) {
                session = this.webServer.newSession(t.getRemoteAddress().getAddress(), 
                    sessionId);
            }
        }
        session.setCookies(cookies);
        
        long now = Time.currentTimeMillis();
        session.setLastAction(now);
        String uri = t.getRequestURI().toString();
        session.setLastUri(uri);
        
        logger.trace(session + " requested " + uri);
        
        boolean timedOut = session.isLoggedIn() && session.isTimedOut(
            this.webServer.getSessionTimeOut());
        
        boolean blocked = session.shouldBlock(this.webServer.getErrorThreshold());
        
        this.handleRequest(uri, session, t, timedOut, blocked);
        
        if (timedOut) {
            this.webServer.closeSession(session);
        }
    }
    
    
    
    protected abstract void handleRequest(String requestUri, HttpSession session, 
        HttpExchange t, boolean timedOut, boolean blocked) throws IOException;
}