package de.uhd.ifi.se.pcm.bppcm.resources;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.WorkingPeriod;
import de.uhd.ifi.se.pcm.bppcm.resources.entities.ActorResourceInstance;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEventDelegator;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationModel;

public class ActorResourceTracker {

	private ISimulationModel model;
	
	private Set<ActorResource> activeActors;
	
	public ActorResourceTracker(ISimulationModel model) {
		this.model = model;
		activeActors = new HashSet<ActorResource>();
	}
	
	public void track(Collection<ActorResourceInstance> actors) {
		for(ActorResourceInstance r : actors) {
			for(WorkingPeriod p : r.getSpecification().getWorkingPeriods()) {
				double startTime = p.getPeriodStartTimePoint();
				double endTime = p.getPeriodEndTimePoint();
		
				assert(model.getSimulationControl().getCurrentSimulationTime() == 0);
			
				new ActorBeginsWorkingPeriod(model).schedule(r, startTime);
				new ActorEndsWorkingPeriod(model).schedule(r, endTime);
			}
		}
	}
	
	
	
	public Set<ActorResource> getActiveActors() {
		return activeActors;
	}



	private class ActorBeginsWorkingPeriod extends AbstractSimEventDelegator<ActorResourceInstance> {

		protected ActorBeginsWorkingPeriod(ISimulationModel model) {
			super(model, "Actor begins working period");
		}

		@Override
		public void eventRoutine(ActorResourceInstance who) {
			activeActors.add(who.getSpecification());
		}
		
	}
	
	private class ActorEndsWorkingPeriod extends AbstractSimEventDelegator<ActorResourceInstance> {

		protected ActorEndsWorkingPeriod(ISimulationModel model) {
			super(model, "Actor ends working period");
		}

		@Override
		public void eventRoutine(ActorResourceInstance who) {
			activeActors.remove(who.getSpecification());
		}
		
	}
	
}
