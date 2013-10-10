package de.skuzzle.polly.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.skuzzle.polly.sdk.exceptions.CommandException;
import de.skuzzle.polly.sdk.exceptions.ConversationException;
import de.skuzzle.polly.sdk.exceptions.DisposingException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.InsufficientRightsException;
import de.skuzzle.polly.sdk.roles.RoleManager;
import de.skuzzle.polly.sdk.roles.SecurityContainer;
import de.skuzzle.polly.sdk.roles.SecurityObject;



/**
 * <p>This is the base class for all Commands that can be executed by polly. A command
 * needs to have a name and can have an additional info text which describes how it 
 * is used.</p>
 * 
 * <p>A command has a reference to all of its formal signatures. Add new formal signatures
 * for a command via {@link #createSignature(String, Parameter...)} or 
 * {@link #createSignature(String, String, Parameter...)}. Upon execution of a command, 
 * the actual signature is passed. Please see {@link Signature} to find out how signatures
 * are actually being matched.</p>
 * 
 * This is an example of creating an own command:
 * <pre>
 * public class MyCommand extends Command {
 *     public MyCommand(MyPolly polly) throws DuplicatedSignatureException {
 *         super(polly, "mycmd");    // create command with name 'mycmd'
 *         // Create signature with a short help text, a String and a Number Parameter
 *         this.createSignature("Do something", 
 *             new Parameter("ParamName", Types.STRING), 
 *             new Parameter("ParamName2", Types.NUMBER));
 *     }
 *     
 *     <code>@Override</code>
 *     public void executeOnChannel(User executer, String channel, Signature signature) {
 *         if (signature.getId() == 0) {
 *             String string = signature.getStringValue(0);
 *             double number = signature.getNumberValue(1);
 *             
 *             // work with those values
 *         }
 *     }
 *     
 *     <code>@Override</code>
 *     public void executeOnQuery(User executer, Signature signature) {
 *         // do nothing. our command can only be executed in a channel
 *     }
 * </pre>
 * 
 * Now that you have this command, you need to register it with polly:
 * 
 * <pre>
 * Command myCmd = new MyCommand(myPolly);
 * myPolly.commands().registerCommand(myCmd);
 * </pre>
 * 
 * <p>Or you may use the {@link PollyPlugin#addCommand(Command)} method to register 
 * commands.</p>
 * 
 * <p>If you want your Command to have the same behavior whether its executed on a channel 
 * or on a query, you may override {@link #executeOnBoth(User, String, Signature)} and
 * make it return <code>false</code>. Note that it returns <code>false</code> by default.
 * That means if you want your command to be only executable on a query, you need to
 * override {@link #executeOnBoth(User, String, Signature)} and make it return 
 * <code>true</code>. Now, both {@link #executeOnChannel(User, String, Signature)} and
 * {@link #executeOnQuery(User, Signature)} are executed, depending on where the 
 * command has been called.</p>
 * 
 * <p>It is essential for the usability of polly, that you set a proper help text using
 * {@link #setHelpText(String)}.</p>
 * 
 * <p>Commands are subject to pollys role based security system. Thus a command reports
 * its required permissions using {@link #getRequiredPermission()}. The default 
 * implementation returns a Set containing only one element. If this command is set to be
 * executable by only registered users, the set will contain the permission 
 * {@link RoleManager#REGISTERED_PERMISSION}, otherwise it will contain 
 * {@link RoleManager#NONE_PERMISSION} and will thus be executable by everyone.</p>
 * 
 * <p>Additionally, each formal signature can as well have a set of permissions that are
 * required to execute it. So a user must not only have the required permissions for this 
 * command but also the required permissions for the signature he wants to execute.</p>
 * 
 * @author Simon
 * @since zero day
 * @version RC 1.0
 */
public abstract class Command extends AbstractDisposable implements Comparable<Command>, 
        SecurityContainer, SecurityObject {
    
    
	/**
	 * This commands name.
	 */
	protected String commandName;
	
	/**
	 * The {@link MyPolly} instance.
	 */
	protected MyPolly polly;
	
	
	/**
	 * All formal signatures for this command.
	 */
	private List<FormalSignature> signatures;
	
	
	/**
	 * Determines whether unregistered users may execute this command.
	 */
	protected boolean registeredOnly;
	
	
	/**
	 * The userlevel for this command.
	 */
	protected int userLevel;
	
	/**
	 * <code>true</code> if ths command can only be executed in qry.
	 * @since 0.7
	 */
	protected boolean qryCommand;
	
	/**
	 * This commands help message.
	 */
	protected String helpText;
	
	/**
	 * Constants that will be used for the next execution of this command.
	 */
	protected Map<String, Types> constants;
	
	/**
	 * Formal signature to output help text.
	 */
	private FormalSignature helpSignature0;
	
	/**
	 * Formal signature to output help text of certain real signature
	 */
	private FormalSignature helpSignature1;
	
	/**
	 * A set that contains the permissions of all signatures for this command. 
	 */
	private Set<String> containedPermissions;

	
	
	/**
	 * Creates a new Command with the given MyPolly instance and command name.
	 * @param polly The MyPolly instance.
	 * @param commandName The command name.
	 */
	public Command(MyPolly polly, String commandName) {
		this.commandName = commandName;
		this.polly = polly;
		this.signatures = new ArrayList<FormalSignature>();
		this.helpText = Messages.bind(Messages.commandNoDescription, commandName);
		this.constants = new HashMap<String, Types>();
		this.helpSignature0 = new FormalSignature(commandName, 0, "",  //$NON-NLS-1$
		    new Parameter("", Types.HELP)); //$NON-NLS-1$
		this.helpSignature1 = new FormalSignature(commandName, 0, "",  //$NON-NLS-1$
		    new Parameter("", Types.HELP),  //$NON-NLS-1$
		    new Parameter("", Types.NUMBER)); //$NON-NLS-1$
		this.containedPermissions = new HashSet<String>();
	}
	
	
	
	/**
	 * Returns the MyPolly instance this command was initialized with.
	 * @return The MyPolly instance.
	 */
	public final MyPolly getMyPolly() {
		return this.polly;
	}
	
	
	
	/**
	 * Returns this commands name.
	 * @return the command name.
	 */
	public final String getCommandName() {
		return this.commandName;
	}
	
	
	
	/**
	 * <p>Returns the help text for this command. The default implementation returns a
	 * suitable default string. Override it to provide your own help message.</p>
	 * 
	 * <p>Your help messages should contain the numbers of all possible formal parameter 
	 * ids, so that the user can retrieve infos for each signature of your command.</p>
	 * 
	 * @return A help text for this command.
	 */
	public String getHelpText() {
		String help = this.helpText.endsWith(".")  //$NON-NLS-1$
		    ? this.helpText + " " : this.helpText + ". "; //$NON-NLS-1$ //$NON-NLS-2$
		help += Messages.bind(Messages.commandSignatures, this.signatures.size(), 
		        this.commandName);
		return help;
	}
	
	
	
	/**
	 * Sets the help text which can be displayed using the help command.
	 * 
	 * @param helpText The help text for this command.
	 */
	public void setHelpText(String helpText) {
	    this.helpText = helpText;
	}
	
	
	
	/**
	 * Determines if this command can only be executed in query.
	 * 
	 * @return <code>true</code> if this command can only be executed in query.
	 * @since 0.7
	 */
    public boolean isQryCommand() {
        return this.qryCommand;
    }
    
    
    
    /**
     * Sets whether this command can only be executed in a query.
     * 
     * @param qryCommand <code>true</code> if this command can only be executed in query.
     * @since 0.7
     */
    public void setQryCommand(boolean qryCommand) {
        this.qryCommand = qryCommand;
    }
	
	
	
	/**
	 * This method returns a help message for a formal signature with given id. If the
	 * id is not valid, it returns an error string.
	 * 
	 * @param signatureId The id of the signature which help text shall be returned.
	 * @return A help text for this command and the given signature id.
	 */
	public String getHelpText(int signatureId) {
		if (signatureId >= 0 && signatureId < this.signatures.size()) {
			FormalSignature fs = this.signatures.get(signatureId);
			return fs.getHelp();
		}
		return Messages.bind(Messages.commandNoSignature, this.commandName, signatureId);
	}
	
	
	
	/**
	 * Gets a readomly list of all signatures for this command.
	 * @return A list of all formal signatures of this command.
	 */
	public List<FormalSignature> getSignatures() {
		return Collections.unmodifiableList(this.signatures);
	}
	
	
	
	/**
	 * Returns the required user level for executing this Command. The default value is
	 * {@link UserManager#UNKNOWN}, which means everyone may execute it.
	 * 
	 * @return The required user level for this command which is 0 by default.
	 */
	public int getUserLevel() {
		return this.userLevel;
	}
	
	
	
	/**
	 * Sets the required user level to execute this command. Use one of the default
	 * userlevel constants in {@link UserManager} or own values.
	 * 
	 * @param userLevel The new userlevel for this command.
	 * @deprecated Userlevel has been replace by roles
	 */
	@Deprecated
	public void setUserLevel(int userLevel) {
	    this.userLevel = userLevel;
	}
	
	
	
	/**
	 * The hashcode of a command equals the hashcode of its name.
	 * @return The commands hashcode.
	 */
	@Override
	public final int hashCode() {
		return this.commandName.hashCode();
	}
	
	
	
	/**
	 * Convenience method for replying directly to a user.
	 * @param user The user to send a private message to.
	 * @param message The message to send.
	 */
	protected void reply(User user, String message) {
		this.getMyPolly().irc().sendMessage(user.getCurrentNickName(), message);
	}
	
	
	
	/**
	 * Convenience method for replying to a channel or a user.
	 * @param channel The channel (if preceded by "#") or the nickname to send a 
	 * 		message to.
	 * @param message The message to send.
	 */
	protected void reply(String channel, String message) {
		this.getMyPolly().irc().sendMessage(channel, message, this);
	}
	
	
	
	/**
	 * <p>This method is called by polly to execute this command. You should not 
	 * call it yourself.</p>
	 * 
	 * <p>It checks the users permission and if he has sufficient rights, it delivers
	 * the execution to your implementations of the executeOn methods.</p>
	 * 
	 * <p>It also checks if the signature matches the two special signatures for 
	 * displaying this commands help text. If so, the help is sent to the channel and
	 * the method returns.
	 * </p>
	 * 
	 * <p>If {@link #executeOnBoth(User, String, Signature)} returns <code>true</code>, 
	 * it will call {@link #executeOnChannel(User, String, Signature)} or 
	 * {@link #executeOnQuery(User, Signature)} according to given parameters.
	 * If it returns <code>false</code>, none of the both methods will be called 
	 * afterwards. That is the default implementation.</p>
	 * 
	 * @param executer The user who executed this command.
	 * @param channel The channel this command was executed on.
	 * @param query Whether this command was executed on query.
	 * @param signature The actual signature that this command was executed with.
	 * @throws InsufficientRightsException If the executing user has not all permissions 
	 *             required by either this command or the signature he tries to execute.
	 * @throws CommandException Implementors can throw this to indicate an error during
	 *     execution.
	 */
	public void doExecute(User executer, String channel, boolean query, 
	        Signature signature) throws InsufficientRightsException, CommandException {

		// check if help is requested
		if (signature.equals(this.helpSignature0)) {
		    this.reply(channel, this.getHelpText()); 
		    return;
		} else if (signature.equals(this.helpSignature1)) {
		    int num = (int) signature.getNumberValue(1);
		    if (num < 0 || num >= this.signatures.size()) {
		        this.reply(channel, Messages.bind(Messages.commandNoSignatureId, num)); 
		        return;
		    }
		    this.reply(channel, this.getHelpText(num));
		    this.reply(channel, Messages.bind(Messages.commandSample, 
		                    this.commandName, this.signatures.get(num).getSample()));
		    return;
		}

		
		FormalSignature formal = this.signatures.get(signature.getId());
        this.checkPermissions(executer, formal);

		
		
		// execute the command
		try {
    		boolean runOthers = this.executeOnBoth(executer, channel, signature);
    		
    		if (runOthers && query) {
    			this.executeOnQuery(executer, signature);
    		} else if (runOthers) {
    			this.executeOnChannel(executer, channel, signature);
    		}
		} catch (InsufficientRightsException e) {
		    throw e;
		} catch (Exception e) {
		    throw new CommandException(e.getMessage(), e);
		}
	}

	
	
	
	/**
	 * Checks whether a user has the required permissions to execute this command
	 * and the given signature.
	 * 
	 * @param executer The user to check.
	 * @param signature The signature to check.
	 * @throws InsufficientRightsException If the user is not allowed to execute this 
	 *             command.
	 */
    private void checkPermissions(User executer, FormalSignature signature) 
            throws InsufficientRightsException {
           
        // get matching formal signature to the actual signature and check the 
        // permissions.
        if (!this.getMyPolly().roles().canAccess(executer, this)) {
            throw new InsufficientRightsException(this);
        }
        if (!this.getMyPolly().roles().hasPermission(executer, 
            signature.getRequiredPermission())) {
            throw new InsufficientRightsException(signature);
        }
    }
	
	
	
	/**
	 * <p>This method is called either if this command has been executed on a channel or
	 * on a query with a valid signature. That means the the passed signature matches 
	 * one that this command was registered for.</p>
	 * 
	 * <p>If it returns <code>true</code>, the methods 
	 * {@link #executeOnChannel(User, String, Signature)} and 
	 * {@link #executeOnQuery(User, Signature)} are run afterwards. The default 
	 * implementation returns <code>false</code>.</p>
	 * 
	 * <p>If you need to reply, reply to the <tt>channel</tt>. It will point to the user 
	 * if this was executed on a query.</p>
	 * 
	 * @param executer The user who executed this command.
	 * @param channel The channel this command was executed on.
	 * @param signature The actual signature that this command was executed with.
	 * @return Whether the 2 other executeX methods should be run afterwards.
     * @throws CommandException Implementors can throw this to indicate an error during
     *     execution.
     * @throws InsufficientRightsException If, for any reasons, the executor can not 
     *          execute this command.
	 */
	protected boolean executeOnBoth(User executer, String channel, Signature signature) 
	        throws CommandException, InsufficientRightsException { 
		return false;
	}
	
	
	
	/**
	 * <p>This method is called if this command has been executed on a channel with
	 * a valid signature. That means the the passed signature matches one that this
	 * command was registered for.
	 * The default implementation simply does nothing upon calling. Decide yourself
	 * if you want to override it.</p>
	 * 
	 * <p>This method is not called if {@link #executeOnBoth(User, String, Signature)} 
	 * returns true.</p>
	 *  
	 * @param executer The user who executed this command.
	 * @param channel The channel this command was executed on.
	 * @param signature The actual signature that this command was executed with.
     * @throws CommandException Implementors can throw this to indicate an error during
     *     execution.
     * @throws InsufficientRightsException If, for any reasons, the executor can not 
     *          execute this command.
	 */
	protected void executeOnChannel(User executer, String channel, 
			Signature signature) throws CommandException, InsufficientRightsException {}
	
	
	
	/**
	 * <p>This method is called if this command has been executed on a query with
	 * a valid signature. That means the the passed signature matches one that this
	 * command was registered for.
	 * The default implementation simply does nothing upon calling. Decide yourself
	 * if you want to override it.</p>
	 * 
	 * <p>This method is not called if {@link #executeOnBoth(User, String, Signature)} 
	 * returns true.</p>
	 *  
	 * @param executer The user who executed this command in a query.
	 * @param signature The actual signature that this command was executed with.
     * @throws CommandException Implementors can throw this to indicate an error during
     *     execution.
     * @throws InsufficientRightsException If, for any reasons, the executor can not 
     *          execute this command.
	 */
	protected void executeOnQuery(User executer, Signature signature) 
            throws CommandException, InsufficientRightsException {}
	
	
	
	/**
	 * Use this method in {@link #executeOnChannel(User, String, Signature)} and 
	 * {@link #executeOnQuery(User, Signature)} to determine which formal signature
	 * matches the actual passed signature.
	 * 
	 * @param actual The actual signature that has been passed by polly.
	 * @param formalId The id of the formal signature to check the actual against.
	 * @return <code>true</code> if the actual signature matches the formal id. 
	 * 		<code>false</code> otherwise.
	 */
	protected boolean match(Signature actual, int formalId) {
		return actual.getId() == formalId;
	}
	
	
	
	/**
	 * This is the formal signature for displaying a brief description of this command.
	 * It looks like 'commandName ?'
	 * 
	 * @return the formal signature for this commands help text.
	 * @since 0.9
	 * @see #setHelpText(String)
	 */
	public FormalSignature getHelpSignature0() {
	    return this.helpSignature0;
	}
	
	
	
    /**
     * This is the formal signature for displaying the help text of a signature of this
     * command. It looks like 'commandName ? Number'
     * 
     * @return the formal signature for this commands signatures help texts.
     * @since 0.9
     * @see FormalSignature#FormalSignature(String, int, String, Parameter...)
     * @see FormalSignature#FormalSignature(String, int, String, String, Parameter...)
     */
    public FormalSignature getHelpSignature1() {
        return this.helpSignature1;
    }
	
	
	
	/**
	 * Factory method for creating a new signature for this command. The new signatures 
	 * formal id gets incremented each call and this signature will have no required 
	 * permission.
	 * 
	 * @param help A description text for this signature.
	 * @param parameters The formal parameters for the new signature.
	 * @return A new signature for this command.
	 * @throws DuplicatedSignatureException If the same signature already exists.
	 */	
	public Signature createSignature(String help, Parameter... parameters)
	        throws DuplicatedSignatureException {
	    
	    return this.createSignature(help, RoleManager.NONE_PERMISSION, parameters);
	}
	
	
	
    /**
     * Factory method for creating a new signature for this command. Its equivalent 
     * of creating a new Signature with this commands name. The new signatures 
     * formal id gets incremented each call.
     * 
     * @param help A description text for this signature.
     * @param permissionName The name of the permission that is required to execute this
     *      signature.
     * @param parameters The formal parameters for the new signature.
     * @return A new signature for this command.
     * @throws DuplicatedSignatureException If the same signature already exists.
     */ 
    public Signature createSignature(String help, String permissionName, 
            Parameter... parameters) throws DuplicatedSignatureException {
        int id = this.signatures.size();
        FormalSignature fs = new FormalSignature(
            this.getCommandName(), id, help, permissionName, parameters);
        
        if (this.signatures.contains(fs)) {
            throw new DuplicatedSignatureException(fs.toString());
        }
        this.signatures.add(fs);
        this.containedPermissions.addAll(fs.getRequiredPermission());
        
        return fs;
    }
    
    
    
    @Override
    public Set<String> getContainedPermissions() {
        return this.containedPermissions;
    }
    
    
    
    @Override
    public final Set<String> getRequiredPermission() {
        if (this.registeredOnly) {
            return Collections.singleton(RoleManager.REGISTERED_PERMISSION);
        }
        return Collections.singleton(RoleManager.NONE_PERMISSION);
    }
	
	
	
	/**
	 * Sets whether this command can be executed only by registered(signed on) users.
	 * @param registeredOnly Whether registered users only can execute this command.
	 */
	public void setRegisteredOnly(boolean registeredOnly) {
	    this.registeredOnly = registeredOnly;
	}
	
	
	
	/**
	 * Sets this command to be executable by registered users only.
	 */
	public void setRegisteredOnly() {
	    this.registeredOnly = true;
	}
	
	
	
	/**
	 * Determines whether this command can be executed by anyone or only registered users.
	 * @return <code>true</code> if only registered users can execute this command.
	 * @see #setRegisteredOnly()
	 * @see #setRegisteredOnly(boolean)
	 */
	public boolean isRegisteredOnly() {
	    return this.registeredOnly;
	}
	
	
	
	/**
	 * Determines whether this command will be stored in the {@link CommandManager}s
	 * command history. This method always returns <code>true</code> and may be 
	 * overriden in order to achieve different behavior.
	 * 
	 * @return If the command should be tracked in the command history.
	 * @since 0.8
	 */
	public boolean trackInHistory() {
	    return true;
	}
	
	
	
	/**
	 * Convenience wrapper method for creating a {@link Conversation}.
	 * 
     * @param user The user this conversation is for.
     * @param channel The channel this conversation is for.
     * @return The new {@link Conversation} instance.
     * @throws ConversationException If there is already a conversation active with the
     *          same user on the same channel.
	 * @see ConversationManager
	 * @since 0.6.0
	 */
	protected Conversation createConversation(User user, String channel) 
	            throws ConversationException {
	    return this.getMyPolly().conversations().create(this.getMyPolly().irc(), user, 
	        channel);
	}
	
	
	
	/**
	 * Compares two commands and considers them equals if they have the same commandname.
	 * @param obj The object to compare this command with.
	 * @return <code>true</code> iff the other obj is an instanceof command and its 
	 * 		commandname equals this commands name.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Command)) {
			return false;
		}
		
		return ((Command) obj).getCommandName().equals(this.getCommandName());
	}
	
	
	
	/**
	 * This method does nothing in its default implementation. It can be overridden to 
	 * provide some command specific constants before actually evaluating a polly 
	 * expression.
	 * 
	 * @param map Map that can be filled with custom variables. 
	 * @since 0.7
	 */
	public void renewConstants(Map<String, Types> map) { }
	
	
	
	/**
	 * Returns a String representation of this command.
	 * 
	 * @return The name of this command.
	 */
	@Override
	public String toString() {
	    return this.getNameString();
	}
	
	
	
	/**
	 * Constructs the result string for the {@link #toString()} Method. It appends
	 * "R" and/or "qry" to the command name according to whether this command is 
	 * only executable for registered user or in query only.
	 * 
	 * @return The command name with additional infos.
	 */
	protected String getNameString() {
	    StringBuilder result = new StringBuilder(this.getCommandName().length() + 10);
	    result.append(this.commandName);
	    
	    if (this.isRegisteredOnly() || this.isQryCommand()) {
	        result.append("("); //$NON-NLS-1$
	        if (this.registeredOnly) {
	            result.append("R"); //$NON-NLS-1$
	        } 
	        if (this.isQryCommand()) {
	            if (this.isRegisteredOnly()) {
	                result.append(","); //$NON-NLS-1$
	            }
	            result.append("qry"); //$NON-NLS-1$
	        }
	        result.append(")"); //$NON-NLS-1$
	    }
	    return result.toString();
	}
	
	
	
	/**
	 * Implementation of the {@link Disposable} interface. The default implementation
	 * in the {@link Command} class is empty.
	 */
	@Override
	protected void actualDispose() throws DisposingException {}
	
	
	
	/**
	 * Compares 2 commands using their required user level. Commands will be sorted from
	 * low user level to high user level.
	 * 
	 * @return {@inheritDoc}
	 */
	@Override
	public int compareTo(Command o) {
	    int first = ((Integer)this.userLevel).compareTo(o.userLevel);
	    if (first == 0) {
	        return this.getCommandName().compareTo(o.getCommandName());
	    } else {
	        return first;
	    }
	}
}