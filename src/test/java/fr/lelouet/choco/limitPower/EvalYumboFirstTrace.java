/**
 *
 */
package fr.lelouet.choco.limitPower;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingResult;
import fr.lelouet.choco.limitpower.heuristics.HeuristicsMaker;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;
import fr.lelouet.choco.limitpower.model.SchedulingProblem.Objective;
import fr.lelouet.choco.limitpower.model.parser.yumbo.DataLoader;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class EvalYumboFirstTrace {

	public static void main(String[] args) {
		System.err.println("pb\tsolve\ttime");
		for (int pbNb = 0; pbNb < 24; pbNb++) {
			SchedulingProblem p = new DataLoader().load(pbNb);
			DataLoader.injectLinearProfits(p, p.appNames(), 1.3);
			p.nbIntervals = 1;
			p.objective = Objective.PROFIT;
			// p.setResource("ram", null);
			long time = System.currentTimeMillis();
			AppScheduler s = new AppScheduler();
			s.withHeuristics(HeuristicsMaker.STRATEGY_HIGHPROFIT);
			SchedulingResult res = s.solve(p);
			System.err.println("" + pbNb + "\t" + (res != null) + "\t" + (1.0 * System.currentTimeMillis() - time) / 1000);
		}
	}

}
