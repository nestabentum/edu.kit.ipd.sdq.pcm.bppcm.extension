package de.uhd.ifi.se.pcm.bppcm.workload;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import de.uhd.ifi.se.pcm.bppcm.workload.generator.BPWorkloadGeneratorFactory;
import edu.kit.ipd.sdq.eventsim.api.IWorkload;
import edu.kit.ipd.sdq.eventsim.workload.EventSimWorkloadModel;
import edu.kit.ipd.sdq.eventsim.workload.entities.UserFactory;
import edu.kit.ipd.sdq.eventsim.workload.generator.WorkloadGeneratorFactory;


public class BPWorkloadModule extends AbstractModule{

	@Override
	protected void configure() {
		
		install(new FactoryModuleBuilder().build(UserFactory.class));
		install(new FactoryModuleBuilder().build(BPWorkloadGeneratorFactory.class));
		// TODO Auto-generated method stub
		bind(IWorkload.class).to(BPWorkloadModel.class);
		
		
	}

}
