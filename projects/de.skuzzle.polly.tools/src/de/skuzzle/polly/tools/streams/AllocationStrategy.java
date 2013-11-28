package de.skuzzle.polly.tools.streams;

import java.io.Closeable;



public interface AllocationStrategy extends Closeable {

    /**
     * Gets the speed in bytes per seconds at which this allocator currently allocates.
     * 
     * @return The current allocation speed in bytes per second
     */
    public double getSpeed();
    
    /**
     * Tries to allocate the given number of bytes. This method does not actually allocate
     * any memory, but figures out how many memory there is available to the caller. 
     * The result must always be in the interval of <tt>[0, bytes]</tt>. Using the 
     * <tt>source</tt> parameter, implementors are able to distinguish different callers 
     * and my assign different priorities.
     * 
     * <p>This method may or may not block if no bytes are available. Implementors should
     * document the behavior regarding this issue.</p>
     * 
     * @param source The caller object
     * @param bytes The number of bytes to allocate.
     * @return A number between <tt>0</tt> and <tt>bytes</tt>, both inclusive indices.
     */
    public int allocate(Object source, int bytes);
}
