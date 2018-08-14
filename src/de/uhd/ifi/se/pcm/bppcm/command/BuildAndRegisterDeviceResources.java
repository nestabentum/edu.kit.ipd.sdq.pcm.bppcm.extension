package de.uhd.ifi.se.pcm.bppcm.command;

import com.google.inject.Inject;


import de.uhd.ifi.se.pcm.bppcm.NewEventSimClasses.IntBIISEventSimSystemModel;

import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.OrganizationEnvironmentModel;
import de.uhd.ifi.se.pcm.bppcm.resources.DeviceResourceModel;
import edu.kit.ipd.sdq.eventsim.api.PCMModel;
import edu.kit.ipd.sdq.eventsim.command.ICommandExecutor;
import edu.kit.ipd.sdq.eventsim.command.IPCMCommand;
import edu.kit.ipd.sdq.eventsim.resources.ResourceFactory;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimPassiveResource;

/**
 * Builds up a registry that contains all device resources of the PCM model.
 * 
 * @author Robert Heinrich
 * 
 */
public class BuildAndRegisterDeviceResources implements IPCMCommand<Void> {

	
	IntBIISEventSimSystemModel model;
	
	@Inject 
	DeviceResourceModel resourceModel;
	
	
	/**
     * Constructs a command that builds up a registry containing all device resources of a PCM
     * model.
     * 
     * @param model
     *            the simulation model
     */
    public BuildAndRegisterDeviceResources(IntBIISEventSimSystemModel model) {
        this.model = model;
    }
	
	@Override
	public Void execute(PCMModel model, ICommandExecutor<PCMModel> executor) {
			
			// get the organization environment model
			OrganizationEnvironmentModel oem = this.model.getOrganizationalModel();
			
			// return if the oem is not contained in the model
			// if only IT is simulated the oem is not required
			if(oem == null){
				return null;
			}
		
		// for each device resource specification
        for (DeviceResource specification : oem.getDeviceResources()) {
            
        	
        	// create device resource instance
            //IntBIISSimDeviceResource res = IntBIISResourceFactory.createDeviceResource(eventSimModel, specification);             
            // register the created device resource instance
        	this.resourceModel.getDeviceResource(specification);
            //this.resourceModel.findOrCreateResource(specification, this.model);
            
        }
        
        // this command is not supposed to return a value
        return null;
	
	}
	

	@Override
	public boolean cachable() {
		// TODO Auto-generated method stub
		return false;
	}

}
