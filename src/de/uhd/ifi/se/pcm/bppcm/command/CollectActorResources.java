package de.uhd.ifi.se.pcm.bppcm.command;

import java.util.LinkedHashSet;
import java.util.Set;

import de.uhd.ifi.se.pcm.bppcm.resources.ActorResourceRegistry;
import de.uhd.ifi.se.pcm.bppcm.resources.entities.ActorResourceInstance;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.command.ICommand;
import edu.kit.ipd.sdq.eventsim.command.ICommandExecutor;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimActiveResource;




/**
 * Creates and returns a set of all actor resources
 * 
 * @author Robert Heinrich
 * 
 */
public class CollectActorResources implements ICommand<Set<SimActiveResource>, PCMModel> {

	private ActorResourceRegistry registry;

    public CollectActorResources(ActorResourceRegistry environment) {
        this.registry = environment;
    }

//    @Override
//    public Set<SimActiveResource> execute(PCMModel pcm, ICommandExecutor<PCMModel> executor) {
//        
//    	// collect all active resources
//        Set<SimActiveResource> resources = new LinkedHashSet<SimActiveResource>();
//        for (ActorResourceInstance a : registry.getAllActorResourceInstances()) {
//        
//        	resources.add(a.getResource());
//        }
//        return resources;
//    }

    @Override
    public boolean cachable() {
        return false;
    }

	@Override
	public Set<SimActiveResource> execute(PCMModel model, ICommandExecutor<PCMModel> executor) {
		// TODO Auto-generated method stub
		Set<SimActiveResource> resources = new LinkedHashSet<SimActiveResource>();
        for (ActorResourceInstance a : registry.getAllActorResourceInstances()) {
        
        	resources.add(a.getResource());
        }
        return resources;
	}

}
