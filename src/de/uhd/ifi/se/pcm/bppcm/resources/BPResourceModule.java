package de.uhd.ifi.se.pcm.bppcm.resources;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import de.uka.ipd.sdq.scheduler.SchedulerModel;
import edu.kit.ipd.sdq.eventsim.middleware.simulation.SimulationModel;



public class BPResourceModule extends AbstractModule{

	@Override
	protected void configure() {
		// TODO Auto-generated method stub
		bind(IActorResource.class).to(ActorResourceModel.class).asEagerSingleton();
        bind(IDeviceResource.class).to(DeviceResourceModel.class).asEagerSingleton();
        
        bind(SchedulerModel.class).to(SimulationModel.class).in(Singleton.class);
	}

}
