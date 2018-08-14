package de.uhd.ifi.se.pcm.bppcm.resources;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ActorStep;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;

/**
 * The Dispatcher selects an actor to perform an actor step
 * 
 * @author Robert Heinrich
 *
 */
public interface IDispatcher {

	public ActorResource dispatch(User instance, ActorStep step);
	
}
