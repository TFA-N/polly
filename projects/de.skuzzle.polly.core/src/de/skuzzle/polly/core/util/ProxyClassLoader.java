package de.skuzzle.polly.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;


public class ProxyClassLoader extends ClassLoader {

    private final static Logger logger = Logger.getLogger(ProxyClassLoader.class
            .getName());
    
    private final List<PluginClassLoader> children;
    
    
    
    public ProxyClassLoader() {
        this(ClassLoader.getSystemClassLoader());
    }



    public ProxyClassLoader(ClassLoader parent) {
        super(parent);
        if (!registerAsParallelCapable()) {
            logger.error("Failed to register ClassLoader as parallel capable");
        }
        this.children = new LinkedList<PluginClassLoader>();
    }
    
    
    
    // this method is adapted from jdtsoft.com's JarClassLoader
    public void invokeMain(String sClass, String[] args) throws Throwable {
        // The default is sun.misc.Launcher$AppClassLoader (run from file system or JAR)
        Thread.currentThread().setContextClassLoader(this);
        Class<?> clazz = loadClass(sClass);
        Method method = clazz.getMethod("main", new Class<?>[] { String[].class });
        
        boolean bValidModifiers = false;
        boolean bValidVoid = false;
        
        if (method != null) {
            method.setAccessible(true); // Disable IllegalAccessException
            int nModifiers = method.getModifiers(); // main() must be "public static"
            bValidModifiers = Modifier.isPublic(nModifiers) && 
                              Modifier.isStatic(nModifiers);
            Class<?> clazzRet = method.getReturnType(); // main() must be "void"
            bValidVoid = (clazzRet == void.class); 
        }
        if (method == null  ||  !bValidModifiers  ||  !bValidVoid) {
            throw new NoSuchMethodException(
                    "The main() method in class \"" + sClass + "\" not found.");
        }
        
        // Invoke method.
        // Crazy cast "(Object)args" because param is: "Object... args"
        try {
            method.invoke(null, (Object)args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
    
    
    
    
    public void addLoader(PluginClassLoader child) {
        synchronized (this.children) {
            this.children.add(child);
        }
    }
    
    
    
    public void removeLoader(PluginClassLoader child) {
        synchronized (this.children) {
            this.children.remove(child);
        }
    }
    
    
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final List<PluginClassLoader> children;
        synchronized (this.children) {
            // copy children to avoid further synchronization
            children = new ArrayList<>(this.children);
        }
        
        for (PluginClassLoader cl : children) {
            try {
                return cl.findClass(name);
            } catch (ClassNotFoundException ignore){};
        }
        throw new ClassNotFoundException(name);
    }
    
    
    
    @Override
    protected URL findResource(String name) {
        final List<PluginClassLoader> children;
        synchronized (this.children) {
            // copy children to avoid further synchronization
            children = new ArrayList<>(this.children);
        }
        
        for (PluginClassLoader cl : children) {
            final URL result = cl.findResource(name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    
    
    @Override
    public String toString() {
        return this.getClass().getName();
    }
}