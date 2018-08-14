package de.uhd.ifi.se.pcm.bppcm.workload;

import java.util.List;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;

import com.google.inject.Inject;

import de.uhd.ifi.se.pcm.bppcm.workload.generator.BuildBPWorkloadGenerator;
import de.uka.ipd.sdq.probfunction.math.IProbabilityFunctionFactory;
import de.uka.ipd.sdq.probfunction.math.impl.ProbabilityFunctionFactoryImpl;
import de.uka.ipd.sdq.simucomframework.variables.cache.StoExCache;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationModel;
import edu.kit.ipd.sdq.eventsim.api.ISimulationMiddleware;
import edu.kit.ipd.sdq.eventsim.api.IWorkload;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.api.events.WorkloadUserFinishedEvent;
import edu.kit.ipd.sdq.eventsim.api.events.IEventHandler.Registration;
import edu.kit.ipd.sdq.eventsim.api.events.SimulationPrepareEvent;
import edu.kit.ipd.sdq.eventsim.command.PCMModelCommandExecutor;
import edu.kit.ipd.sdq.eventsim.instrumentation.description.core.InstrumentationDescription;
import edu.kit.ipd.sdq.eventsim.instrumentation.description.useraction.UserActionRepresentative;
import edu.kit.ipd.sdq.eventsim.instrumentation.injection.Instrumentor;
import edu.kit.ipd.sdq.eventsim.instrumentation.injection.InstrumentorBuilder;
import edu.kit.ipd.sdq.eventsim.interpreter.TraversalListenerRegistry;
import edu.kit.ipd.sdq.eventsim.measurement.MeasurementFacade;
import edu.kit.ipd.sdq.eventsim.measurement.MeasurementStorage;
import edu.kit.ipd.sdq.eventsim.measurement.osgi.BundleProbeLocator;
import edu.kit.ipd.sdq.eventsim.measurement.probe.IProbe;
import edu.kit.ipd.sdq.eventsim.workload.Activator;
import edu.kit.ipd.sdq.eventsim.workload.EventSimWorkloadModel;
import edu.kit.ipd.sdq.eventsim.workload.WorkloadMeasurementConfiguration;
import edu.kit.ipd.sdq.eventsim.workload.debug.DebugUsageTraversalListener;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;
import edu.kit.ipd.sdq.eventsim.workload.generator.WorkloadGenerator;
;

//This is a replica of EventSimWorkloadModel to intigrate the BP Capabilities
public class BPWorkloadModel extends EventSimWorkloadModel{
	  private static final Logger logger = Logger.getLogger(EventSimWorkloadModel.class);

	    @Inject
	    private ISimulationMiddleware middleware;

	    @Inject
	    private MeasurementStorage measurementStorage;

	    @Inject
	    private PCMModelCommandExecutor executor;

	    @Inject
	    private ISimulationModel model;

	    @Inject
	    private TraversalListenerRegistry<AbstractUserAction, User> traversalListeners;

	    @Inject
	    private BuildBPWorkloadGenerator workloadGeneratorBuilder;

	    @Inject
	    private PCMModel pcm;

	    @Inject
	    private InstrumentationDescription instrumentation;

	    private MeasurementFacade<WorkloadMeasurementConfiguration> measurementFacade;
	    
	    @Inject
	    public BPWorkloadModel(ISimulationMiddleware middleware) {
	        // initialize in simulation preparation phase
	    	super(middleware);
	    	
	        middleware.registerEventHandler(SimulationPrepareEvent.class, e -> {
	            init();
	            return Registration.UNREGISTER;
	        });
	    }
	    
	    /**
	     * This method prepares the EventSim workload simulator and creates the initial events to start
	     * the workload generation.
	     */
	    private void init() {
	        // initialise probfunction factory and random generator
	        IProbabilityFunctionFactory probFunctionFactory = ProbabilityFunctionFactoryImpl.getInstance();
	        probFunctionFactory.setRandomGenerator(middleware.getRandomGenerator());
	        StoExCache.initialiseStoExCache(probFunctionFactory);

	        // install debug traversal listeners, if debugging is enabled
	        if (logger.isDebugEnabled()) {
	            traversalListeners.addTraversalListener(new DebugUsageTraversalListener());
	        }

	        setupMeasurements();

	        registerEventHandler();

	        generate();
	    }
	    
		@Override
		public void generate() {
			 // start the simulation by generating the workload
	        final List<WorkloadGenerator> workloadGenerators = executor.execute(workloadGeneratorBuilder);
	        for (final WorkloadGenerator d : workloadGenerators) {
	            d.processWorkload();
	        }
			
		}
		   /**
	     * Register event handler to react on specific simulation events.
	     */
	    private void registerEventHandler() {
	        middleware.registerEventHandler(WorkloadUserFinishedEvent.class, e -> {
	            middleware.increaseMeasurementCount();
	            return Registration.KEEP_REGISTERED;
	        });
	    }
		 private void setupMeasurements() {
		        // create instrumentor for instrumentation description
		        // TODO get rid of cast (and middleware/simulation dependencies)
		        Instrumentor<?, ?> instrumentor = InstrumentorBuilder.buildFor(pcm).inBundle(Activator.getContext().getBundle())
		                .withDescription(instrumentation).withStorage(measurementStorage)
		                .forModelType(UserActionRepresentative.class).withoutMapping().createFor(getMeasurementFacade());
		        instrumentor.instrumentAll();

		        // setup inter-arrival/-departure time probe for each UsageScenario in the usage model
		        // TODO allow to switch off via launch configuration
		        pcm.getUsageModel().getUsageScenario_UsageModel().forEach(scenario -> {
		            // setup inter-arrival time probe
		            IProbe<?> interArrivalProbe = getMeasurementFacade().createProbe(scenario, "inter_arrival_time");
		            interArrivalProbe.forEachMeasurement(m -> measurementStorage.put(m));

		            // setup inter-departure time probe
		            IProbe<?> interDepartureProbe = getMeasurementFacade().createProbe(scenario, "inter_departure_time");
		            interDepartureProbe.forEachMeasurement(m -> measurementStorage.put(m));

		            // setup active users probe
		            IProbe<?> activeUsersProbe = getMeasurementFacade().createProbe(scenario, "active_users");
		            activeUsersProbe.forEachMeasurement(m -> measurementStorage.put(m));
		        });

		        measurementStorage.addIdExtractor(User.class, c -> Long.toString(((User) c).getEntityId()));
		        measurementStorage.addNameExtractor(User.class, c -> ((User) c).getName());
		        measurementStorage.addIdExtractor(AbstractUserAction.class, c -> ((AbstractUserAction) c).getId());
		        measurementStorage.addNameExtractor(AbstractUserAction.class, c -> ((AbstractUserAction) c).getEntityName());
		    }

		    public MeasurementFacade<WorkloadMeasurementConfiguration> getMeasurementFacade() {
		        if (measurementFacade == null) {
		            // setup measurement facade
		            Bundle bundle = Activator.getContext().getBundle();
		            measurementFacade = new MeasurementFacade<>(new WorkloadMeasurementConfiguration(this, middleware, model),
		                    new BundleProbeLocator<>(bundle));
		        }
		        return measurementFacade;
		    }

		    public TraversalListenerRegistry<AbstractUserAction, User> getTraversalListeners() {
		        return traversalListeners;
		    }
	    
}
