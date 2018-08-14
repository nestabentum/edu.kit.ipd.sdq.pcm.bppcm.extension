package de.uhd.ifi.se.pcm.bppcm.workload.generator;

import org.palladiosimulator.pcm.usagemodel.ClosedWorkload;
import org.palladiosimulator.pcm.usagemodel.OpenWorkload;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ProcessWorkload;
import edu.kit.ipd.sdq.eventsim.workload.generator.ClosedWorkloadGenerator;
import edu.kit.ipd.sdq.eventsim.workload.generator.OpenWorkloadGenerator;
import edu.kit.ipd.sdq.eventsim.workload.generator.WorkloadGeneratorFactory;

public interface BPWorkloadGeneratorFactory{
	
	ClosedWorkloadGenerator createClosed(ClosedWorkload workload);
    
    OpenWorkloadGenerator createOpen(OpenWorkload workload);
    
    ProcessWorkloadGenerator createProcess(ProcessWorkload workload);

}
