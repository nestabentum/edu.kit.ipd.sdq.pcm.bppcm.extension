package de.uhd.ifi.se.pcm.bppcm.command;

import java.util.LinkedHashSet;
import java.util.Set;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.Activity;
import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.BpusagemodelPackage;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.Branch;
import org.palladiosimulator.pcm.usagemodel.BranchTransition;
import org.palladiosimulator.pcm.usagemodel.Loop;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;

import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.command.ICommandExecutor;
import edu.kit.ipd.sdq.eventsim.command.IPCMCommand;
import edu.kit.ipd.sdq.eventsim.command.useraction.FindActionInUsageBehaviour;


/**
 * Searches for and returns all elements of type {@link Activity} contained in the given usage scenario
 * 
 * @author Robert Heinrich
 * 
 */
public class FindActivitiesOfScenario implements IPCMCommand<Set<Activity>> {

	private UsageScenario scenario;

    /**
     * Constructs a command that returns all activities contained in the given usage
     * scenario.
     * 
     * @param scenario
     *            the usage scenario
     */
    public FindActivitiesOfScenario(UsageScenario scenario) {
        this.scenario = scenario;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Activity> execute(PCMModel pcm, ICommandExecutor<PCMModel> executor) {
        ScenarioBehaviour behaviour = scenario.getScenarioBehaviour_UsageScenario();
        return findActivities(behaviour, executor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cachable() {
        return false;
    }

    /**
     * Searches for and returns all activities that are contained in the specified scenario
     * behavior
     */
    private Set<Activity> findActivities(ScenarioBehaviour behaviour, ICommandExecutor<PCMModel> executor) {
        Set<Activity> activities = new LinkedHashSet<Activity>();

        // find start action
        AbstractUserAction currentAction = executor.execute(new FindActionInUsageBehaviour<Start>(behaviour,
                Start.class));
        while (currentAction != null) {
            if (BpusagemodelPackage.eINSTANCE.getActivity().isInstance(currentAction)) {
                activities.add((Activity) currentAction);
                activities.addAll(findActivityInActivity((Activity) currentAction, executor));
            } else if (UsagemodelPackage.eINSTANCE.getBranch().isInstance(currentAction)) {
                activities.addAll(findActivityInBranch((Branch) currentAction, executor));
            } else if (UsagemodelPackage.eINSTANCE.getLoop().isInstance(currentAction)) {
                activities.addAll(findActivityInLoop((Loop) currentAction, executor));
            }
            currentAction = currentAction.getSuccessor();
        }
        return activities;
    }

    /**
     * Searches for and returns all activities that are contained in the specified activity.
     */
    private Set<Activity> findActivityInActivity (Activity action, ICommandExecutor<PCMModel> executor){
			
    	ScenarioBehaviour behaviour = action.getScenario();
        return findActivities(behaviour, executor);
	}

	/**
     * Searches for and returns all activities that are contained in the specified branch.
     */
    private Set<Activity> findActivityInBranch(Branch action, ICommandExecutor<PCMModel> executor) {
        Set<Activity> activities = new LinkedHashSet<Activity>();
        for (BranchTransition t : action.getBranchTransitions_Branch()) {
            ScenarioBehaviour behaviour = t.getBranchedBehaviour_BranchTransition();
            activities.addAll(findActivities(behaviour, executor));
        }
        return activities;
    }

    /**
     * Searches for and returns all activities that are contained in the specified loop.
     */
    private Set<Activity> findActivityInLoop(Loop action, ICommandExecutor<PCMModel> executor) {
        ScenarioBehaviour behaviour = action.getBodyBehaviour_Loop();
        return findActivities(behaviour, executor);
    }


}

