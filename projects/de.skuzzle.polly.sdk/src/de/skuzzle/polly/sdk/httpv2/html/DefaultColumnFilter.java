package de.skuzzle.polly.sdk.httpv2.html;

import java.util.regex.Pattern;


public class DefaultColumnFilter implements HTMLColumnFilter {
    
    
    public final static Acceptor REGEX_ACCEPTOR = new Acceptor() {
        

        @Override
        public boolean accept(Object filter, Object cellValue) {
            final String comp = cellValue == null 
                ? "" : cellValue.toString().toLowerCase(); //$NON-NLS-1$
            final Pattern p = (Pattern) filter;
            return p.matcher(comp).matches();
        }
        
        

        @Override
        public Object parseFilter(String filter) {
            try {
                return Pattern.compile(".*" + filter + ".*",  //$NON-NLS-1$ //$NON-NLS-2$
                    Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                return Pattern.compile(".*"); //$NON-NLS-1$
            }
        }
    }; 
    
    
    
    @Override
    public Acceptor getAcceptor(int column) {
        return REGEX_ACCEPTOR;
    }
}
