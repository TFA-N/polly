package polly.rx.core.orion.model;


public enum LoadRequired {
    NONE, PARTIAL, FULL;
    
    public static LoadRequired parse(String s) {
        if (s.equalsIgnoreCase("nein") || s.equalsIgnoreCase("keine")) { //$NON-NLS-1$ //$NON-NLS-2$
            return NONE;
        } else if (s.equalsIgnoreCase("ja")) { //$NON-NLS-1$
            return FULL;
        } else if (s.equalsIgnoreCase("partiell")) { //$NON-NLS-1$
            return PARTIAL;
        }
        throw new IllegalArgumentException(s);
    }
}
