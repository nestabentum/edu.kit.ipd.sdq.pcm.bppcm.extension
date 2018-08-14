package de.uhd.ifi.se.pcm.bppcm.probes;

import edu.kit.ipd.sdq.eventsim.measurement.Measurement;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPoint;
import edu.kit.ipd.sdq.eventsim.measurement.Metadata;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.Probe;
import edu.kit.ipd.sdq.eventsim.measurement.probe.AbstractProbe;
import edu.kit.ipd.sdq.eventsim.resources.entities.AbstractActiveResource;
import edu.kit.ipd.sdq.eventsim.resources.listener.IStateListener;
import de.uhd.ifi.se.pcm.bppcm.probes.configurations.ActorResourceProbeConfiguration;
import edu.kit.ipd.sdq.eventsim.entities.EventSimEntity.EntityLifecyclePhase;
/**
 * This class probes the state of a given {@link de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource}, whereas
 *  'state' is synonymous with EventSim's 'EntityLifecyclePhase'. This probe has to be fed with the {@link edu.kit.ipd.sdq.eventsim.resources.entities.AbstractActiveResource}
 *  of an {@link de.uhd.ifi.se.pcm.bppcm.resources.entities.ActorResourceInstance}.
 *  This class might be obsolete as EventSim's QueueLengthProbe might already have all the functionality this class has.
 * @author Nesta Bentum
 * 
 * @see de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.ActorResource
 * @see edu.kit.ipd.sdq.eventsim.resources.entities.AbstractActiveResource
 * @see de.uhd.ifi.se.pcm.bppcm.resources.entities.ActorResourceInstance
 */
@Probe(type = AbstractActiveResource.class, property = "resource_state")
public class ActorResourceStateProbe extends AbstractProbe<AbstractActiveResource, ActorResourceProbeConfiguration>{

	
	//TODO this is a copy of ResourceDemandProbe... is this even needed?
	public ActorResourceStateProbe(MeasuringPoint<AbstractActiveResource> p, ActorResourceProbeConfiguration configuration) {
		super(p, configuration);
		
		AbstractActiveResource resource = p.getElement();
			for (int instance = 0; instance < resource.getNumberOfInstances(); instance++) {
				resource.addStateListener(new IStateListener() {
					
					@Override
					public void stateChanged(long state, int instanceId) {
						
	                    // build measurement
	                    double simTime = resource.getModel().getSimulationControl().getCurrentSimulationTime();
	                    Measurement<AbstractActiveResource> m = new Measurement<>("RESOURCE_STATE", getMeasuringPoint(), null,
	                            state, simTime);

	                    measurementListener.forEach(l -> l.notify(m));						
					}
				}, instance);
			}
	}

	
}
