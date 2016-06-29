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

	public static void printData(double[][] data, int[] xLegend, double[] yLegend) {
		assert yLegend.length == data.length;
		System.err.print(" ");
		for (int x : xLegend) {
			System.err.print("\t" + x);
		}
		System.err.println();
		for (int yi = 0; yi < yLegend.length; yi++) {
			assert data[yi].length == xLegend.length;
			System.err.print(yLegend[yi]);
			for (int xi = 0; xi < xLegend.length; xi++) {
				System.err.print("\t" + data[yi][xi]);
			}
			System.err.println();
		}

	}

	public static void main(String[] args) {
		System.err.println("pb\tsolve\ttime");
		double[] multipliers = { 1.3, 1.5, 1.7, 1.8, 1.9 };
		int[] intervals = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };
		double[][] values = new double[multipliers.length][intervals.length];
		for (int multIdx = 0; multIdx < multipliers.length; multIdx++) {
			for (int itvIdx = 0; itvIdx < 24; itvIdx++) {
				SchedulingProblem p = new DataLoader().load(itvIdx);
				DataLoader.injectLinearProfits(p, p.appNames(), multipliers[multIdx]);
				p.nbIntervals = 1;
				p.objective = Objective.PROFIT;
				// p.setResource("ram", null);
				long time = System.currentTimeMillis();
				AppScheduler s = new AppScheduler();
				s.withHeuristics(HeuristicsMaker.STRATEGY_HIGHPROFITREMAINRAM).withTimeLimit(60 * 2 * 1000);// limit search to
																																																		// 5min
				SchedulingResult res = s.solve(p);
				values[multIdx][itvIdx] = res!=null?(1.0 * System.currentTimeMillis() - time) / 1000:-1;
				EvalYumboFirstTrace.printData(values, intervals, multipliers);
			}
		}
	}

}
