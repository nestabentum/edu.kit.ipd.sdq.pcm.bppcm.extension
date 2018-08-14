package de.uhd.ifi.se.pcm.bppcm.strategies;


import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.AcquireDeviceResourceAction;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import de.uhd.ifi.se.pcm.bppcm.resources.IDeviceResource;

import java.util.function.Consumer;

import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;

import com.google.inject.Inject;

import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import edu.kit.ipd.sdq.eventsim.interpreter.SimulationStrategy;
import edu.kit.ipd.sdq.eventsim.interpreter.TraversalInstruction;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimPassiveResource;
import edu.kit.ipd.sdq.eventsim.system.entities.Request;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;

/**
 * This traversal strategy is responsible for {@AcquireDeviceResource} actions.
 * 
 * @author Robert Heinrich
 * 
 */
public class AcquireDeviceResourceTraversalStrategy implements SimulationStrategy<AcquireDeviceResourceAction, Request> {

	@Inject 
	IDeviceResource deviceResourceModel;
//	@Override
//	public ITraversalInstruction<AbstractUserAction, UserState> traverse(
//			AcquireDeviceResourceAction action, User entity, UserState state) {
//		
//        final DeviceResource passiveResouce = action.getPassiveresource_AcquireAction();
//
//        final SimPassiveResource res = entity.getSimulatedProcess().getModel().getDeviceResourceRegistry().getDeviceResource(passiveResouce);
//        
//        final boolean acquired = res.acquire(entity.getSimulatedProcess(), 1, false, action.getTimeoutValue());
//
//        if (acquired) {
//            return UsageTraversalInstructionFactory.traverseNextAction(action.getSuccessor());
//        } else {
//            entity.passivate(new ResumeUsageTraversalEvent(entity.getSimulatedProcess().getModel(), state), 0);
//
//            // here, it is assumed that the passive resource grants access to waiting processes as
//            // soon as the requested capacity becomes available. Thus, we do not need to acquire the
//            // passive resource again as this will be done within the release method. Accordingly
//            // the traversal resumes with the successor of this action.
//            return UsageTraversalInstructionFactory.interruptTraversal(action.getSuccessor());
//        }
//	}

	@Override
	public void simulate(AcquireDeviceResourceAction action, Request entity,
			Consumer<TraversalInstruction> onFinishCallback) {
		
		final DeviceResource deviceResouce = action.getPassiveresource_AcquireAction();
				
		deviceResourceModel.acquire(entity, deviceResouce, 1, () ->{
			onFinishCallback.accept(() -> {
				((User)entity.getUser()).simulateAction(action.getSuccessor());
			});
		});

		
	}

}
