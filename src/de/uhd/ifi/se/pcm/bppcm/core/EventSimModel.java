package de.uhd.ifi.se.pcm.bppcm.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.osgi.framework.BundleContext;


import de.uhd.ifi.se.pcm.bppcm.core.BPPCMModel;
import de.uhd.ifi.se.pcm.bppcm.resources.ActorResourceRegistry;
import de.uhd.ifi.se.pcm.bppcm.resources.DeviceResourceRegistry;
import de.uhd.ifi.se.pcm.bppcm.resources.Dispatcher;
import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.BpusagemodelPackage;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.OrganizationEnvironmentModel;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.resourcetype.SchedulingPolicy;

import de.uka.ipd.sdq.scheduler.ISchedulingFactory;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimEngineFactory;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationControl;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationModel;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.command.ICommand;
import edu.kit.ipd.sdq.eventsim.command.ICommandExecutor;
import edu.kit.ipd.sdq.eventsim.command.PCMModelCommandExecutor;
import edu.kit.ipd.sdq.eventsim.entities.EventSimEntity;
import edu.kit.ipd.sdq.eventsim.resources.PassiveResourceRegistry;
import edu.kit.ipd.sdq.eventsim.system.staticstructure.AllocationRegistry;
import edu.kit.ipd.sdq.eventsim.system.staticstructure.ComponentInstance;
import edu.kit.ipd.sdq.eventsim.system.staticstructure.SimulatedResourceEnvironment;



/**
 * The simulation model. This is the central class of an EventSim simulation run. Before the
 * simulation starts, it initialises the simulation in the {@code init()} method. During the
 * simulation, it provides information about the PCM model that is to be simulated, the simulation
 * configuration and the simulation status. Finally, it cleans up after a simulation run in the
 * {finalise()} method.
 * <p>
 * Instances are created by using the static {@code create} method that builds the simulation model
 * in accordance with a specified simulation configuration.
 * 
 * @author Philipp Merkle, Robert Heinrich (BP-specific parts)
 * 
 */
public class EventSimModel extends SchedulerModel implements ISimulationModel {

    private static final Logger logger = Logger.getLogger(EventSimModel.class);

    private final BundleContext bundleContext;
    private final EventSimConfig config;
    private final ICommandExecutor<PCMModel> executor;
    private PCMModel pcmModel;
    private ISimEngineFactory factory;
    private ISimulationControl simControl;
    private long mainMeasurementsCount;

    private final List<EventSimEntity> activeEntitiesList;
    private final PassiveResourceRegistry passiveResourceRegistry;
    private SimulatedResourceEnvironment resourceEnvironment;
    private AllocationRegistry resourceAllocation;
    private Map<String, ComponentInstance> componentRegistry;

    // mapping for business process resources
    private ActorResourceRegistry actorResourceRegistry;
    private DeviceResourceRegistry deviceResourceRegistry;

//	private EntityTraceRecorder traceRecorder;
//    private ProbeSpecContext probeSpecContext;
    private ISchedulingFactory schedulingFactory;

//    private SeffBehaviourInterpreter seffInterpreter;
//    private UsageBehaviourInterpreter usageInterpreter;
//    
    private Dispatcher dispatcher;

    // TODO move this to definition to another class
    // TODO mainly, make this work -> scheduling policy only static class, no extendability anymore
    public static final SchedulingPolicy BP_FCFS = new SchedulingPolicy("edu.kit.ipd.sdq.pcm.simulation.scheduler.bpfcfs",
            "Business Process First-Come First-Served") {
        // no abstract methods need to be implemented
    };
    
    // FIXME
    private static final IPath ORGANIZATIONAL_MODEL_LOCATION = new Path("model/My.organizationenvironmentmodel");
    private OrganizationEnvironmentModel organisationalModel;

    private EventSimModel(final EventSimConfig config, final ISimEngineFactory factory, final BundleContext context) {
        this.config = config;
        this.bundleContext = context;
        this.factory = factory;
        this.executor = new PCMModelCommandExecutor(this.getPCMModel());
        this.passiveResourceRegistry = new PassiveResourceRegistry();
        this.activeEntitiesList = new CopyOnWriteArrayList<EventSimEntity>();
        this.actorResourceRegistry = new ActorResourceRegistry();
        this.deviceResourceRegistry = new DeviceResourceRegistry();
    }

    /**
     * Creates the simulation model for the specified configuration.
     * 
     * @param config
     *            the simulation configuration
     * @param bundleContext
     * @return the created simulation model
     */
    public static EventSimModel create(final EventSimConfig config, final BundleContext bundleContext) {
        // load factory for the preferred simulation engine
        ISimEngineFactory factory = SimulationPreferencesHelper.getPreferredSimulationEngine();
        if (factory == null) {
            throw new RuntimeException("There is no simulation engine available. Install at least one engine.");
        }

        // create simulation model
        final EventSimModel model = new EventSimModel(config, factory, bundleContext);
        factory.setModel(model);

        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        // initialise behaviour interpreters
        seffInterpreter = new SeffBehaviourInterpreter(new SeffInterpreterConfiguration());
        UsageInterpreterConfiguration interpreterConfiguration = new UsageInterpreterConfiguration();
        usageInterpreter = new UsageBehaviourInterpreter(interpreterConfiguration);
        interpreterConfiguration.registerActionHandler(BpusagemodelPackage.eINSTANCE.getActorStep(), new ActorStepTraversalStrategy());
        interpreterConfiguration.registerActionHandler(BpusagemodelPackage.eINSTANCE.getActivity(), new ActivityTraversalStrategy());
        interpreterConfiguration.registerActionHandler(BpusagemodelPackage.eINSTANCE.getAcquireDeviceResourceAction(), new AcquireDeviceResourceTraversalStrategy());
        interpreterConfiguration.registerActionHandler(BpusagemodelPackage.eINSTANCE.getReleaseDeviceResourceAction(), new ReleaseDeviceResourceTraversalStrategy());
        
        
        BpusagemodelPackage.eINSTANCE.getProcessWorkload_ProcessTriggerPeriods();
        
        // make business process scheduling policies available
        SchedulingPolicy.registerSchedulingPolicy(BP_FCFS);
        
        // initialise probfunction factory and random generator
        IProbabilityFunctionFactory probFunctionFactory = ProbabilityFunctionFactoryImpl.getInstance();
        probFunctionFactory.setRandomGenerator(this.config.getRandomGenerator());
        StoExCache.initialiseStoExCache(probFunctionFactory);

        // set up the resource scheduler
        schedulingFactory = new SchedulingFactory(this);

        // initialise resource environment and allocation
        this.resourceEnvironment = this.execute(new BuildSimulatedResourceEnvironment(this));
        this.resourceAllocation = this.execute(new BuildResourceAllocation(this.resourceEnvironment));

        // initialise component instances
        this.componentRegistry = this.execute(new BuildComponentInstances(this, this.resourceAllocation));

        // initialise passive resources
        this.execute(new BuildAndRegisterPassiveResources(this, this.resourceAllocation));
        
        // initialise actor resource instances
        this.execute(new BuildAndRegisterActorResources(this));
        
        // track active actor resouces
		ActorResourceTracker tracker = new ActorResourceTracker(this);
		tracker.track(actorResourceRegistry.getAllActorResourceInstances());
        
        // initialize actor resource dispatcher
        this.dispatcher = new Dispatcher(this, tracker);
        
        // initialise device resource instances
        this.execute(new BuildAndRegisterDeviceResources(this));
        
        // install debug traversal listeners, if debugging is enabled
        if (logger.isDebugEnabled()) {
            DebugUsageTraversalListener.install(this.usageInterpreter.getConfiguration());
            DebugSeffTraversalListener.install(this.seffInterpreter.getConfiguration());
            // SimSlowdown.install("_K_PPcEg4Ed2v5eXKEbOQ9g");
        }

        // this.traceRecorder = EntityTraceRecorder.install();

        // setup handling for PCM parameter characterisations
        this.execute(new InstallSystemCallParameterHandling(this.usageInterpreter.getConfiguration()));
        this.execute(new InstallExternalCallParameterHandling(this.seffInterpreter.getConfiguration()));

        // initialise the Probe Specification
        this.initialiseProbeSpecification();

        // setup the stop conditions
        this.setupStopConditions();

        // notify registered listeners that the simulation is about to start...
        this.notifyStartListeners();

        // ...and start the simulation by generating the workload
        final List<IWorkloadGenerator> workloadGenerators = this.execute(new BuildWorkloadGenerator(this));
        for (final IWorkloadGenerator d : workloadGenerators) {
            d.processWorkload();
        }
    }

    /**
     * Initialises the Probe Specification by building the {@link ProbeSpecContext}, setting up the
     * calculators and mounting the probes.
     */
    private void initialiseProbeSpecification() {
        // create ProbeSpecification context
        probeSpecContext = new ProbeSpecContext();

        // create a blackboard of the specified type
        ISampleBlackboard blackboard = BlackboardFactory.createBlackboard(config.getBlackboardType(), probeSpecContext
                .getThreadManager());

        // initialise ProbeSpecification context
        probeSpecContext.initialise(blackboard, new EventSimProbeStrategyRegistry(), new CalculatorFactory(this));

        // install a garbage collector which keeps track of the samples stored on the blackboard and
        // removes samples when they become obsolete
        IRegionBasedGarbageCollector<RequestContext> garbageCollector = new EventSimGarbageCollector(blackboard);
        probeSpecContext.setBlackboardGarbageCollector(garbageCollector);

        // build calculators
        this.execute(new BuildResponseTimeCalculators(this));
        this.execute(new BuildActiveResourceCalculators(this, this.resourceEnvironment));
        this.execute(new BuildPassiveResourceCalculators(this, this.passiveResourceRegistry));
        
        // calculators for actor step
        this.execute(new BuildActorStepExecutionTimeCalculators(this));
        // calculators for activity
        this.execute(new BuildActivityExecutionTimeCalculators(this));
        // calculators for actor resource
        this.execute(new BuildActorResourceCalculators(this, this.actorResourceRegistry));

        // mount probes
        this.execute(new MountUsageScenarioProbes(this.usageInterpreter.getConfiguration()));
        this.execute(new MountSystemCallProbes(this.usageInterpreter.getConfiguration()));
        this.execute(new MountExternalCallProbes(this.seffInterpreter.getConfiguration()));
        this.execute(new MountActiveResourceProbes(this, this.resourceEnvironment));
        this.execute(new MountPassiveResourceProbes(this, this.passiveResourceRegistry));
        
        // mount probes for actor step
        this.execute(new MountActorStepProbes(this.usageInterpreter.getConfiguration()));
        // mount probes for activity
        this.execute(new MountActivityProbes(this.usageInterpreter.getConfiguration()));
        // mount probes for actor resource
        this.execute(new MountActorResourceProbes(this, this.actorResourceRegistry));
    }

    private void setupStopConditions() {
        long MAX_MEASUREMENTS = getConfiguration().getMaxMeasurementsCount();
        long MAX_SIMULATION_TIME = getConfiguration().getSimuTime();

        if (MAX_MEASUREMENTS <= 0 && MAX_SIMULATION_TIME <= 0) {
        	if(logger.isDebugEnabled())
        		logger.debug("Deactivating maximum simulation time stop condition per user request");
            getSimulationControl().setMaxSimTime(0);
        } else {
        	if(logger.isDebugEnabled())
        		logger.debug("Enabling simulation stop condition at maximum simulation time of " + MAX_SIMULATION_TIME);
            if (MAX_SIMULATION_TIME > 0)
                getSimulationControl().setMaxSimTime(MAX_SIMULATION_TIME);
        }

        this.getSimulationControl().addStopCondition(new MaxMeasurementsStopCondition(this));

        // Add confidence stop condition if configured
        // if (model.getConfig().isUseConfidence()) {
        // double level = model.getConfig().getConfidenceLevel() / 100.0;
        // double halfWidth = model.getConfig().getConfidenceHalfWidth() / 100.0;
        // model.getSimulationControl().addStopCondition(
        // new ConfidenceStopCondition(model, new PhiMixingBatchAlgorithm(),
        // new SampleMeanEstimator(), level, halfWidth));
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalise() {
        // notify observers that the simulation is finished (below we just clean up...)
        this.notifyStopListeners();

        // notify active entities that the simulation is finished (and therefore, also their
        // existence in the simulated system)
        for (EventSimEntity entity : activeEntitiesList) {
            assert entity.getState().equals(EventSimEntity.EntityState.ENTERED_SYSTEM) : "Found an entity in the "
                    + "list of active entities which is in the state " + entity.getState()
                    + ", and therefore can not be an active entity.";
            entity.notifyLeftSystem();
        }
        assert activeEntitiesList.isEmpty() : "There are some entities left in the list of active entities, though "
                + "each of them was asked to leave the system.";

        // notify active resources about the simulation stop
        deactivateResources();

        // clean up
        if (logger.isDebugEnabled()) {
            logger.debug("Cleaning up...");
        }
        EventSimEntity.resetIdGenerator();
        probeSpecContext.finish();
        AbstractActiveResource.cleanProcesses();
        config.getRandomGenerator().dispose();

        if(logger.isEnabledFor(Level.INFO))
        	logger.info("Simulation took " + getSimulationControl().getCurrentSimulationTime() + " simulation seconds");
        
        // TODO
        // traceRecorder.print();
        // ((CountingPCMModelCommandExecutor)executor).printStatistics();
    }

    /**
     * Notifies all active resources that the simulation run has stopped.
     */
    private void deactivateResources() {
        for (SimulatedResourceContainer c : this.resourceEnvironment.getResourceContainers()) {
            for (SimActiveResource r : c.getResources()) {
                r.deactivateResource();
            }
        }
    }

    /**
     * Executes the specified command and returns the result.
     * 
     * @param <T>
     *            the return type
     * @param command
     *            the command that is to be executed
     * @return the command's result
     */
    public <T> T execute(final ICommand<T, PCMModel> command) {
        return this.executor.execute(command);
    }

    /**
     * Returns the resource environment comprising {@link SimulatedResourceContainer}.
     * 
     * @return the resource environment
     */
    public SimulatedResourceEnvironment getResourceEnvironment() {
        return this.resourceEnvironment;
    }

    /**
     * Returns the allocation of {@link AssemblyContext}s to {@link SimulatedResourceContainer}s.
     * 
     * @return a registry containing the resource allocations
     */
    public AllocationRegistry getResourceAllocation() {
        return this.resourceAllocation;
    }

    /**
     * Returns the component instance that is encapsulated by the specified assembly context.
     * 
     * @param assemblyContext
     *            the assembly context
     * @return the queried component instance
     */
    public ComponentInstance getComponent(final AssemblyContext assemblyContext) {
        return this.componentRegistry.get(assemblyContext.getId());
    }

    /**
     * Returns the registry for passive resources.
     * 
     * @return
     */
    public PassiveResourceRegistry getPassiveResourceRegistry() {
        return this.passiveResourceRegistry;
    }
    
    /**
     * Returns the registry for actor resource instances.
     * 
     * @return
     */
    public ActorResourceRegistry getActorResourceRegistry() {
        return this.actorResourceRegistry;
    }
    
    /**
     * Returns the registry for device resources.
     * 
     * @return
     */
    public DeviceResourceRegistry getDeviceResourceRegistry() {
        return this.deviceResourceRegistry;
    }

    /**
     * Increases the number of measurements, which has been taken, by one.
     * <p>
     * Notice that one measurements refers to one {@link User} that has completely traversed the
     * simulated system.
     */
    public void increaseMainMeasurementsCount() {
        this.mainMeasurementsCount++;
    }

    /**
     * Returns the number of measurements, which has been taken.
     * <p>
     * Notice that one measurements refers to one {@link User} that has completely traversed the
     * simulated system.
     * 
     * @return the current number of measurements
     */
    public long getMainMeasurementsCount() {
        return this.mainMeasurementsCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISimulationControl getSimulationControl() {
        if (this.simControl == null) {
            this.simControl = this.factory.createSimulationControl();
        }
        return this.simControl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISimEngineFactory getSimEngineFactory() {
        return this.factory;
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public void setSimulationControl(final ISimulationControl control) {
        this.simControl = control;
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public void setSimulationEngineFactory(final ISimEngineFactory factory) {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventSimConfig getConfiguration() {
        return this.config;
    }

    /**
     * Returns the execution context of this simulation run.
     * 
     * @return the bundle execution context
     */
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public ProbeSpecContext getProbeSpecContext() {
        return probeSpecContext;
    }

    public ISchedulingFactory getSchedulingFactory() {
        return schedulingFactory;
    }

    public EntityTraceRecorder getTraceRecorder() {
        return traceRecorder;
    }

    public UsageBehaviourInterpreter getUsageInterpreter() {
        return usageInterpreter;
    }

    public SeffBehaviourInterpreter getSeffInterpreter() {
        return seffInterpreter;
    }

    /**
     * Informs this simulation model about the creation of a new entity.
     * <p>
     * Once the registered entity has left the simulation system,
     * {@link #unregisterEntity(EventSimEntity)} should be called. Entities that were registered,
     * but have not been unregistered are deemed as active entities, i.e. they have not yet finished
     * simulating their behaviour. When the simulation is about to stop, the {@code finalise} method
     * notifies active entities of the imminent stop.
     * 
     * @param entity
     *            the entity that has just been spawned
     */
    public void registerEntity(EventSimEntity entity) {
        this.activeEntitiesList.add(entity);
    }

    /**
     * Informs this simulation model that the specified entity has finished to simulate its
     * behaviour and is about to leave the simulated system.
     * 
     * @param entity
     *            the entity that has just finished their simulated behaviour
     */
    public void unregisterEntity(EventSimEntity entity) {
        this.activeEntitiesList.remove(entity);
    }

    /**
     * Notfies all simulation observers that the simulation is about to start
     */
    private void notifyStartListeners() {
        for (final ISimulationListener l : this.config.getListeners()) {
            l.simulationStart();
        }
    }

    /**
     * Notfies all simulation observers that the simulation has stopped
     */
    private void notifyStopListeners() {
        for (final ISimulationListener l : this.config.getListeners()) {
            l.simulationStop();
        }
    }

    /**
     * Returns the PCM model that is to be simulated. If it has not been loaded before, this methods
     * loads the PCM model from the bundle.
     * 
     * @return the PCM model
     */
    private PCMModel getPCMModel() {
        if (this.pcmModel == null) {
            final IPath usageModelLocation = this.createRelativePathToModelFile(this.config.getUsageModelFile());
            final IPath allocationModelLocation = this.createRelativePathToModelFile(this.config
                    .getAllocationModelFile());
            this.pcmModel = PCMModel.loadFromBundle(this.bundleContext.getBundle(), usageModelLocation,
                    allocationModelLocation);
        }
        return this.pcmModel;
    }
    
    public OrganizationEnvironmentModel getOrganizationalModel() {
        if (this.organisationalModel == null) {
//            this.organisationalModel = BPPCMModel.loadFromBundle(this.bundleContext.getBundle(),
//                    this.createRelativePathToModelFile(ORGANIZATIONAL_MODEL_LOCATION));
        	this.organisationalModel = BPPCMModel.loadFromBundle(this.bundleContext.getBundle(),
                    ORGANIZATIONAL_MODEL_LOCATION);
        }

        return organisationalModel;
    }

    /**
     * Takes a path and converts it to a path that is relative to the bundle.
     * 
     * @param location
     *            the path
     * @return the relative path
     */
    private IPath createRelativePathToModelFile(final String location) {
        URI u = URI.createFileURI(location);
        String fileName = u.segment(u.segmentCount() - 1);

        final IPath path = new Path("model/" + fileName);
        return path;
    }
    
    public Dispatcher getDispatcher(){
    	return this.dispatcher;
    }

}
