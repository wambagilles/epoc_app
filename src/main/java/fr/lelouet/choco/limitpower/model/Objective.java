/**
 *
 */
package fr.lelouet.choco.limitpower.model;

import org.chocosolver.solver.variables.IntVar;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.model.objectives.Profit;
import fr.lelouet.choco.limitpower.model.objectives.ProfitOnSchedule;
import fr.lelouet.choco.limitpower.model.objectives.ProfitPower;

/** the objective to solve this problem */
public interface Objective {

	public IntVar getObjective(AppScheduler scheduler);

	public Heuristic[] getStrategies();

	public static final Profit PROFIT = Profit.INSTANCE;

	public static final ProfitOnSchedule PROFITONSCHEDULE = ProfitOnSchedule.INSTANCE;

	public static final ProfitPower PROFITPOWER = ProfitPower.INSTANCE;

	public static final Objective[] DEFAULTS = { Objective.PROFIT, Objective.PROFITONSCHEDULE, Objective.PROFITPOWER };

}