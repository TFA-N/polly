package de.skuzzle.polly.tools.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>{@link InputStream} implementation that reads from a byte array. It differs from the
 * implementation provided by the java SDK in that this stream is not synchronized and
 * thus spares out some runtime overhead.</p>
 * 
 * <p>None of the IO methods in this stream will throw an {@link IOException}.</p>
 * 
 * @author Simon Taddiken
 */
public class FastByteArrayInputStream extends InputStream {

    protected final byte[] buffer;
    protected final int size;
    protected int pos;
    private int mark;
    
    
    
    /**
     * Creates a new FastByteArrayInputStream that reads its data from the provided
     * {@link FastByteArrayOutputStream}. Note that no data is copied and this stream will
     * use the same buffer as the provided output stream.
     * 
     * @param out The Stream to read from.
     */
    public FastByteArrayInputStream(FastByteArrayOutputStream out) {
        this(out.getBuffer(), out.getBufferSize());
    }
    
    
    
    /**
     * Creates a new FastByteArrayInputStream that reads from the given byte array.
     * @param buffer The array to read bytes from.
     */
    public FastByteArrayInputStream(byte[] buffer) {
        this(buffer, buffer.length);
    }
    
    
    
    /**
     * Creates a new FastByteArrayInputStream that reads from the given byte array.
     *
     * @param buffer The array to read bytes from.
     * @param size Upper bound to which bytes are read.
     */
    public FastByteArrayInputStream(byte[] buffer, int size) {
        this.size = size;
        this.buffer = buffer;
        this.pos = 0;
        this.mark = -1;
    }
    
    
    
    @Override
    public int read() {
        return this.pos < this.size ? this.buffer[this.pos++] : -1;
    }
    
    
    
    @Override
    public int read(byte[] b, int off, int len) {
        if (this.pos >= this.size) {
            return -1;
        }
        if (this.pos + len > this.size) {
            len = this.size - this.pos;
        }
        System.arraycopy(this.buffer, this.pos, b, off, len);
        this.pos += len;
        return len;
    }
    
    
    
    @Override
    public long skip(long n) {
        if (this.pos + n > this.size) {
            n = this.size - this.pos;
        }
        if (n < 0) {
            return 0;
        }
        this.pos += n;
        return n;
    }
    
    
    
    @Override
    public boolean markSupported() {
        return true;
    }
    
    
    
    @Override
    public void mark(int readlimit) {
        this.mark = this.pos;
    }
    
    
    
    @Override
    public void reset() throws IOException {
        if (this.mark == -1) {
            throw new IOException("mark not set");
        }
        this.pos = this.mark;
    }
    
    
    
    @Override
    public void close()  {}
    
    
    
    @Override
    public String toString() {
        return "FastByteArrayInput, size: " + this.size;
    }
}