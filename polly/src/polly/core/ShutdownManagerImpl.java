package polly.core;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import polly.Polly;
import polly.util.events.ProcessExecutor;


import de.skuzzle.polly.sdk.CompositeDisposable;
import de.skuzzle.polly.sdk.Disposable;
import de.skuzzle.polly.sdk.ShutdownManager;
import de.skuzzle.polly.sdk.exceptions.DisposingException;



/**
 * 
 * @author Simon
 * @version 27.07.2011 ae73250
 * @since Beta 0.5
 */
public class ShutdownManagerImpl implements ShutdownManager {

    private static Logger logger = Logger.getLogger(ShutdownManagerImpl.class.getName());

    
    /** 
     * Sun property pointing the main class and its arguments. 
     * Might not be defined on non Hotspot VM implementations.
     */
    public static final String SUN_JAVA_COMMAND = "sun.java.command";
    
    
    public AtomicBoolean isSafeShutdown;
    
    
    
    private static ShutdownManagerImpl instance;
    
    public static synchronized ShutdownManagerImpl get() {
        if (instance == null) {
            instance = new ShutdownManagerImpl();
        }
        return instance;
    }
    
    

    private CompositeDisposable shutdownList;
    
    private ShutdownManagerImpl() {
        this.isSafeShutdown = new AtomicBoolean(false);
        this.shutdownList = new CompositeDisposable();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (!ShutdownManagerImpl.this.isSafeShutdown.get()) {
                    logger.warn("Unexpected shutdown. Trying to shutdown all " +
                    		"resources properly.");
                    ShutdownManagerImpl.this.emergencyShutdown();
                }
            }
        });
    }
    
    
    
    public CompositeDisposable getShutdownList() {
        return this.shutdownList;
    }
    
    
    
    public synchronized void addDisposable(Disposable d) {
        this.shutdownList.addLast(d);
    }
    
    
    
    @Override
    public void restart() {
        this.restart(Polly.getCommandLine());
    }
    
    
    /*
     * Implementation is adapted from 
     * http://java.dzone.com/articles/programmatically-restart-java
     */
    
    @Override
    public synchronized void restart(String commandLine) {
        // properly shutdown all loaded components
        this.shutdown(false);
        logger.info("Preparing to restart...");
        logger.debug("Commandline arguments: " + commandLine);
        
        try {
            String s = System.getProperty("file.separator");
            // java binary
            final String java = System.getProperty("java.home") + s +"bin" + s + "java";
            logger.trace("Java Home resolved to: " + java);
            // vm arguments
            List<String> vmArguments = 
                ManagementFactory.getRuntimeMXBean().getInputArguments();
            final ProcessExecutor pe = new ProcessExecutor();
            pe.setDefaultTerminal(Polly.getConfig().getDefaultTerminal());
            pe.setTerminalArguments(Polly.getConfig().getTerminalArguments());
            
            // HACK: using absolute path fails on windows, so we assume there is a 
            //       correct %PATH% entry for java
            pe.addCommand("java");
            for (String arg : vmArguments) {
                // if it's the agent argument : we ignore it otherwise the
                // address of the old application and the new one will be in conflict
                if (!arg.contains("-agentlib")) {
                    pe.addCommand(arg);
                }
            }
            pe.addCommandsFromString("-jar polly.jar");

            if (commandLine != null && !commandLine.equals("")) {
                pe.addCommandsFromString(commandLine);
            }
            
            logger.trace("Java command: '" + pe.toString() + "'");
            // execute the command in a shutdown hook, to be sure that all the
            // resources have been disposed before restarting the application
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        pe.start(Polly.getConfig().isRunInConsole());
                        logger.trace("Executed: " + pe.toString());
                        logger.info("New polly instance initialized.");
                    } catch (IOException e) {
                        logger.error("Could not start new polly instance.", e);
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Could not start new polly instance", e);
        } finally {
            // All resources have been unloaded, so perform shutdown
            logger.warn("Exiting current polly process.");
            System.exit(0);
        }
    }
    
    
    
    public synchronized void shutdown(boolean exit) {
        this.isSafeShutdown.set(true);
        logger.info("Shutting down all components.");
        try {
            this.shutdownList.dispose();
        } catch (DisposingException e) {
            logger.error("One or more components failed to properly unload. " +
            		"Last exception trace:", e);
        }
        
        logger.trace("Remaining active threads: " + Thread.activeCount());
        logger.trace("Remaining stacktraces:");
        this.printRemainingThreads();
        
        if (exit) {
            logger.info("All connections closed. Now exiting the whole program. ByeBye");
            LogManager.shutdown();
            System.exit(0);
        }
    }
    
    
    
    public void shutdown() {
        this.shutdown(true);
    }
    
    
    
    private void emergencyShutdown() {
        try {
            LogManager.shutdown();
            this.shutdownList.dispose();
        } catch (DisposingException e) {
            e.printStackTrace();
        }
    }
    
    
    
    private void printRemainingThreads() {
        Thread[] active = new Thread[Thread.activeCount()];
        Thread.enumerate(active);
        for (Thread t : active) {
            logger.trace("Thread: " + t.toString());
            for (StackTraceElement e : t.getStackTrace()) {
                logger.trace("    " + e.toString());
            }
        }
    }
}