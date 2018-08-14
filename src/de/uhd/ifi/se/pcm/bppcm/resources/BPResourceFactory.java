package de.uhd.ifi.se.pcm.bppcm.resources;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import de.uhd.ifi.se.pcm.bppcm.NewEventSimClasses.IntBIISEventSimSystemModel;
import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.WorkingPeriod;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.CompositionFactory;
import org.palladiosimulator.pcm.repository.PassiveResource;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentFactory;
import org.palladiosimulator.pcm.resourcetype.ResourceRepository;
import org.palladiosimulator.pcm.resourcetype.SchedulingPolicy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.uka.ipd.sdq.scheduler.IPassiveResource;
import de.uka.ipd.sdq.scheduler.SchedulerModel;

import de.uka.ipd.sdq.simucomframework.variables.StackContext;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEventDelegator;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationModel;
import edu.kit.ipd.sdq.eventsim.resources.ResourceFactory;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimActiveResource;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimPassiveResource;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimResourceFactory;
import edu.kit.ipd.sdq.pcm.simulation.bpscheduler.SuspendableFCFSResource;



/**
 * Factory for creating active and passive BP resources
 * 
 * @author Robert Heinrich
 * 
 */
@Singleton
public class BPResourceFactory {
	
	@Inject
	private ResourceFactory resourceFactory;
	
	@Inject 
	private BPSimResourceFactory simResourceFactory;
	

    private SchedulerModel model;
    
    
    @Inject
    public BPResourceFactory(SchedulerModel model) {
        this.model = model;
    }
    
    
	/**
     * Creates an active resource in accordance with the given resource specification.
     * 
     * @param model
     *            the simulation model
     * @param specification
     *            the resource specification
     * @return the created resource
     */
    public SimActiveResource createActiveResource(final IntBIISEventSimSystemModel model, ActorResource specification) {
  
    	ProcessingResourceSpecification spec = ResourceenvironmentFactory.eINSTANCE.createProcessingResourceSpecification();
    	    	
    	// load scheduling policy from resource type model
    	ResourceSet resSet = new ResourceSetImpl();
    	Resource resource = resSet.getResource(URI.createURI("pathmap://BUSINESS_PROCESS_SCHEDULER_MODELS/BusinessProcessSchedulers.resourcetype"), true);
    	ResourceRepository repository = (ResourceRepository) resource.getContents().get(0); 
    	SchedulingPolicy schedulingPolicy = repository.getSchedulingPolicies__ResourceRepository().get(0);
    	
        // set default values of attributes not contained in human actor specification
    	spec.setNumberOfReplicas(1);
    	final PCMRandomVariable processingRate = CoreFactory.eINSTANCE.createPCMRandomVariable();
    	processingRate.setSpecification("1");
    	spec.setProcessingRate_ProcessingResourceSpecification(processingRate);
    	spec.setSchedulingPolicy(schedulingPolicy);
   
    	// call the original resource factory 
    	SimActiveResource result = resourceFactory.createActiveResource(spec);
    	
    	
    	SuspendableFCFSResource x = (SuspendableFCFSResource)result.getSchedulerResource();
    	
    	// SuspendEvent is fired at the beginning of the simulation
    	new SuspendEvent(model.getMiddleware().getSimulationModel(), "suspend", specification.getWorkingPeriods()).schedule(x,0);
    	
    	return result;
    }
    
    /**
     * Creates a SimPassiveResource in accordance with the given DeviceResource specification.
     * 
     * @param model
     *            the simulation model
     * @param specification
     *            the DeviceResource specification
     * 
     * Note, the assembly context is neglected for device resources. It is assumed that the context is not required.
     * 
     */
    public SimDeviceResource createDeviceResource(final DeviceResource specification) {
        // obtain capacity by evaluating the associated StoEx
        final PCMRandomVariable capacitySpecification = specification.getCapacity();
        final int capacity = StackContext.evaluateStatic(capacitySpecification.getSpecification(), Integer.class);

        final String name = specification.getEntityName();
        final String resourceId = specification.getId();
        final String combinedId = resourceId;

       
        
        
        IPassiveResource schedulerResource = new SimSimpleFairPassiveDeviceResource(specification, model, capacity, name, combinedId);



        return simResourceFactory.createDeviceResource(schedulerResource, specification);
        		
    }
    
    
    /**
     * Creates a passive resource in accordance with the given resource specification.
     * 
     * @param model
     *            the simulation model
     * @param specification
     *            the resource specification
     * @return the created resource
     * 
     * 
     * TODO SimSimpleFairPassiveResource not usable like this. But not found any usage of this method in old IntBIIS project
     * 
     */
//    public SimPassiveResource createPassiveResource(final EventSimModel model,
//            final DeviceResource specification) {
//    	
//    	// obtain capacity by evaluating the associated StoEx
//        final PCMRandomVariable capacitySpecification = specification.getCapacity();
//        final int capacity = StackContext.evaluateStatic(capacitySpecification.getSpecification(), Integer.class);
//
//        final String name = specification.getEntityName();
//        final String resourceId = specification.getId();
//        // set dummy assembly context id
//        final String assemblyContextId = "BusinessProcessContext";
//        final String combinedId = specification.getId();
//        AssemblyContext assemblyContext = CompositionFactory.eINSTANCE.createAssemblyContext();
//        assemblyContext.setId("BusinessProsceeContext");
//        IPassiveResource schedulerResource = new SimSimpleFairPassiveResource(model, capacity, name, resourceId, assemblyContextId, combinedId, false);
//        return new SimPassiveResource(model, schedulerResource);
//    }
    
    private static class SuspendEvent extends AbstractSimEventDelegator<SuspendableFCFSResource> {
    	
    	private EList<WorkingPeriod> periods;

        protected SuspendEvent(ISimulationModel model, String name, EList<WorkingPeriod> periods) {
            super(model, name);
            this.periods = periods;
        }

        @Override
        public void eventRoutine(SuspendableFCFSResource resource) {
            resource.suspend();
            
            double currentTime = this.getModel().getSimulationControl().getCurrentSimulationTime();
            double nextResume = 0;
            for (WorkingPeriod p: periods){
            	// as we are in suspend state we know that we are between two working periods
            	// find the start time of the working period next to the current simulation time
            	if (currentTime <= p.getPeriodStartTimePoint()){
            		
            		nextResume = p.getPeriodStartTimePoint() - currentTime;
            		new ResumeEvent(getModel(), "resume", periods).schedule(resource, nextResume);
            		
            		break;
            	}
            	
            }
            
        }

    }

    private static class ResumeEvent extends AbstractSimEventDelegator<SuspendableFCFSResource> {

    	private EList<WorkingPeriod> periods;
    	
        protected ResumeEvent(ISimulationModel model, String name, EList<WorkingPeriod> periods) {
            super(model, name);
            this.periods = periods;
        }

        @Override
        public void eventRoutine(SuspendableFCFSResource resource) {
            resource.resume();
            
            double currentTime = this.getModel().getSimulationControl().getCurrentSimulationTime();
            double nextSuspend = 0;
            
            for (WorkingPeriod p: periods){
            	// as we are in running state we know that we are in a working period
            	// find the working period
            	if ((currentTime >= p.getPeriodStartTimePoint()) && (currentTime<= p.getPeriodEndTimePoint())){
            		
            		// suspend at the end of the working period
            		nextSuspend = p.getPeriodEndTimePoint() - currentTime;
            		new SuspendEvent(getModel(), "suspend", periods).schedule(resource, nextSuspend);
            		
            		break;
            	}
            	
            }
            
        }

    }
    

}
