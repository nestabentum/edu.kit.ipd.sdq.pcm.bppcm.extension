package de.uhd.ifi.se.pcm.bppcm.resources;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourcetype.ResourceInterface;
import org.palladiosimulator.pcm.resourcetype.ResourceType;

import com.google.inject.Inject;

import de.uhd.ifi.se.pcm.bppcm.NewEventSimClasses.IntBIISEventSimSystemModel;
import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ActorStep;
import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import de.uhd.ifi.se.pcm.bppcm.probes.configurations.ActorResourceProbeConfiguration;
import de.uhd.ifi.se.pcm.bppcm.resources.ActorResourceRegistry;
import de.uhd.ifi.se.pcm.bppcm.resources.entities.ActorResourceInstance;
import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;
import edu.kit.ipd.sdq.eventsim.api.IRequest;
import edu.kit.ipd.sdq.eventsim.api.ISimulationMiddleware;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.api.Procedure;
import edu.kit.ipd.sdq.eventsim.api.events.SimulationPrepareEvent;
import edu.kit.ipd.sdq.eventsim.api.events.SimulationStopEvent;
import edu.kit.ipd.sdq.eventsim.api.events.IEventHandler.Registration;
import edu.kit.ipd.sdq.eventsim.instrumentation.description.core.InstrumentationDescription;
import edu.kit.ipd.sdq.eventsim.instrumentation.description.resource.ActiveResourceRep;
import edu.kit.ipd.sdq.eventsim.instrumentation.injection.Instrumentor;
import edu.kit.ipd.sdq.eventsim.instrumentation.injection.InstrumentorBuilder;
import edu.kit.ipd.sdq.eventsim.measurement.MeasurementFacade;
import edu.kit.ipd.sdq.eventsim.measurement.MeasurementStorage;
import edu.kit.ipd.sdq.eventsim.measurement.osgi.BundleProbeLocator;
//import edu.kit.ipd.sdq.eventsim.measurement.osgi.BundleProbeLocator;
import edu.kit.ipd.sdq.eventsim.resources.Activator;
import edu.kit.ipd.sdq.eventsim.resources.ActiveResourceRegistry;
import edu.kit.ipd.sdq.eventsim.resources.EventSimActiveResourceModel;
import edu.kit.ipd.sdq.eventsim.resources.ProcessRegistry;
import edu.kit.ipd.sdq.eventsim.resources.ResourceProbeConfiguration;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimActiveResource;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimulatedProcess;
import edu.kit.ipd.sdq.pcm.simulation.bpscheduler.ISuspendableSchedulableProcess;

public class ActorResourceModel implements IActorResource {
	 private static final Logger logger = Logger.getLogger(ActorResourceModel.class);

	    private Map<ResourceInterface, ResourceType> resourceInterfaceToTypeMap;

	    private Instrumentor<ActorResourceInstance, ?> instrumentor;

	    @Inject
	    private MeasurementStorage measurementStorage;

	    @Inject
	    private PCMModel pcm;

	    private MeasurementFacade<ResourceProbeConfiguration> measurementFacade;

	    @Inject
	    private InstrumentationDescription instrumentation;

	    @Inject
	    private ProcessRegistry processRegistry;
	     
	    @Inject 
	    private ActorResourceRegistry actorRegistry;
	    
	    @Inject
	    private IntBIISEventSimSystemModel model;
	    
	    @Inject
	    public ActorResourceModel(ISimulationMiddleware middleware) {
	        // initialize in simulation preparation phase
	        middleware.registerEventHandler(SimulationPrepareEvent.class, e -> {
	            init();
	            return Registration.UNREGISTER;
	        });
	        // finalize on simulation stop
	        middleware.registerEventHandler(SimulationStopEvent.class, e -> {
	            finalise();
	            return Registration.UNREGISTER;
	        });
	        resourceInterfaceToTypeMap = new HashMap<>();
	    }
	    
	    public void init() {
	        // setup measurement facade
	        Bundle bundle = Activator.getContext().getBundle();
	        //TODO Make measurement work
	       measurementFacade = new MeasurementFacade<>(new ResourceProbeConfiguration(),
	    		   new BundleProbeLocator<>(bundle));

	        // add hints for extracting IDs and names
	        measurementStorage.addIdExtractor(ActorResourceInstance.class, c -> ((ActorResourceInstance) c).getResource().getId());
	        measurementStorage.addNameExtractor(ActorResourceInstance.class, c -> ((ActorResourceInstance) c).getName());
	        measurementStorage.addIdExtractor(SimulatedProcess.class,
	                c -> Long.toString(((SimulatedProcess) c).getEntityId()));
	        measurementStorage.addNameExtractor(SimulatedProcess.class, c -> ((SimulatedProcess) c).getName());

	        // create instrumentor for instrumentation description
	        instrumentor = InstrumentorBuilder.buildFor(pcm).inBundle(Activator.getContext().getBundle())
	                .withDescription(instrumentation).withStorage(measurementStorage).forModelType(ActiveResourceRep.class)
	                .withMapping(
	                        (ActorResourceInstance r) -> new ActiveResourceRep(r.getResource().getResourceContainer(), r.getResource().getResourceType()))
	                .createFor(measurementFacade);
	        
	        //TODO do this for actorResourceRegistry
	        // instrument newly created resources
	        	actorRegistry.addResourceRegistrationListener(resource -> {
	        		//create probes and calculators (if requested by instrumentation description)
	        		instrumentor.instrument(resource);
	        	});
	    }
	    
	    public void finalise() {
	        actorRegistry.finalise();

	        // clean up scheduler
	        AbstractActiveResource.cleanProcesses();
	    }
	    
	    @Override
	    public void consume(final ActorResource specification, final IRequest request, final double absoluteDemand, final int resourceServiceID,
	            Procedure onServedCallback) {
	    	
	    	
	    	final SimActiveResource resource = actorRegistry.getActorResourceForContext(specification).getResource();
	    	
	    	resource.consumeResource(processRegistry.getOrCreateSimulatedProcess(request), absoluteDemand, resourceServiceID, onServedCallback);
	    	
//	        if (resource == null) {
//	            throw new RuntimeException("Could not find a resource of type " + resourceType.getEntityName());
//	        }

	    }
	    
	    public void setInterruptable(IRequest request, ActorStep action){
	    	((ISuspendableSchedulableProcess)processRegistry.getOrCreateSimulatedProcess(request)).setInterruptable(action.isInterruptable());
	    }

		@Override
		public ActorResourceInstance findOrRegisterActorResourceInstance(ActorResource specification) {
			return this.actorRegistry.findOrCreateActorResourceInstance(specification, model);
		}

		public Collection<ActorResourceInstance> getAllActorResourceInstances(){
			return actorRegistry.getAllActorResourceInstances();
		}

	    

}
