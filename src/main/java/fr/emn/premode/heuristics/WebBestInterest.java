package fr.emn.premode.heuristics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

import fr.emn.premode.Scheduler;
import fr.emn.premode.center.PowerMode;

/**
 * set the modes of the web apps to the highest profit. The web apps are first
 * ordered by their (max profit-min profit)/(max profit power - min profit
 * power), decreasing.
 */
public class WebBestInterest implements Heuristic {

	public static final WebBestInterest INSTANCE = new WebBestInterest();

	@Override
	public AbstractStrategy<?> makeStrat(Scheduler sc) {
		// map the app names to their interest.
		HashMap<String, Integer> appInterest = new HashMap<>();
		sc.getSource().webNames().forEach(n -> {
			int minprofit = Integer.MAX_VALUE, maxProfit = Integer.MIN_VALUE, minProfitPower = 0, maxProfitPower = 0;
			for (PowerMode m : sc.getSource().getWebPowerModes(n)) {
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

		return Search.inputOrderUBSearch(Stream.of(arr).map(e -> sc.webModes.get(e.getKey())).flatMap(List::stream)
				.map(wsc -> wsc.profit).collect(Collectors.toList()).toArray(new IntVar[] {}));
	}

}
