/**
 *
 */
package fr.emn.premode.objectives;

import org.chocosolver.solver.variables.IntVar;

import fr.emn.premode.Scheduler;
import fr.emn.premode.heuristics.Heuristic;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class Profit implements Objective {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Profit.class);

	public static final Profit INSTANCE = new Profit();

	final Heuristic[] strategies;

	/**
	 *
	 */
	public Profit() {
		this((Heuristic[]) null);
	}

	public Profit(Heuristic... strategies) {
		this.strategies = strategies;
	}

	@Override
	public IntVar getObjective(Scheduler scheduler) {
		return scheduler.getProfit();
	}

	@Override
	public Heuristic[] getStrategies() {
		return null;
	}
}
