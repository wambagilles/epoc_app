/**
 *
 */
package fr.lelouet.choco.limitpower.heuristics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.IntVar;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.model.PowerMode;
import fr.lelouet.choco.limitpower.model.SchedulingProblem.Objective;

/**
 * set the modes of the web apps to the highest profit.
 *
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 */
public class HeuristicsMaker {

	/**
	 * set the modes of the web apps to the highest profit. The web apps are first ordered by their (max profit-min
	 * profit)/(max profit power/min profit power) decreasing.
	 *
	 * @param scheduler
	 * @return
	 */
	public static IntStrategy webBestInterest(AppScheduler scheduler) {
		// map the app names to their interest.
		HashMap<String, Integer> appInterest = new HashMap<>();
		scheduler.getSource().webNames().forEach(n -> {
			int minprofit = Integer.MAX_VALUE, maxProfit = Integer.MIN_VALUE, minProfitPower = 0, maxProfitPower = 0;
			for (PowerMode m : scheduler.getSource().getWebPowerModes(n)) {
				if (m.profit > maxProfit) {
					maxProfit = m.profit;
					maxProfitPower = m.power;
				}
				if (m.profit < minprofit) {
					minprofit = m.profit;
					minProfitPower = m.power;
				}
			}
			appInterest.put(n, (maxProfit - minprofit) / (maxProfitPower - minProfitPower));
		});
		// order the map by values in an array
		@SuppressWarnings("unchecked")
		Entry<String, Integer>[] arr = appInterest.entrySet().toArray(new Map.Entry[] {});
		Arrays.sort(arr, (e1, e2) -> {
			return e2.getValue() - e1.getValue();
		});

		return Search.inputOrderUBSearch(Stream.of(arr).map(e -> scheduler.webModes.get(e.getKey())).flatMap(List::stream)
				.map(wsc -> wsc.profit).collect(Collectors.toList()).toArray(new IntVar[] {}));
	}

	public static IntStrategy webHighestProfitFirst(AppScheduler scheduler) {
		IntVar[] vars = scheduler.webModes.values().stream().flatMap(l -> l.stream()).map(wsc -> wsc.profit)
				.collect(Collectors.toList()).toArray(new IntVar[] {});
		return Search.inputOrderUBSearch(vars);
	}

	/**
	 * set the mode of webs to the lowest power use
	 *
	 * @param scheduler
	 * @return
	 */
	public static IntStrategy webLowPower(AppScheduler scheduler) {

		return Search.inputOrderLBSearch(scheduler.webModes.values().stream().flatMap(List::stream).map(wsc -> wsc.power)
				.collect(Collectors.toList()).toArray(new IntVar[] {}));
	}

	/**
	 * all intVar are set to the highest value.
	 *
	 * @param scheduler
	 * @return
	 */
	public static IntStrategy defaultVariables(AppScheduler scheduler) {
		Stream<IntVar> vars = Stream.empty();
		vars = Stream.concat(vars, Stream.of(scheduler.getVars()).filter(v -> v instanceof IntVar).map(v -> (IntVar) v));
		return Search.inputOrderUBSearch(vars.collect(Collectors.toList()).toArray(new IntVar[] {}));
	}

	@SuppressWarnings("unchecked")
	public static final Function<Objective, Function<AppScheduler, AbstractStrategy<?>>[]> STRATEGY_HIGHPROFIT = o -> {
		Function<AppScheduler, AbstractStrategy<?>> ret = sc -> new StrategiesSequencer(
				HeuristicsMaker.webHighestProfitFirst(sc),
				HeuristicsMaker.defaultVariables(sc));
		return new Function[] { ret };
	};

}
