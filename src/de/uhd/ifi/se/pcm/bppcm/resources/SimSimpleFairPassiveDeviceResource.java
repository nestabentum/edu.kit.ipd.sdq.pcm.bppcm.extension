package de.uhd.ifi.se.pcm.bppcm.resources;

import java.util.ArrayDeque;
import java.util.Queue;

import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.PassiveResource;

import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import de.uka.ipd.sdq.scheduler.IPassiveResource;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.processes.IWaitingProcess;
import de.uka.ipd.sdq.scheduler.processes.SimpleWaitingProcess;
import de.uka.ipd.sdq.scheduler.resources.AbstractSimResource;
import de.uka.ipd.sdq.scheduler.resources.passive.PassiveResourceObservee;
import de.uka.ipd.sdq.scheduler.sensors.IPassiveResourceSensor;
import de.uka.ipd.sdq.simucomframework.exceptions.FailureException;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.resources.PassiveResourceTimeoutEvent;



/*
 * This Class corresponds with the SimSimpleFairPassiveResource of the SimuCom Framework. 
 * It is created because the SimuComp Resource is fitted to the PassivResource with an Assembly Context 
 * and PassiveResource as constructor parameters. To use DeviceResource without inherit from PassiveResource, 
 * this class was created. 
 * TODO Factor out the common parts for redundancy elimination and further enhancements 
 * TODO Refused Bequest of IPassiveResource, but no other possibility at the moment other than create IDeviceResource scheduling interface
 * however possibility to break the working mechanism 
 */
public class SimSimpleFairPassiveDeviceResource extends AbstractSimResource implements IPassiveResource{

	
	protected Queue<IWaitingProcess> waitingQueue;
    private final SchedulerModel myModel;
    private long available;
    private final String passiveResourceID;
    private final boolean simulateFailures;
    
 // provides observer functionality to this resource
    private final PassiveResourceObservee observee;
    private final DeviceResource resource;
	
	public SimSimpleFairPassiveDeviceResource(final DeviceResource resource, SchedulerModel model, long capacity, String name, String id) {
		super(model, capacity, name, id);
		this.resource = resource;
		
		this.waitingQueue = new ArrayDeque<IWaitingProcess>();
        this.myModel = model;
        this.passiveResourceID = resource.getId();
        this.observee = new PassiveResourceObservee();
        this.available = capacity;
        
        /* 
		 * The following workaround can be removed once failure simulation has been factored out of this class or has
		 * been made independent of SimuComModel.
		 * 
		 * Actually, neither this class nor the failure-enhanced class should prescribe the concrete simulation model
		 * because this excludes simulators other than SimuCom from reusing this class. Instead, this class should
		 * require an abstract (!) simulation model like ISimulationModel.
		 */        
		if (myModel instanceof SimuComModel) {
			this.simulateFailures = ((SimuComModel) model).getConfiguration().getSimulateFailures();
		} else {
			this.simulateFailures = false;
		}
	}

	
	private boolean canProceed(final ISchedulableProcess process, final long num) {
	     return (waitingQueue.isEmpty() || waitingQueue.peek().getProcess().equals(process)) && num <= available;
	    }
	  
	  private void grantAccess(final ISchedulableProcess process, final long num) {
	        LoggingWrapper.log("Process " + process + " acquires " + num + " of " + this);
	        this.available -= num;
	        observee.fireAquire(process, num);
	        assert this.available >= 0 : "More resource than available have been acquired!";
	    }
	@Override
	public boolean acquire(ISchedulableProcess process, long num, boolean timeout, double timeoutValue) {
		 // AM: Copied from AbstractActiveResource: If simulation is stopped,
        // allow all processes to finish
        if (!myModel.getSimulationControl().isRunning()) {
            // Do nothing, but allows calling process to complete
            return true;
        }
        
        // TODO:
        // Do we need some logic here to check if the simulation has stopped?
        // In this case, this method should not block, but return in order to
        // allow processes to complete
        observee.fireRequest(process, num);
        if (canProceed(process, num)) {
            grantAccess(process, num);
            return true;
        } else {
            LoggingWrapper.log("Process " + process + " is waiting for " + num + " of " + this);
            final SimpleWaitingProcess waitingProcess = new SimpleWaitingProcess(myModel, process, num);
            processTimeout(timeout, timeoutValue, waitingProcess);
            waitingQueue.add(waitingProcess);
            process.passivate();
            return false;
        }
	}
	  /**
     * Schedules a timeout event if a timeout is specified and failures are simulated.
     *
     * @param timeout
     *            indicates if the acquire request is associated with a timeout
     * @param timeoutValue
     *            the timeout value
     * @param process
     *            the waiting process
     */
    private void processTimeout(final boolean timeout, final double timeoutValue, final SimpleWaitingProcess process) {
        if (!simulateFailures || !timeout) {
            return;
        }
		// this cast is safe because simulateFailure is true if and only if myModel is a SimuComModel
		SimuComModel simuComModel = (SimuComModel) myModel;
		
        if (timeoutValue == 0.0) {
        	//TODO: FixThis
//            FailureException.raise(
//                    simuComModel,
//                    simuComModel.getFailureStatistics().getResourceTimeoutFailureType(this.assemblyContext.getId(),
//                            this.passiveResourceID));
        }
        if (timeoutValue > 0.0) {
            final PassiveDeviceResourceTimeoutEvent event = new PassiveDeviceResourceTimeoutEvent(simuComModel, this.myModel, this, process); 
            event.schedule(process, timeoutValue);
        }
    }
	@Override
	public void release(ISchedulableProcess process, long num) {
		// AM: Copied from AbstractActiveResource: If simulation is stopped,
        // allow all processes to finish
        if (!myModel.getSimulationControl().isRunning()) {
            // Do nothing, but allows calling process to complete
            return;
        }

        LoggingWrapper.log("Process " + process + " releases " + num + " of " + this);
        this.available += num;
        observee.fireRelease(process, num);
        notifyWaitingProcesses();
	}
	
	
	//TODO isn't passive resource but interface needed ... find workaround
	@Override
	public PassiveResource getResource() {
		// TODO Auto-generated method stub
		return null;
	}
	//TODO isn't passive resource but interface needed ... find workaround
	@Override
	public AssemblyContext getAssemblyContext() {
		// TODO Auto-generated method stub
		return null;
	}

	public DeviceResource getDeviceResource(){
		return this.resource;
	}
	@Override
	public long getAvailable() {
		return available;
	}

	@Override
	public void addObserver(IPassiveResourceSensor observer) {
		observee.addObserver(observer);
		
	}

	@Override
	public void removeObserver(IPassiveResourceSensor observer) {
		 observee.removeObserver(observer);
		
	}

	@Override
	public Queue<IWaitingProcess> getWaitingProcesses() {
		return waitingQueue;
	}
	
	 protected String getPassiveResourceID() {
	        return passiveResourceID;
	    }
	 
	 private void notifyWaitingProcesses() {
	        SimpleWaitingProcess waitingProcess = (SimpleWaitingProcess) waitingQueue.peek();
	        while (waitingProcess != null && canProceed(waitingProcess.getProcess(), waitingProcess.getNumRequested())) {
	            grantAccess(waitingProcess.getProcess(), waitingProcess.getNumRequested());
	            waitingQueue.remove();
	            waitingProcess.getProcess().activate();
	            waitingProcess = (SimpleWaitingProcess) waitingQueue.peek();
	        }
	    }


	    /**
	     * Determines if a given process is currently waiting to acquire this resource.
	     *
	     * @param process
	     *            the process
	     * @return TRUE if the process is waiting to acquire the resource; FALSE otherwise
	     */
	    public boolean isWaiting(final SimpleWaitingProcess process) {
	        return waitingQueue.contains(process);
	    }

	    /**
	     * Removes a waiting process from the queue.
	     *
	     * @param process
	     *            the process to remove
	     */
	    public void remove(final SimpleWaitingProcess process) {
	        waitingQueue.remove(process);
	    }
	}

