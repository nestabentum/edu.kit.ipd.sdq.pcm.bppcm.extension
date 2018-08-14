package de.uhd.ifi.se.pcm.bppcm.calculators;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.Activity;
import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.ActorStep;
import edu.kit.ipd.sdq.eventsim.measurement.calculator.AbstractBinaryCalculator;
import edu.kit.ipd.sdq.eventsim.measurement.probe.IProbe;
import edu.kit.ipd.sdq.eventsim.measurement.Measurement;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPoint;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPointPair;
import edu.kit.ipd.sdq.eventsim.measurement.Pair;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.Calculator;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.ProbePair;
/**
 * This calculator calculates the execution time of actor steps.
 * @author Nesta Bentum
 *
 */
@Calculator(metric = "executiontime_of_actorstep", type = Pair.class, fromType = ActorStep.class, toType = ActorStep.class,
	intendedProbes = {@ProbePair(from = "before", to = "after")})
public class ActorStepExecutionTimeCalculator extends AbstractBinaryCalculator<ActorStep, ActorStep>  {

	@Override
	public Measurement<Pair<ActorStep, ActorStep>> calculate(Measurement<ActorStep> from, Measurement<ActorStep> to) {
		double when = to.getWhen();
		double executionTime = to.getValue() - from.getValue();
		
		MeasuringPoint<Pair<ActorStep, ActorStep>> mp = new MeasuringPointPair<>(from.getWhere(), to.getWhere(), "executiontime_of_actorstep", to.getWhere().getContexts());

		return new Measurement<Pair<ActorStep,ActorStep>>("EXECUTIONTIME_OF_ACTORSTEP", mp, to.getWho(), executionTime, when);
	}

	@Override
	public void setup(IProbe<ActorStep> from, IProbe<ActorStep> to) {
		//make sure none of the measurements is null
		if(!(from == null || to == null)) {
		from.enableCaching();
			to.forEachMeasurement(measurement-> {
				notify(calculate(from.getLastMeasurementOf(measurement.getWho()), measurement));
			});
		}
		
	}

}
