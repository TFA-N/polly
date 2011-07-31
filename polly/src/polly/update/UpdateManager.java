package polly.update;


import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import polly.update.DownloadManager.DownloadObject;
import polly.update.DownloadManager.DownloadCallback;


/**
 * 
 * @author Simon
 * @version 27.07.2011 ae73250
 */
public class UpdateManager {

    public static void main(String[] args) {

    }
    
    private static Logger logger = Logger.getLogger(UpdateManager.class.getName());

    
    
    public UpdateManager() {
    }
    

    
    public List<UpdateProperties> collect(List<UpdateItem> items) {
        List<UpdateProperties> result = new LinkedList<UpdateProperties>();
        
        logger.info("Collecting update information for " + items.size() + " items.");
        for (UpdateItem item : items) {
            try {
                UpdateProperties up = new UpdateProperties(item.getPropertyUrl());
                if (up.getVersion().compareTo(item.getCurrentVersion()) > 0) {
                    logger.info(item + " is out of date. Your version: " + 
                            item.getCurrentVersion() + ", newest version: " + 
                            up.getVersion());
                    result.add(up);
                } else {
                    logger.info(item + " is up to date.");
                }
            } catch (IOException e) {
                logger.warn("Could not read update information for item: " + item, e);
            } catch (UpdateException e) {
                logger.error("Invalid update property file for item: " + item, e);
            }
        }
        
        return result;
    }
    
    
    
    public List<File> downloadUpdates(List<UpdateProperties> updates) {
        final List<File> files = new LinkedList<File>();
        DownloadManager dm = new DownloadManager();
        
        logger.info("Downloading " + updates.size() + " updates.");
        for (final UpdateProperties update : updates) {
            try {
                File dest = File.createTempFile("" + System.nanoTime(), ".zip");
                
                dm.downloadAndWait(update.getUpdateUrl(), dest, new DownloadCallback() {
                    
                    @Override
                    public void downloadFinished(DownloadObject o) {
                        logger.info("Downloaded " + o);
                        files.add(o.getDestination());
                    }
                    
                    
                    
                    @Override
                    public void downloadFailed(DownloadObject o, Exception e) {
                        logger.error("Error while downloading " + o, e);
                    }
                });
                
            } catch (IOException e) {
                logger.error("Could not generate temp file for " + update, e);
            }
        }
        
        return files;
    }
    
    
    
    public void install(List<File> files) {
        
    }
}