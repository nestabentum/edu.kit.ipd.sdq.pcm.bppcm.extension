package de.uhd.ifi.se.pcm.bppcm.strategies;


import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ReleaseDeviceResourceAction;
import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import de.uhd.ifi.se.pcm.bppcm.resources.IDeviceResource;

import java.util.function.Consumer;

import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;

import com.google.inject.Inject;


import edu.kit.ipd.sdq.eventsim.interpreter.SimulationStrategy;
import edu.kit.ipd.sdq.eventsim.interpreter.TraversalInstruction;

import edu.kit.ipd.sdq.eventsim.system.entities.Request;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;

/**
 * This traversal strategy is responsible for {@ReleaseDeviceResource} actions.
 * 
 * @author Robert Heinrich
 * 
 */
public class ReleaseDeviceResourceTraversalStrategy implements SimulationStrategy<ReleaseDeviceResourceAction, Request>{

	@Inject
	IDeviceResource dev;
	
	
//	@Override
//	public ITraversalInstruction<AbstractUserAction, UserState> traverse(
//			ReleaseDeviceResourceAction action, User entity, UserState state) {
//		
//
//        final DeviceResource passiveResouce = action.getPassiveresource_ReleaseAction();
//
//        final SimPassiveResource res = entity.getSimulatedProcess().getModel().getDeviceResourceRegistry().getDeviceResource(passiveResouce);
//        		
//        res.release(entity.getSimulatedProcess(), 1);
//
//        return UsageTraversalInstructionFactory.traverseNextAction(action.getSuccessor());
//	}

	@Override
	public void simulate(ReleaseDeviceResourceAction action, Request entity,
			Consumer<TraversalInstruction> onFinishCallback) {

		final DeviceResource deviceResource = action.getPassiveresource_ReleaseAction();
		
		dev.release(entity, deviceResource, 1);
		
		onFinishCallback.accept(()->{
			((User)entity.getUser()).simulateAction(action.getSuccessor());
		});
	}

}
