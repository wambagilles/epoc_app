/**
 *
 */
package fr.lelouet.choco.limitPower;

import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.heuristics.HeuristicsMaker;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;
import fr.lelouet.choco.limitpower.model.SchedulingProblem.Objective;
import fr.lelouet.choco.limitpower.model.parser.yumbo.DataLoader;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class EvalYumboFirstTrace {

	@Test
	public void eval() {
		SchedulingProblem p = new DataLoader().load(0);
		DataLoader.injectLinearProfits(p, p.appNames(), 1.8);
		p.nbIntervals = 1;
		p.objective = Objective.PROFIT;
// p.setResource("ram", null);
		long time = System.currentTimeMillis();
		AppScheduler s = new AppScheduler();
		s.withHeuristics(HeuristicsMaker.STRATEGY_HIGHPROFIT);
		System.err.println(s.solve(p));
		System.err.println("solve time " + (1.0 * System.currentTimeMillis() - time) / 1000);

	}

}
