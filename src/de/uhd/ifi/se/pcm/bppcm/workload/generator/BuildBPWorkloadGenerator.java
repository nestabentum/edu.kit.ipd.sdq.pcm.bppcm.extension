package de.uhd.ifi.se.pcm.bppcm.workload.generator;

import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.pcm.usagemodel.ClosedWorkload;
import org.palladiosimulator.pcm.usagemodel.OpenWorkload;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;
import org.palladiosimulator.pcm.usagemodel.Workload;

import com.google.inject.Inject;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.BpusagemodelPackage;
import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ProcessWorkload;
import de.uhd.ifi.se.pcm.bppcm.workload.generator.BPWorkloadGeneratorFactory;
import de.uhd.ifi.se.pcm.bppcm.workload.generator.ProcessWorkloadGenerator;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.command.ICommandExecutor;
import edu.kit.ipd.sdq.eventsim.command.IPCMCommand;
import edu.kit.ipd.sdq.eventsim.workload.WorkloadModelDiagnostics;
import edu.kit.ipd.sdq.eventsim.workload.generator.ClosedWorkloadGenerator;
import edu.kit.ipd.sdq.eventsim.workload.generator.OpenWorkloadGenerator;
import edu.kit.ipd.sdq.eventsim.workload.generator.WorkloadGenerator;
import edu.kit.ipd.sdq.eventsim.workload.generator.WorkloadGeneratorFactory;

//TODO This class is a copy of BuildWorkloadGenerator and extendet with Process workload. Making this extendable woulb be reduce duplicated code.
public class BuildBPWorkloadGenerator implements IPCMCommand<List<WorkloadGenerator>> {

	@Inject
    private BPWorkloadGeneratorFactory factory;
    
    @Inject
    private WorkloadModelDiagnostics diagnostics;
    
	@Override
	public List<WorkloadGenerator> execute(PCMModel model, ICommandExecutor<PCMModel> executor) {
		 final List<WorkloadGenerator> workloads = new ArrayList<WorkloadGenerator>();
	        for (final UsageScenario u : model.getUsageModel().getUsageScenario_UsageModel()) {
	            final Workload w = u.getWorkload_UsageScenario();
	            if (UsagemodelPackage.eINSTANCE.getOpenWorkload().isInstance(w)) {
	                OpenWorkloadGenerator generator = factory.createOpen((OpenWorkload) w);
	                workloads.add(generator);
	            } else if (UsagemodelPackage.eINSTANCE.getClosedWorkload().isInstance(w)) {
	                ClosedWorkloadGenerator generator = factory.createClosed((ClosedWorkload) w);
	                workloads.add(generator);
	            } else if (BpusagemodelPackage.eINSTANCE.getProcessWorkload().isInstance(w)) {
	            	ProcessWorkloadGenerator generator = factory.createProcess((ProcessWorkload) w);
	            	workloads.add(generator); 
	            }else {
	                diagnostics.reportMissingWorkload(u);
	            }
	        }
	        return workloads;
	}

	@Override
	public boolean cachable() {
		// TODO Auto-generated method stub
		return false;
	}

}
