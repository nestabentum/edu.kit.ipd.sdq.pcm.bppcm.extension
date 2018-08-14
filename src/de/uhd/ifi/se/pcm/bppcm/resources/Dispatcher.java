package de.uhd.ifi.se.pcm.bppcm.resources;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;

import com.google.inject.Inject;
import com.google.inject.Singleton;


import de.uhd.ifi.se.pcm.bppcm.NewEventSimClasses.IntBIISEventSimSystemModel;
import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ActorStep;
import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import de.uhd.ifi.se.pcm.bppcm.resources.entities.ActorResourceInstance;
import de.uhd.ifi.se.pcm.bppcm.resources.entities.RoleActorPair;
import de.uhd.ifi.se.pcm.bppcm.strategies.ActorStepTraversalStrategy;
import edu.kit.ipd.sdq.eventsim.api.IUser;
import edu.kit.ipd.sdq.eventsim.entities.IEntityListener;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;
import edu.kit.ipd.sdq.pcm.simulation.bpscheduler.SuspendableFCFSResource;


/**
 * Selects and returns an {@link ActorResource} to perform an {@link ActorStep}
 * 
 * @author Robert Heinrich
 *
 */
@Singleton
public class Dispatcher implements IDispatcher {
	
	private Hashtable<Long, RoleActorPair> dispatcherList;
	
	private static final Logger logger = Logger.getLogger(ActorStepTraversalStrategy.class);
	
	private IntBIISEventSimSystemModel model;
	
	@Inject 
	ActorResourceModel arm;
	
	private ActorResourceTracker tracker;
	
//	public Dispatcher(EventSimSystemModel model, ActorResourceTracker tracker){
//		this.model = model;
//		this.tracker = tracker;
//		this.dispatcherList = new Hashtable<Long, RoleActorPair>();
//	}
	
	public Dispatcher(IntBIISEventSimSystemModel model, ActorResourceTracker tracker) {
		this.model = model;
		this.tracker = tracker;
		this.dispatcherList = new Hashtable<Long, RoleActorPair>();
	}

	@Override
	public ActorResource dispatch(final User instance, ActorStep step) {
		
		instance.addEntityListener(new IEntityListener() {
			
			@Override
			public void leftSystem() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void enteredSystem() {
				if (dispatcherList.containsKey(instance.getEntityId())){
					// remove the entry for the instance that is finished
					dispatcherList.remove(instance.getEntityId());
				}
				
			}
		});
		
		EList<ActorResource> actors = step.getResponsibleRole().getActors();
		ActorResource selectedActor = null;
		
        Set<ActorResource> availableActors = tracker.getActiveActors();
        
        
        
        
        RoleActorPair pair = dispatcherList.get(instance.getEntityId());
        
        // check whether this is the first step in the process (no entry available in the dispatcherList)
        if(pair == null){
        	// select the actor with the shortest waiting queue to perform the step
    		selectedActor = selectActor(actors, availableActors);
			dispatcherList.put(instance.getEntityId(), new RoleActorPair(selectedActor, step.getResponsibleRole()));
    		logger.info("Actor "+selectedActor.getEntityName()+" was selected for step "+step.getEntityName()+" in process instance "+instance.getEntityId());
			
    		return selectedActor;
        }
        else{
        
	        selectedActor = pair.getActor();
	        
		    // if the flag continuouslyPerformed is set in the current actor step AND
	        // the role of the previous actor step is the same as the role of the current actor step AND
	        // the actor who performed the previous step is currently available
	        // then, select the actor of the previous actor step to perform the current step
	        if(step.isContinuouslyPerformed() && pair.getRole().equals(step.getResponsibleRole()) && availableActors.contains(selectedActor)){
	        	
	        	logger.info("Actor "+selectedActor.getEntityName()+" was selected for step "+step.getEntityName()+" in process instance "+instance.getEntityId());
				// return the actor of the previous actor step
				return selectedActor;
	        	
	        }
	        else{
	        	// select the actor with the shortest waiting queue to perform the step
	    		selectedActor = selectActor(actors, availableActors);
	    		logger.info("Actor "+selectedActor.getEntityName()+" was selected for step "+step.getEntityName()+" in process instance "+instance.getEntityId());
	    		
	    		// update dispatcherList
	    		dispatcherList.remove(instance.getEntityId());
	    		dispatcherList.put(instance.getEntityId(), new RoleActorPair(selectedActor, step.getResponsibleRole()));
	    		
	    		return selectedActor;
	        	
	        }
        }
        
	}
	
	/**
	 * Selects an actor to perform the {@link ActorStep}
	 * @param allActors
	 * @param availableActors
	 * @param model
	 * @return the selected actor
	 */
	private ActorResource selectActor(EList<ActorResource> allActors, Collection<ActorResource> availableActors){
		
		ActorResource selectedActor = null;
		
		// select the actor with the shortest waiting queue
        // what if there is no actor available at all?
        if (availableActors.isEmpty()){
        	
        	// select the actor with the shortest waiting queue from the unavailalbe actors
        	// other strategies are possible
        	selectedActor = findActorWithShortestQueue(allActors);
        }
        else{
        
        	// select the actor with the shortest waiting queue from the available actors
        	selectedActor = findActorWithShortestQueue(availableActors);
        }
        
        return selectedActor;
		
	}
	
	private ActorResource findActorWithShortestQueue (Collection<ActorResource> actors){
		ActorResource selectedActor = null;
		double selectedDemand = Double.MAX_VALUE;
		
		// select the actor with the shortest waiting queue		
		for (ActorResource j : actors) {
			ActorResourceInstance ari = arm.findOrRegisterActorResourceInstance(j);
			double demand = getCurrentDemand(ari);
			
			if(demand < selectedDemand) {
				selectedActor = j;
				selectedDemand = demand;
			}
		}
        
        return selectedActor;
	}
	
	private double getCurrentDemand(ActorResourceInstance ari){
		return ((SuspendableFCFSResource)ari.getResource().getSchedulerResource()).getRemainingDemand();
	}

}
