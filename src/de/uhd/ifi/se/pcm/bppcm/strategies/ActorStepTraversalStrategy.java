package de.uhd.ifi.se.pcm.bppcm.strategies;



import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ActorStep;
import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource;
import de.uhd.ifi.se.pcm.bppcm.resources.ActorResourceModel;
import edu.kit.ipd.sdq.pcm.simulation.bpscheduler.SuspendableFCFSResource;
import de.uka.ipd.sdq.simucomframework.variables.converter.NumberConverter;
import edu.kit.ipd.sdq.eventsim.interpreter.SimulationStrategy;
import edu.kit.ipd.sdq.eventsim.interpreter.TraversalInstruction;
import edu.kit.ipd.sdq.eventsim.system.entities.Request;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;

import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;

import com.google.inject.Inject;


/**
 * This traversal strategy is responsible for {@link ActorStep} actions.
 * 
 * @author Robert Heinrich
 * 
 */
public class ActorStepTraversalStrategy implements SimulationStrategy<ActorStep, Request>{

    private static final Logger logger = Logger.getLogger(ActorStepTraversalStrategy.class);
    
    @Inject
    private ActorResourceModel ar;
    
//	@Override
//	public ITraversalInstruction<AbstractUserAction, UserState> traverse(
//			ActorStep action, User user, UserState state) {
//		final EventSimModel model = user.getModel();
//		
//		// set flag whether the execution of the step is interruptable
//		user.getSimulatedProcess().setInterruptable(action.isInterruptable());
//
//        ActorResource selectedActor = model.getDispatcher().dispatch(user, action);
//        
//        
//        // allocate the resource demand to the selected actor
//        if (selectedActor != null){
//        	
//        	final PCMRandomVariable d = action.getProcessingTime();
//        
//        	final double demand = NumberConverter.toDouble(state.getStoExContext().evaluate(d.getSpecification()));
//      
//        	ar.consume(specification, request, absoluteDemand, resourceServiceID, onServedCallback);model.getActorResourceRegistry().getActorResourceForContext(selectedActor).getResource().consumeResource(user.getSimulatedProcess(), demand);
//        	
//        	logger.info("Queue-Legth of " + selectedActor.getEntityName() + " AFTER allocation is: " + model.getActorResourceRegistry().getActorResourceForContext(selectedActor).getResource().getCurrentDemand());
//        }
//        
//        // wait for resting time before the next step
//        user.passivate(new ResumeUsageTraversalEvent(model, state), action.getRestingTime());
//         
//        return UsageTraversalInstructionFactory.interruptTraversal(action.getSuccessor());
//           
//    }

	@Override
	public void simulate(ActorStep action, Request entity, Consumer<TraversalInstruction> onFinishCallback) {
		// TODO Auto-generated method stub
		final EventSimModel model = (EventSimModel)entity.getModel();
		final User user = (User)entity.getUser();
		ActorResource selectedActor = model.getDispatcher().dispatch(user,action);
		// set flag whether the execution of the step is interruptable
		ar.setInterruptable(entity, action);
		 // allocate the resource demand to the selected actor
		
		//Set some default ID
		//TODO: Set some default ID, cf. InternalActionSimulationStragegy
		int resourceServiceID = 1;
        if (selectedActor != null){
        	
        	final PCMRandomVariable d = action.getProcessingTime();
        
        	final double demand = NumberConverter.toDouble(entity.getUser().getStochasticExpressionContext().evaluate(d.getSpecification()));
        	
        	//Consume Resource and Procede
        	ar.consume(selectedActor, entity, demand, resourceServiceID, () -> {});
        	
        	//model.getActorResourceRegistry().getActorResourceForContext(selectedActor).getResource().consumeResource(user.getSimulatedProcess(), demand);

        	logger.info("Queue-Legth of " + selectedActor.getEntityName() + " AFTER allocation is: " + ((SuspendableFCFSResource)model.getActorResourceRegistry().getActorResourceForContext(selectedActor).getResource().getSchedulerResource()).getRemainingDemand());
        }
        
    	//Wait for Resting time before next step
		user.delay(action.getRestingTime(), () -> {
			//Go to next step
			user.simulateAction(action.getSuccessor());
		});
		
	}

	

}
