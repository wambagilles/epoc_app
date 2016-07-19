package fr.emn.premode.heuristics;

import java.util.stream.Collectors;

import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

import fr.emn.premode.Scheduler;

/**
 * set the web modes with highest profit first, to their highest profit.
 * 
 * @author Guillaume Le Louët
 *
 */
public class WebHighestProfitFirst implements Heuristic {

	public static final WebHighestProfitFirst INSTANCE = new WebHighestProfitFirst();

	@Override
	public AbstractStrategy<?> makeStrat(Scheduler scheduler) {
		IntVar[] vars = scheduler.webModes.values().stream().flatMap(l -> l.stream()).map(wsc -> wsc.profit)
				.sorted((i1, i2) -> i1.getUB() - i2.getUB()).collect(Collectors.toList()).toArray(new IntVar[] {});
		return Search.inputOrderUBSearch(vars);
	}

}
