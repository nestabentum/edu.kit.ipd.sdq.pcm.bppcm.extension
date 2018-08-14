package de.uhd.ifi.se.pcm.bppcm.probes;

import de.uhd.ifi.se.pcm.bppcm.probes.configurations.ActorResourceProbeConfiguration;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import edu.kit.ipd.sdq.eventsim.measurement.Measurement;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPoint;
import edu.kit.ipd.sdq.eventsim.measurement.Metadata;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.Probe;
import edu.kit.ipd.sdq.eventsim.measurement.probe.AbstractProbe;
import edu.kit.ipd.sdq.eventsim.resources.entities.AbstractActiveResource;
import edu.kit.ipd.sdq.eventsim.resources.listener.IDemandListener;

@Probe(type = AbstractActiveResource.class, property = "resource_demand")
public class ActorResourceDemandProbe extends AbstractProbe<AbstractActiveResource, ActorResourceProbeConfiguration>{

	
	//TODO this is a copy of ResourceDemandProbe... is this even needed?
	public ActorResourceDemandProbe(MeasuringPoint<AbstractActiveResource> p,
			ActorResourceProbeConfiguration configuration) {
		super(p, configuration);
		AbstractActiveResource resource = p.getElement();
		
		 for (int instance = 0; instance < resource.getNumberOfInstances(); instance++) {
	            resource.addDemandListener(new IDemandListener() {
	                @Override
	                public void demand(ISchedulableProcess process, double demand, int resourceServiceId) {
	                    // build measurement
	                    double simTime = resource.getModel().getSimulationControl().getCurrentSimulationTime();
	                    Measurement<AbstractActiveResource> m = new Measurement<>("ACTORRESOURCE_DEMAND", getMeasuringPoint(),
	                            process, demand, simTime, new Metadata("resourceserviceid", resourceServiceId));

	                    // store
	                    // cache.put(m); TODO cache not needed! --> account for in abstract
	                    // superclass/constructor? or
	                    // enable by calculator (!)?

	                    // notify
	                    measurementListener.forEach(l -> l.notify(m));
	                }
	            });
	        }
		
	}

	
}
