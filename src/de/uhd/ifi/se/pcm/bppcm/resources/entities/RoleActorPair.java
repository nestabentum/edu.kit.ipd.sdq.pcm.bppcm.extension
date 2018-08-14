package de.uhd.ifi.se.pcm.bppcm.resources.entities;

import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.Role;

/**
 * This pair is used in the {@link Dispatcher}
 * 
 * @author Robert Heinrich
 * 
 */
public class RoleActorPair {

	private ActorResource actor;
	private Role role;
	
	public RoleActorPair(ActorResource actor, Role role){
		this.actor = actor;
		this.role = role;
	}

	public ActorResource getActor() {
		return actor;
	}

	public Role getRole() {
		return role;
	}
	
	
	
	
}
