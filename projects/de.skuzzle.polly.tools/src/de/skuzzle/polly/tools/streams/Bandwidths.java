package de.skuzzle.polly.tools.streams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


public class Bandwidths {
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        // create big test file
        final long bigTestFileSize = megaBytesToBytes(1000);
        final byte[] buffer = new byte[80000];
        final int bytesPerSecond = 5242880;
        Arrays.fill(buffer, (byte) 65);
        
        try (OutputStream out = new FileOutputStream(new File("C:\\Users\\Simon\\Desktop\\temp.txt"))) {
            final BandwidthOutputStream bandOut = new BandwidthOutputStream(out, bytesPerSecond);
            
            int written = 0;
            while (written < bigTestFileSize) {
                bandOut.write(buffer, 0, buffer.length);
                written += buffer.length;
                double kbs = bandOut.getSpeed();
                System.out.println("Written: " + written + " bytes, Speed: " + kbs + " kb/s");
            }
            bandOut.close();
        }
    }

    private final static int KILO_BYTE = 1 << 10;
    
    private final static int MEGA_BYTE = 1 << 20;
    
    
    
    public static long kiloBytesToBytes(int kb) {
        return kb * KILO_BYTE;
    }
    
    
    
    public static long kiloBitsToBytes(int kb) {
        return (kb >> 3) * KILO_BYTE;
    }
    
    
    
    public static long megaBytesToBytes(int mb) {
        return mb * MEGA_BYTE;
    }
    
    
    
    public static long megaBitsToBytes(int mb) {
        return (mb >> 3) * MEGA_BYTE;
    }
}
