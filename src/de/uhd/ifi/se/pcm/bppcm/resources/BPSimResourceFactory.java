package de.uhd.ifi.se.pcm.bppcm.resources;

import org.palladiosimulator.pcm.repository.PassiveResource;

import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import de.uka.ipd.sdq.scheduler.IPassiveResource;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimPassiveResource;

public interface BPSimResourceFactory {

	SimDeviceResource createDeviceResource(IPassiveResource resource, DeviceResource specification);
}
