package polly.rx.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Types;
import de.skuzzle.polly.sdk.exceptions.InsufficientRightsException;
import de.skuzzle.polly.sdk.http.HttpAction;
import de.skuzzle.polly.sdk.http.HttpEvent;
import de.skuzzle.polly.sdk.http.HttpTemplateContext;
import de.skuzzle.polly.sdk.http.HttpTemplateException;


public class ResourcePriceGrabber extends HttpAction {

    private final static String API_URL = "http://qzone.servebbs.net/pollyPriceDaily.php";

    
    private String ts;
    private Date lastRefreshDate;
    private Map<String, Types> prices;
    private int refreshCounter;
    private final int refreshThreshold;
    
    
    
    public ResourcePriceGrabber(int refreshThreshold, MyPolly myPolly) {
        super("/priceDaily", myPolly);
        this.refreshThreshold = refreshThreshold;
        this.refreshCounter = refreshThreshold;
    }
    
    
    
    @Override
    public HttpTemplateContext execute(HttpEvent e) throws HttpTemplateException,
            InsufficientRightsException {
        
        
        return null;
    }
    
    
    
    public synchronized Map<String, Types> getPrices() {
        if (this.refreshCounter++ % this.refreshThreshold == 0) {
            try {
                this.refresh();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.prices;
    }
    
    
    
    public synchronized Date getlastRefreshDate() {
        return this.lastRefreshDate;
    }
    
    
    
    private void refresh() throws IOException {
        final URL url = new URL(API_URL);
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            int i = 0;
            final Map<String, Types> result = new LinkedHashMap<String, Types>();
            while ((line = r.readLine()) != null) {
                ++i;
                switch (i) {
                case 1: break;
                case 2: break;
                case 3: break;
                case 4:
                    if (line.equals(this.ts)) {
                        result.putAll(this.prices);
                        return;
                    }
                    this.ts = line;
                    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    this.lastRefreshDate = df.parse(line.split(";")[1]);
                case 5: break;
                default:
                    final String[] parts = line.split(";");
                    result.put(parts[0], new Types.NumberType(
                        Double.parseDouble(parts[1])));
                    break;
                }
            }
            this.prices = result;
            
        } catch (ParseException e) {
            throw new IOException(e);
        } finally {
            if (r != null) {
                r.close();
            }
        }
    }
}