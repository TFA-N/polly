package polly.annoyingPeople;

import de.skuzzle.polly.sdk.resources.Constants;
import de.skuzzle.polly.sdk.resources.Resources;


public class MSG extends Constants {
    
    private final static String FAMILY = "polly.annoyingPeople.Translation"; //$NON-NLS-1$

    public static String aktienNames;
    public static String ressNames;
    public static String quadNames; 
    public static String askForRessPrice;
    public static String askForCourse;
    public static String askIfDown;
    public static String askForCode;
    public static String askForDirection;
    public static String askForKonstru;
    
    public static String addPersonHelp;
    public static String addPersonSig0Desc;
    public static String addPersongSig0Name;
    public static String addPersonSig0Channel;
    public static String addPersonSuccess;

    public static String removePersonSig0Desc;
    public static String removePersongSig0Name;
    public static String removePersonSig0Channel;
    public static String removePersonHelp;
    public static String removePersonSuccess;
    
    
    static {
        Resources.init(FAMILY, MSG.class);
    }
}
