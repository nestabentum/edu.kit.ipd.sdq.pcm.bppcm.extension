package de.uhd.ifi.se.pcm.bppcm.resources;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.processes.SimpleWaitingProcess;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEventDelegator;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationModel;


/*
 * This class is created to support DeviceResources and duplicates the PassiveResourceTimeoutEvent 
 * TODO: factor out a common base for using other passive resources than "PassiveResource" 
 */
public class PassiveDeviceResourceTimeoutEvent extends AbstractSimEventDelegator<SimpleWaitingProcess>{

	
    private final SimpleWaitingProcess process;
    
    private final SimSimpleFairPassiveDeviceResource resource;
    
    private final SimuComModel simuComModel; 
		
	public PassiveDeviceResourceTimeoutEvent(final SimuComModel simuComModel, final SchedulerModel model,
            final SimSimpleFairPassiveDeviceResource resource, final SimpleWaitingProcess process) {
		super(model, resource.getName());


		this.resource = resource;
        this.simuComModel = simuComModel;
        this.process = process;
	}

	    @Override
	    public void eventRoutine(final SimpleWaitingProcess who) {

	        // Check if the process is still waiting:
	        if (!resource.isWaiting(process)) {
	            return;
	        }

	        // Trigger a timeout of the waiting process:
	        resource.remove(process);
	        
	        //TODO:FixThis
//	        process.getProcess().timeout(
//	                this.simuComModel
//	                        .getFailureStatistics()
//	                        .getResourceTimeoutFailureType(resource.getAssemblyContext().getId(),
//	                                resource.getPassiveResourceID()).getId());
	    }

	    /**
	     * Retrieves the waiting process.
	     * 
	     * @return the waiting process
	     */
	    public SimpleWaitingProcess getProcess() {
	        return process;
	    }

	    /**
	     * Retrieves the passive resource.
	     * 
	     * @return the passive resource
	     */
	    public SimSimpleFairPassiveDeviceResource getResource() {
	        return resource;
	    }

}
