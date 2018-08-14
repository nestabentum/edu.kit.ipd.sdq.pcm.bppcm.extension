package de.uhd.ifi.se.pcm.bppcm.strategies;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.Activity;

import java.util.function.Consumer;


import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;

import edu.kit.ipd.sdq.eventsim.api.IUser;
import edu.kit.ipd.sdq.eventsim.interpreter.SimulationStrategy;
import edu.kit.ipd.sdq.eventsim.interpreter.TraversalInstruction;
import edu.kit.ipd.sdq.eventsim.system.entities.Request;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;


/**
 * This traversal strategy is responsible for {@link Activity} actions.
 * 
 * @author Robert Heinrich
 * 
 */
public class ActivityTraversalStrategy implements SimulationStrategy<Activity, Request> {


@Override
public void simulate(Activity action, Request entity, Consumer<TraversalInstruction> onFinishCallback) {
	ScenarioBehaviour behaviour = action.getScenarioBehaviour_AbstractUserAction();
	User user = (User)entity.getUser();
	user.simulateBehaviour(behaviour, () -> {
		onFinishCallback.accept(() -> {
			user.simulateAction(action.getSuccessor());
		});
	});
	}
}
