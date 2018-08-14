package de.uhd.ifi.se.pcm.bppcm.NewEventSimClasses;

import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.Interface;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.BpusagemodelPackage;
import de.uhd.ifi.se.pcm.bppcm.command.BuildAndRegisterActorResources;
import de.uhd.ifi.se.pcm.bppcm.core.BPPCMModel;
import de.uhd.ifi.se.pcm.bppcm.command.BuildAndRegisterDeviceResources;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.OrganizationEnvironmentModel;
import de.uhd.ifi.se.pcm.bppcm.resources.ActorResourceTracker;
import de.uhd.ifi.se.pcm.bppcm.resources.Dispatcher;
import de.uhd.ifi.se.pcm.bppcm.resources.IActorResource;
import de.uhd.ifi.se.pcm.bppcm.resources.IDeviceResource;
import edu.kit.ipd.sdq.eventsim.api.IActiveResource;
import edu.kit.ipd.sdq.eventsim.api.IPassiveResource;
import edu.kit.ipd.sdq.eventsim.api.ISimulationMiddleware;
import edu.kit.ipd.sdq.eventsim.api.ISystem;
import edu.kit.ipd.sdq.eventsim.api.IUser;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.api.Procedure;
import edu.kit.ipd.sdq.eventsim.api.events.SimulationPrepareEvent;
import edu.kit.ipd.sdq.eventsim.api.events.SimulationStopEvent;
import edu.kit.ipd.sdq.eventsim.api.events.SystemRequestFinishedEvent;
import edu.kit.ipd.sdq.eventsim.api.events.SystemRequestSpawnEvent;
import edu.kit.ipd.sdq.eventsim.api.events.IEventHandler.Registration;
import edu.kit.ipd.sdq.eventsim.command.ICommand;
import edu.kit.ipd.sdq.eventsim.command.PCMModelCommandExecutor;
import edu.kit.ipd.sdq.eventsim.instrumentation.description.action.ActionRepresentative;
import edu.kit.ipd.sdq.eventsim.instrumentation.description.core.InstrumentationDescription;
import edu.kit.ipd.sdq.eventsim.instrumentation.injection.Instrumentor;
import edu.kit.ipd.sdq.eventsim.instrumentation.injection.InstrumentorBuilder;
import edu.kit.ipd.sdq.eventsim.interpreter.TraversalListenerRegistry;
import edu.kit.ipd.sdq.eventsim.measurement.MeasurementFacade;
import edu.kit.ipd.sdq.eventsim.measurement.MeasurementStorage;
import edu.kit.ipd.sdq.eventsim.measurement.osgi.BundleProbeLocator;
import edu.kit.ipd.sdq.eventsim.system.Activator;
import edu.kit.ipd.sdq.eventsim.system.EventSimSystemModel;
import edu.kit.ipd.sdq.eventsim.system.SystemMeasurementConfiguration;
import edu.kit.ipd.sdq.eventsim.system.entities.ForkedRequest;
import edu.kit.ipd.sdq.eventsim.system.entities.Request;
import edu.kit.ipd.sdq.eventsim.system.entities.RequestFactory;
import edu.kit.ipd.sdq.eventsim.system.staticstructure.AllocationRegistry;
import edu.kit.ipd.sdq.eventsim.system.staticstructure.ComponentInstance;
import edu.kit.ipd.sdq.eventsim.system.staticstructure.SimulatedResourceEnvironment;



/*TODO 
 * This class is nearly a duplicate to the EventSimSystemModel class. Only difference is the inclusion of the IntBIISFacade
 * to access additional IntBIIS functionality. Is there a cleaner way to implement new Domains? 
*/
@Singleton
public class IntBIISEventSimSystemModel implements ISystem{
	


	private static final Logger logger = Logger.getLogger(IntBIISEventSimSystemModel.class);
	 	@Inject
	    private IActiveResource activeResource;

	    @Inject
	    private IPassiveResource passiveResource;

	    @Inject
	    private PCMModelCommandExecutor executor;

	    @Inject
	    private MeasurementStorage measurementStorage;

	    @Inject
	    private ISimulationMiddleware middleware;

	    @Inject
	    private TraversalListenerRegistry<AbstractAction, Request> traversalListeners;

	    @Inject
	    private PCMModel pcm;

	    @Inject
	    private InstrumentationDescription instrumentation;

	    @Inject
	    private RequestFactory requestFactory;

	    private MeasurementFacade<SystemMeasurementConfiguration> measurementFacade;

	    private SimulatedResourceEnvironment resourceEnvironment;
	    private AllocationRegistry resourceAllocation;
	    private Map<String, ComponentInstance> componentRegistry;
	    
	    
	    //IntBIISStuff - No Facade first
		@Inject 
		IActorResource actorResourceModel;
		
		@Inject 
		IDeviceResource deviceResourceModel;
		
		Dispatcher dispatcher;
		
		@Inject
		ActorResourceTracker tracker;
	    
		 // FIXME
	    private static final IPath ORGANIZATIONAL_MODEL_LOCATION = new Path("model/My.organizationenvironmentmodel");
	    private OrganizationEnvironmentModel organisationalModel;
	   
		@Inject
		public IntBIISEventSimSystemModel(ISimulationMiddleware middleware) {
			middleware.registerEventHandler(SimulationPrepareEvent.class, e -> {
	            init();
	            return Registration.UNREGISTER;
	        });
		}
	    
		private void init() {

			 // install debug traversal listeners, if debugging is enabled
	        if (logger.isDebugEnabled()) {
	            traversalListeners.addTraversalListener(new DebugSeffTraversalListener());
	        }

	        this.setupMeasurements();
	        BpusagemodelPackage.eINSTANCE.getProcessWorkload_ProcessTriggerPeriods();
	        
	        // initialise resource environment and allocation
	        this.resourceEnvironment = executor.execute(new BuildSimulatedResourceEnvironment());
	        this.resourceAllocation = executor.execute(new BuildResourceAllocation(this.resourceEnvironment));

	        // initialise component instances
	        this.componentRegistry = executor.execute(new BuildComponentInstances(this.resourceAllocation));

	        // install extern call parameter handling
	        executor.execute(new InstallExternalCallParameterHandling(traversalListeners));

	        
	       
	        //initialize IntBIIS Stuff
	    	this.dispatcher = new Dispatcher(this, tracker);
	    	
	    	 // initialise actor resource instances
	    	this.execute(new BuildAndRegisterActorResources(this));
	       
	    	
	    	 // track active actor resouces
			ActorResourceTracker tracker = new ActorResourceTracker(middleware.getSimulationModel());
			tracker.track(actorResourceModel.getAllActorResourceInstances());
			
			// initialize actor resource dispatcher
	        this.dispatcher = new Dispatcher(this, tracker);
	        
	        // initialise device resource instances
	        this.execute(new BuildAndRegisterDeviceResources(this));
	        

	        registerEventHandler();
		}

		   /**
	     * Register event handler to react on specific simulation events.
	     */
		private void registerEventHandler() {
	        middleware.registerEventHandler(SimulationStopEvent.class, e -> {
	            finalise();
	            return Registration.UNREGISTER;
	        });

	        // setup system call parameter handling
	        middleware.registerEventHandler(SystemRequestSpawnEvent.class,
	        		new BeforeSystemCallParameterHandler(this, executor));
	        middleware.registerEventHandler(SystemRequestFinishedEvent.class, new AfterSystemCallParameterHandler());
	    }
		
			 private void setupMeasurements() {
			        // create instrumentor for instrumentation description
			        // TODO get rid of cast
			        Instrumentor<?, ?> instrumentor = InstrumentorBuilder.buildFor(pcm).inBundle(Activator.getContext().getBundle())
			                .withDescription(instrumentation).withStorage(measurementStorage)
			                .forModelType(ActionRepresentative.class).withoutMapping().createFor(getMeasurementFacade());
			        instrumentor.instrumentAll();

			        measurementStorage.addIdExtractor(Request.class, c -> Long.toString(((Request) c).getId()));
			        measurementStorage.addNameExtractor(Request.class, c -> ((Request) c).getName());
			        measurementStorage.addIdExtractor(ForkedRequest.class, c -> Long.toString(((ForkedRequest) c).getEntityId()));
			        measurementStorage.addNameExtractor(ForkedRequest.class, c -> ((ForkedRequest) c).getName());
			        measurementStorage.addIdExtractor(Entity.class, c -> ((Entity) c).getId());
			        measurementStorage.addNameExtractor(Entity.class, c -> ((Entity) c).getEntityName());
			        measurementStorage.addNameExtractor(ExternalCallAction.class, c -> {
			            ExternalCallAction action = (ExternalCallAction) c;
			            OperationSignature calledSignature = action.getCalledService_ExternalService();
			            Interface calledInterface = calledSignature.getInterface__OperationSignature();
			            return calledInterface.getEntityName() + "." + calledSignature.getEntityName();
			        });
			    }

		 private void finalise() {
		        // TODO really?
		        // nothing to do, currently
		    }
		@Override
		public void callService(IUser user, EntryLevelSystemCall call, Procedure onFinishCallback) {
			 // find the component which provides the call
	        final AssemblyContext assemblyCtx = executor.execute(new FindAssemblyContextForSystemCall(call));
	        final ComponentInstance component = this.getComponent(assemblyCtx);
	        final OperationSignature signature = call.getOperationSignature__EntryLevelSystemCall();
	        final ResourceDemandingBehaviour behaviour = component.getServiceEffectSpecification(signature);

	        // spawn a new EventSim request
	        final Request request = requestFactory.createRequest(call, user);

	        // simulate request
	        request.simulateBehaviour(behaviour, component, onFinishCallback);
		}
		
		 public MeasurementFacade<SystemMeasurementConfiguration> getMeasurementFacade() {
		        if (measurementFacade == null) {
		            // setup measurement facade
		            Bundle bundle = Activator.getContext().getBundle(); //TODO where does the context come from?
		            measurementFacade = new MeasurementFacade<>(new SystemMeasurementConfiguration(traversalListeners),
		                    new BundleProbeLocator<>(bundle));
		        }
		        return measurementFacade;
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

		    public OrganizationEnvironmentModel getOrganizationalModel() {
		        if (this.organisationalModel == null) {
//		            this.organisationalModel = BPPCMModel.loadFromBundle(this.bundleContext.getBundle(),
//		                    this.createRelativePathToModelFile(ORGANIZATIONAL_MODEL_LOCATION));
		        	this.organisationalModel = BPPCMModel.loadFromBundle(Activator.getContext().getBundle(),
		                    ORGANIZATIONAL_MODEL_LOCATION);
		        }

		        return organisationalModel;
		    }
		    
		 
		    public ISimulationMiddleware getMiddleware(){
		    	return this.middleware;
		    }
		    
		    
}
