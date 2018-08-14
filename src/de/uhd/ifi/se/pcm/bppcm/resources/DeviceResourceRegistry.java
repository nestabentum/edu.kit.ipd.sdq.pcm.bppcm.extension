package de.uhd.ifi.se.pcm.bppcm.resources;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.palladiosimulator.pcm.repository.PassiveResource;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.uhd.ifi.se.pcm.bppcm.NewEventSimClasses.IntBIISEventSimSystemModel;
import de.uhd.ifi.se.pcm.bppcm.core.EventSimModel;
import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import edu.kit.ipd.sdq.eventsim.resources.PassiveResourceRegistry;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimPassiveResource;
import edu.kit.ipd.sdq.eventsim.util.PCMEntityHelper;

/**
 * This registry maps a given device resource specification to a specified device resource instance.
 * 
 * @author Robert Heinrich
 * 
 */

@Singleton
public class DeviceResourceRegistry  {
	private static final Logger logger = Logger.getLogger(DeviceResourceRegistry.class);
	// maps DeviceResource ID -> device resource instance
    private Map<String, SimDeviceResource /*DeviceResourceInstance*/> map;
    
    private List<Consumer<SimDeviceResource>> registrationListeners;
    
    @Inject
    private BPResourceFactory factory;
    
    
    
    /**
     * Constructs a new registry for device resources.
     */
    public DeviceResourceRegistry() {
        this.map = new HashMap<String, SimDeviceResource /*DeviceResourceInstance*/>();
        registrationListeners = new LinkedList<>();
    }
    
    
    public void addResourceRegistrationListener(Consumer<SimDeviceResource> listener) {
        registrationListeners.add(listener);
    }

    private void notifyRegistrationListeners(SimDeviceResource resource) {
        registrationListeners.forEach(listener -> listener.accept(resource));
    }
    
    /**
     * Registers a device resource instance by mapping the given device resource specification to
     * the specified device resource instance.
     * 
     * @param specification
     *            the device resource specification
     * @param instance
     *            the device resource instance
     */
    public void registerDeviceResource(DeviceResource specification, SimDeviceResource instance/*DeviceResourceInstance instance*/) {
        map.put(specification.getId(), instance);
    }
    
    /**
     * Returns the device resource instance for the given device resource specification
     * 
     * @param specification
     *            the device resource specification
     * @return the resource instance for the passed specification
     */
    public SimDeviceResource getDeviceResource(DeviceResource specification) {
    	SimDeviceResource r = map.get(specification.getId());
        if (r == null) {
            throw new RuntimeException("Could not find the actor resource instance for "
                    + PCMEntityHelper.toString(specification));
        }
        return r;
    }
    
    public SimDeviceResource findOrCreateResource(DeviceResource specification){
    	if(!map.containsKey(specification.getId())){
    		SimDeviceResource resource = factory.createDeviceResource(specification);
    		
    		map.put(specification.getId(), resource);
    		
    		logger.info(String.format("Created Device resource %s",
                    PCMEntityHelper.toString(specification)));
    		
    		 notifyRegistrationListeners(resource);
    	}
		return map.get(specification.getId());
    }



}
