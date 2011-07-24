package commands;

import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.Types.StringType;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;

public class DictCommand extends SearchEngineCommand {

    public DictCommand(MyPolly polly) throws DuplicatedSignatureException {
        super(polly, "dict");
        this.createSignature("Gibt einen Dict.cc-Link zur�ck.", new StringType());
    }

    
    
    @Override
    protected String getSearchLink(String key) {
        return "http://www.dict.cc/?s=" + key;
    }
}
