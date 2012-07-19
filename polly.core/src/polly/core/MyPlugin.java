package polly.core;


import commands.AddTrainCommand;
import commands.AddUserCommand;
import commands.AliasCommand;
import commands.CalendarCommand;
import commands.CloseTrainCommand;
import commands.ClumBombCommand;
import commands.DefineCommand;
import commands.DelVarCommand;
import commands.DeleteUserCommand;
import commands.DeliverTrainCommand;
import commands.DictCommand;
import commands.DitoCommand;
import commands.ExportAttributesCommand;
import commands.FooCommand;
import commands.GetAttributeCommand;
import commands.GhostCommand;
import commands.GooglePicsCommand;
import commands.GreetingCommand;
import commands.HopCommand;
import commands.IsDownCommand;
import commands.JoinCommand;
import commands.KickCommand;
import commands.ListAttributesCommand;
import commands.LmgtfyCommand;
import commands.AnyficationCommand;
import commands.MyTrainsCommand;
import commands.MyVenadCommand;
import commands.PartCommand;
import commands.QuitCommand;
import commands.RawIrcCommand;
import commands.RegisterCommand;
import commands.ReloadConfigCommand;
import commands.RestartCommand;
import commands.SetAttributeCommand;
import commands.SetMyPasswordCommand;
import commands.SetPasswordCommand;
import commands.SetUserLevelCommand;
import commands.ShowCommandsCommand;
import commands.InfoCommand;
import commands.SignOffCommand;
import commands.AuthCommand;
import commands.TalkCommand;
import commands.UptimeCommand;
import commands.UsersCommand;
import commands.VarCommand;
import commands.VenadCommand;
import commands.VersionCommand;
import commands.WikiCommand;
import commands.roles.AssignPermissionCommand;
import commands.roles.AssignRoleCommand;
import commands.roles.CreateRoleCommand;
import commands.roles.DeleteRoleCommand;
import commands.roles.ListPermissionsCommand;
import commands.roles.ListRolesCommand;
import commands.roles.RemovePermissionCommand;
import commands.roles.RemoveRoleCommand;
import core.GreetDeliverer;
import core.JoinTimeCollector;
import core.TrainManagerV2;

import de.skuzzle.polly.sdk.MyPolly;
import de.skuzzle.polly.sdk.PollyPlugin;
import de.skuzzle.polly.sdk.constraints.Constraints;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.DisposingException;
import de.skuzzle.polly.sdk.exceptions.DuplicatedSignatureException;
import de.skuzzle.polly.sdk.exceptions.IncompatiblePluginException;
import de.skuzzle.polly.sdk.exceptions.RoleException;
import de.skuzzle.polly.sdk.roles.RoleManager;
import entities.TopicEntity;
import entities.TrainEntity;
import entities.TrainEntityConverter;
import entities.TrainEntityV2;


/**
 * 
 * @author Simon
 * @version 27.07.2011 3851c1b
 */
public class MyPlugin extends PollyPlugin {
    
    public final static String TRAINER = "polly.roles.TRAINER";
    public final static String CASPER  = "polly.roles.CASPER";
    
    public final static String ADD_TRAIN_PERMISSION              = "polly.permission.ADD_TRAIN";
    public final static String ADD_USER_PERMISSION               = "polly.permission.ADD_USER";
    public final static String ALIAS_PERMISSION                  = "polly.permission.ALIAS";
    public final static String ANYFICATION_PERMISSION            = "polly.permission.ANYFICATION";
    public final static String CLOSE_TRAIN_PERMISSION            = "polly.permission.CLOSE_TRAIN";
    public final static String CLUMBOMB_PERMISSION               = "polly.permission.CLUMBOMB";
    public final static String DEFINE_PERMISSION                 = "polly.permission.DEFINE";
    public final static String DELETE_USER_PERMISSION            = "polly.permission.DELETE_USER";
    public final static String DELIVER_TRAIN_PERMISSION          = "polly.permission.DELIVER_TRAIN";
    public final static String DICT_PERMISSION                   = "polly.permission.DICT";
    public final static String DITO_PERMISSION                   = "polly.permission.DITO";
    public final static String EXPORT_ATTRIBUTES_PERMISSION      = "polly.permission.EXPORT_ATTRIBUTES";
    public final static String EXPORT_USER_ATTRIBUTES_PERMISSION = "polly.permission.EXPORT_USER_ATTRIBUTES";
    public final static String GET_ATTRIBUTE_PERMISSION          = "polly.permission.GET_ATTRIBUTE";
    public final static String GET_USER_ATTRIBUTE_PERMISSION     = "polly.permission.GET_USER_ATTRIBUTE";
    public final static String HOP_PERMISSION                    = "polly.permission.HOP";
    public final static String INFO_PERMISSION                   = "polly.permission.INFO";
    public final static String ISDOWN_PERMISSION                 = "polly.permission.ISDOWN";
    public final static String JOIN_PERMISSION                   = "polly.permission.JOIN";
    public final static String KICK_PERMISSION                   = "polly.permission.KICK";
    public final static String LIST_ATTRIBUTES_PERMISSION        = "polly.permission.LIST_ATTRIBUTES";
    public final static String MYTRAINS_PERMISSION               = "polly.permission.MY_TRAINS";
    public final static String MY_VENAD_PERMISSION               = "polly.permission.MY_VENAD";
    public final static String PART_PERMISSION                   = "polly.permission.PART";
    public final static String QUIT_PERMISSION                   = "polly.permission.QUIT";
    public final static String RAW_IRC_PERMISSION                = "polly.permission.RAW_IRC";
    public final static String RELOAD_CFG_PERMISSION             = "polly.permission.RELOAD_CFG";
    public final static String RESTART_PERMISSION                = "polly.permission.RESTART";
    public final static String SET_ATTRIBUTE_PERMISSION          = "polly.permission.SET_ATTRIBUTE";
    public final static String SET_USER_ATTRIBUTE_PERMISSION     = "polly.permission.SET_USER_ATTRIBUTE";
    public final static String SET_PASSWORD_PERMISSION           = "polly.permission.SET_PASSWORD";
    public final static String TALK_PERMISSION                   = "polly.permission.TALK";
    public final static String UPTIME_PERMISSION                 = "polly.permission.UPTIME";
    public final static String LIST_USERS_PERMISSION             = "polly.permission.LIST_USERS";
    public final static String LIST_VARS_PERMISSION              = "polly.permission.LIST_VARS";
    public final static String ASSIGN_PERMISSION_PERMISSION      = "polly.permission.ASSIGN_PERMISSION";
    public final static String ASSIGN_ROLE_PERMISSION            = "polly.permission.ASSIGN_ROLE";
    public final static String CREATE_ROLE_PERMISSION            = "polly.permission.CREATE_ROLE";
    public final static String DELETE_ROLE_PERMISSION            = "polly.permission.DELETE_ROLE";
    public final static String LIST_PERMISSIONS_PERMISSION       = "polly.permission.LIST_PERMISSIONS";
    public final static String LIST_ROLES_PERMISSION             = "polly.permission.LIST_ROLES";
    public final static String REMOVE_PERMISSION_PERMISSION      = "polly.permission.REMOVE_PERMISSION";
    public final static String REMOVE_ROLE_PERMISSION            = "polly.permission.REMOVE_ROLE";
    
    public final static String GREETING = "GREETING";
    public final static String VENAD    = "VENAD";
    
    private TrainManagerV2 trainManager;
    private GreetDeliverer greetDeliverer;
    private JoinTimeCollector joinTimeCollector;
    
    
    
    @Override
    public void assignPermissions(RoleManager roleManager) 
            throws RoleException, DatabaseException {
        super.assignPermissions(roleManager);
        
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, ADD_USER_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, DELETE_USER_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, TALK_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, ALIAS_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, EXPORT_USER_ATTRIBUTES_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, SET_USER_ATTRIBUTE_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, GET_USER_ATTRIBUTE_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, SET_PASSWORD_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, PART_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, QUIT_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, KICK_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, JOIN_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, RAW_IRC_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, RESTART_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, RELOAD_CFG_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, ASSIGN_PERMISSION_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, ASSIGN_ROLE_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, CREATE_ROLE_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, DELETE_ROLE_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, LIST_PERMISSIONS_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, LIST_ROLES_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, REMOVE_PERMISSION_PERMISSION);
        roleManager.assignPermission(RoleManager.ADMIN_ROLE, REMOVE_ROLE_PERMISSION);
        
        roleManager.createRole(TRAINER);
        roleManager.assignPermission(TRAINER, ADD_TRAIN_PERMISSION);
        roleManager.assignPermission(TRAINER, CLOSE_TRAIN_PERMISSION);
        roleManager.assignPermission(TRAINER, DELIVER_TRAIN_PERMISSION);
        
        roleManager.createRole(CASPER);
        roleManager.assignPermission(CASPER, CLUMBOMB_PERMISSION);
        roleManager.assignPermission(CASPER, ANYFICATION_PERMISSION);
        
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, MYTRAINS_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, DEFINE_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, DICT_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, DITO_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, GET_ATTRIBUTE_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, LIST_ATTRIBUTES_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, SET_ATTRIBUTE_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, ISDOWN_PERMISSION);
        roleManager.assignPermission(RoleManager.DEFAULT_ROLE, EXPORT_ATTRIBUTES_PERMISSION);
    }
    
    
    
	public MyPlugin(MyPolly myPolly) throws IncompatiblePluginException, 
	            DuplicatedSignatureException {
	    
		super(myPolly);
		
		this.addCommand(new AnyficationCommand(myPolly));
		
		//this.topicManager = new TopicManager(myPolly);
		this.trainManager = new TrainManagerV2(myPolly);
        this.getMyPolly().persistence().registerEntity(TopicEntity.class);
        this.getMyPolly().persistence().registerEntity(TrainEntity.class);
        this.getMyPolly().persistence().registerEntity(TrainEntityV2.class);
        
        this.greetDeliverer = new GreetDeliverer(myPolly);
        this.getMyPolly().users().addUserListener(this.greetDeliverer);
        this.addCommand(new GreetingCommand(myPolly));
        
        this.addCommand(new AliasCommand(myPolly));
		this.addCommand(new InfoCommand(myPolly));
		this.addCommand(new QuitCommand(myPolly));
		this.addCommand(new JoinCommand(myPolly));
		this.addCommand(new PartCommand(myPolly));
		this.addCommand(new HopCommand(myPolly));
		this.addCommand(new KickCommand(myPolly));
		this.addCommand(new ClumBombCommand(myPolly));
		this.addCommand(new TalkCommand(myPolly));
		this.addCommand(new VersionCommand(myPolly));
		this.addCommand(new ShowCommandsCommand(myPolly));
		this.addCommand(new CalendarCommand(myPolly));
		
		this.addCommand(new WikiCommand(myPolly));
		this.addCommand(new DefineCommand(myPolly));
		this.addCommand(new LmgtfyCommand(myPolly));
		this.addCommand(new DictCommand(myPolly));
		this.addCommand(new GooglePicsCommand(myPolly));
		this.addCommand(new IsDownCommand(myPolly));
		
		this.addCommand(new UsersCommand(myPolly));
		this.addCommand(new AuthCommand(myPolly));
		this.addCommand(new SignOffCommand(myPolly));
		this.addCommand(new GhostCommand(myPolly));
		this.addCommand(new AddUserCommand(myPolly));
		this.addCommand(new DeleteUserCommand(myPolly));
		this.addCommand(new SetMyPasswordCommand(myPolly));
		this.addCommand(new SetPasswordCommand(myPolly));
		this.addCommand(new SetUserLevelCommand(myPolly));
		this.addCommand(new RegisterCommand(myPolly));
		this.addCommand(new RawIrcCommand(myPolly));
		this.addCommand(new SetAttributeCommand(myPolly));
		this.addCommand(new ListAttributesCommand(myPolly));
		this.addCommand(new GetAttributeCommand(myPolly));
		this.addCommand(new ExportAttributesCommand(myPolly));
		
		
		
		this.addCommand(new AssignRoleCommand(myPolly));
		this.addCommand(new RemoveRoleCommand(myPolly));
		this.addCommand(new CreateRoleCommand(myPolly));
		this.addCommand(new DeleteRoleCommand(myPolly));
		this.addCommand(new ListRolesCommand(myPolly));
		this.addCommand(new AssignPermissionCommand(myPolly));
		this.addCommand(new RemovePermissionCommand(myPolly));
		this.addCommand(new ListPermissionsCommand(myPolly));
		
		
		
		this.joinTimeCollector = new JoinTimeCollector(myPolly.getTimeProvider());
		this.joinTimeCollector.addTo(myPolly.irc());
		this.addCommand(new UptimeCommand(myPolly, this.joinTimeCollector));
		

		this.addCommand(new FooCommand(myPolly));
		this.addCommand(new VarCommand(myPolly));
		this.addCommand(new DelVarCommand(myPolly));
		
		this.addCommand(new VenadCommand(myPolly));
		this.addCommand(new MyVenadCommand(myPolly));
		//this.addCommand(new AddTopicCommand(myPolly, this.topicManager));
		
		this.addCommand(new AddTrainCommand(myPolly, this.trainManager));
		this.addCommand(new CloseTrainCommand(myPolly, this.trainManager));
		this.addCommand(new MyTrainsCommand(myPolly, this.trainManager));
		this.addCommand(new DeliverTrainCommand(myPolly, this.trainManager));
		
		this.addCommand(new RestartCommand(myPolly));
		this.addCommand(new ReloadConfigCommand(myPolly));
		
		this.addCommand(new DitoCommand(myPolly));
	}
	
	
	
	@Override
	public void onLoad() {
	    //this.topicManager.loadAll();
	    /*Calendar c = Calendar.getInstance();
	    c.set(Calendar.DAY_OF_MONTH, 25);
	    TopicEntity test = new TopicEntity("C0mb4t", "#debugging", "Noch %ld% Tage bis ka", "Nu isses soweit", c.getTime());
	    this.topicManager.addTopicTask(test);*/
	    
	    TrainEntityConverter tec = new TrainEntityConverter(
	        this.getMyPolly().persistence());
	    try {
            tec.convertAllTrains();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
	    
	    try {
	        this.getMyPolly().users().addAttribute(VENAD, "<unbekannt>");
	        this.getMyPolly().users().addAttribute(GREETING, "");
	        this.getMyPolly().users().addAttribute("AZ", "0", Constraints.INTEGER);
	    } catch (Exception ignore) {
	        ignore.printStackTrace();
	    }
	}
	
	
	
	@Override
	protected void actualDispose() throws DisposingException {
	    super.actualDispose();
	    //this.topicManager.dispose();
	    this.getMyPolly().users().removeUserListener(this.greetDeliverer);
	    this.joinTimeCollector.remove(this.getMyPolly().irc());
	}
}