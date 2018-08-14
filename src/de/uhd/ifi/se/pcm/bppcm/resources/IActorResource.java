package de.uhd.ifi.se.pcm.bppcm.resources;

import java.util.Collection;

import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourcetype.ResourceType;

import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import de.uhd.ifi.se.pcm.bppcm.resources.entities.ActorResourceInstance;
import edu.kit.ipd.sdq.eventsim.api.IRequest;
import edu.kit.ipd.sdq.eventsim.api.Procedure;

public interface IActorResource {

	

	ActorResourceInstance findOrRegisterActorResourceInstance(ActorResource specification);
	void consume(ActorResource specification, IRequest request, double absoluteDemand, int resourceServiceID,
			Procedure onServedCallback);
	public Collection<ActorResourceInstance> getAllActorResourceInstances();
	
}
