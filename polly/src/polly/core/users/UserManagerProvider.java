package polly.core.users;


import java.io.IOException;

import de.skuzzle.polly.sdk.Configuration;
import de.skuzzle.polly.sdk.exceptions.DatabaseException;
import de.skuzzle.polly.sdk.exceptions.UserExistsException;
import de.skuzzle.polly.sdk.roles.RoleManager;
import de.skuzzle.polly.tools.events.EventProvider;
import polly.configuration.ConfigurationProviderImpl;
import polly.core.ModuleStates;
import polly.core.ShutdownManagerImpl;
import polly.core.persistence.PersistenceManagerImpl;
import polly.core.roles.RoleManagerImpl;
import polly.moduleloader.AbstractProvider;
import polly.moduleloader.ModuleLoader;
import polly.moduleloader.SetupException;
import polly.moduleloader.annotations.Module;
import polly.moduleloader.annotations.Require;
import polly.moduleloader.annotations.Provide;;



@Module(
    requires = {
        @Require(component = ConfigurationProviderImpl.class),
        @Require(component = ShutdownManagerImpl.class),
        @Require(component = EventProvider.class),
        @Require(component = PersistenceManagerImpl.class),
        @Require(component = RoleManagerImpl.class),
        @Require(state = ModuleStates.PERSISTENCE_READY),
        @Require(state = ModuleStates.ROLES_READY)
    },
    provides = {
        @Provide(component = UserManagerImpl.class),
        @Provide(state = ModuleStates.USERS_READY)
    })
public class UserManagerProvider extends AbstractProvider {
    
    public final static String USER_CONFIG = "user.cfg";

    private PersistenceManagerImpl persistenceManager;
    private EventProvider eventProvider;
    private ShutdownManagerImpl shutdownManager;
    private UserManagerImpl userManager;
    private RoleManagerImpl roleManager;
    private Configuration userCfg;

    public UserManagerProvider(ModuleLoader loader) {
        super("USER_MANAGER_PROVIDER", loader, true);
    }



    @Override
    public void beforeSetup() {
        this.eventProvider = this.requireNow(EventProvider.class, true);
        this.persistenceManager = this.requireNow(PersistenceManagerImpl.class, true);
        this.shutdownManager = this.requireNow(ShutdownManagerImpl.class, true);
        this.roleManager = this.requireNow(RoleManagerImpl.class, true);
    }



    @Override
    public void setup() throws SetupException {
        ConfigurationProviderImpl configProvider = 
                this.requireNow(ConfigurationProviderImpl.class, true);
        try {
            userCfg = configProvider.open(USER_CONFIG);
        } catch (IOException e) {
            throw new SetupException(e);
        }
        
        boolean ignoreUnknownIdentifiers = userCfg.readBoolean(Configuration.IGNORE_UNKNOWN_IDENTIFIERS);
        int tempVarLifeTime = this.userCfg.readInt(Configuration.TEMP_VAR_LIFETIME);
        String declarationCachePath = this.userCfg.readString(Configuration.DECLARATION_CACHE);
        
        this.userManager = new UserManagerImpl(
            this.persistenceManager,
            declarationCachePath, 
            tempVarLifeTime, 
            ignoreUnknownIdentifiers, 
            this.eventProvider, 
            this.roleManager);
        
        this.provideComponent(this.userManager);
        this.shutdownManager.addDisposable(this.userManager);
    }
    


    @Override
    public void run() throws Exception {
        de.skuzzle.polly.sdk.model.User admin = null;
        try {
            String adminName = this.userCfg.readString(Configuration.ADMIN_NAME); 
            logger.info("Creating default user with name '"
                + adminName + "'.");
            admin = this.userManager.createUser(adminName, "");

            admin.setHashedPassword(this.userCfg.readString(
                Configuration.ADMIN_PASSWORD_HASH));
            this.userManager.addUser(admin);
        } catch (UserExistsException e) {
            admin = e.getUser();
            logger.debug("Default user already existed.");
        } catch (DatabaseException e) {
            logger.fatal("Database error", e);
        } finally {
            this.userManager.setAdmin(admin);
            ((User)admin).setIsPollyAdmin(true);
            this.addState(ModuleStates.USERS_READY);
            this.roleManager.assignRole(admin, RoleManager.ADMIN_ROLE);
        }
    }

    
    
    @Override
    public void dispose() {
        this.userCfg = null;
        this.eventProvider = null;
        this.persistenceManager = null;
        this.shutdownManager = null;
        this.userManager = null;
        super.dispose();
    }
}
