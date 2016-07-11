/**
 *
 */
package fr.emn.premode.objectives;

import org.chocosolver.solver.variables.IntVar;

import fr.emn.premode.Scheduler;
import fr.emn.premode.heuristics.Heuristic;

/** the objective to solve this problem */
public interface Objective {

	public IntVar getObjective(Scheduler scheduler);

	public Heuristic[] getStrategies();

	public static final Profit PROFIT = Profit.INSTANCE;

	public static final ProfitOnSchedule PROFITONSCHEDULE = ProfitOnSchedule.INSTANCE;

	public static final ProfitPower PROFITPOWER = ProfitPower.INSTANCE;

	public static final Objective[] DEFAULTS = { Objective.PROFIT, Objective.PROFITONSCHEDULE, Objective.PROFITPOWER };

}