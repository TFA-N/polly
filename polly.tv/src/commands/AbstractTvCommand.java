package commands;

import java.util.List;

import polly.tv.MyPlugin;

import core.TVProgram;
import core.TVProgramProvider;

import de.skuzzle.polly.sdk.Command;
import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;


public class AbstractTvCommand extends Command {


    protected TVProgramProvider tvProvider;
    
    public AbstractTvCommand(MyPolly polly, TVProgramProvider tvProvider, String commandName) throws DuplicatedSignatureException {
        super(polly, commandName);
        this.tvProvider = tvProvider;
    }

    
    
    protected void replyPrograms(String replyTo, List<TVProgram> programs) {
        for (TVProgram program : programs) {
            if (program == null) {
                this.reply(replyTo, "Unbekannter Sender.");
            } else {
                this.reply(replyTo, MyPlugin.DEFAULT_FORMAT.format(program, 
                    this.getMyPolly().formatting()));
            }
        }
    }
}
