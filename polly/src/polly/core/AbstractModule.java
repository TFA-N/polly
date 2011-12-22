package polly.core;

import org.apache.log4j.Logger;

public abstract class AbstractModule implements Module {

    protected Logger logger = Logger.getLogger(AbstractModule.class.getName());

    private ModuleLoader loader;
    private String name;
    private boolean crucial;
    private boolean setup;
    private boolean run;



    public AbstractModule(String name, ModuleLoader loader, boolean isCrucial) {
        this.name = name;
        this.loader = loader;
        this.crucial = isCrucial;

        loader.registerModule(this);
    }



    @Override
    public void addState(int state) {
        this.loader.addState(state);
    }



    @Override
    public boolean isStateSet(int state) {
        return this.loader.isStateSet(state);
    }



    @Override
    public void requireState(int state) {
        this.loader.requireState(state, this);
    }



    @Override
    public void willSetState(int state) {
        this.loader.willSetState(state, this);
    }



    @Override
    public <T> void willProvideDuringSetup(Class<T> component) {
        this.loader.willProvideDuringSetup(component, this);
    }



    @Override
    public <T> void requireBeforeSetup(Class<T> component) {
        this.loader.requireBeforeSetup(component, this);
    }



    @Override
    public void provideComponent(Object component) {
        this.loader.provideComponent(component);
    }



    @Override
    public void provideComponentAs(Class<?> type, Object component) {
        this.loader.provideComponentAs(type, component);
    }



    @Override
    public <T> T requireNow(Class<T> component) {
        return this.loader.requireNow(component);
    }



    @Override
    public ModuleLoader getModuleLoader() {
        return this.loader;
    }



    public void beforeSetup() {
    }



    @Override
    public final void setupModule() throws SetupException {
        if (this.setup) {
            return;
        }

        try {
            this.beforeSetup();
            this.setup();
        } catch (SetupException e) {
            if (this.crucial) {
                throw e;
            }
            logger.error(
                "Error while setup of non-crucial module '" + this.getName()
                    + "'", e);
        } finally {
            this.setup = true;
        }
    }



    public abstract void setup() throws SetupException;



    public void beforeRun() {}



    public final void runModule() throws Exception {
        if (this.run) {
            return;
        } else if (!this.setup) {
            throw new ModuleDependencyException("Module " + this + 
                    "' must be set up before running!");
        }

        try {
            this.beforeRun();
            this.run();
        } catch (Exception e) {
            if (this.isCrucial()) {
                throw e;
            }
        } finally {
            this.run = true;
        }
    }



    public void run() throws Exception {
    }



    @Override
    public boolean isCrucial() {
        return this.crucial;
    }



    @Override
    public boolean isSetup() {
        return this.setup;
    }



    @Override
    public boolean isRun() {
        return this.run;
    }



    @Override
    public String getName() {
        return this.name;
    }



    @Override
    public String toString() {
        return this.getName();
    }
}