/**
 *
 */
package fr.emn.premode;

import java.util.stream.IntStream;

import fr.emn.premode.Scheduler;
import fr.emn.premode.Planning;
import fr.emn.premode.center.SchedulingProblem;
import fr.emn.premode.center.parser.yumbo.YumboDecoration;
import fr.emn.premode.heuristics.HeuristicsMaker;
import fr.emn.premode.objectives.Profit;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class EvalYumboFirstTrace {

	public static void printData(String[][] data, int[] xLegend, String[] yLegend) {
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
		double[] multipliers1 = { 1.1, 1.3, 1.5 };
		double[] multipliers2 = { 1.7, 1.9, 2.1 };
		String[] colLegend = new String[multipliers1.length * multipliers2.length];
		for (int multIdx1 = 0; multIdx1 < multipliers1.length; multIdx1++) {
			for (int multIdx2 = 0; multIdx2 < multipliers2.length; multIdx2++) {
				colLegend[multIdx1 * multipliers2.length + multIdx2] = "" + multipliers1[multIdx1] + ";"
						+ multipliers2[multIdx2];
			}
		}

		int[] intervals = IntStream.rangeClosed(0, 10).toArray();

		String[][] values = new String[multipliers1.length * multipliers2.length][intervals.length];

		YumboDecoration generator = new YumboDecoration();
		generator.bAddHPC = true;

		for (int multIdx1 = 0; multIdx1 < multipliers1.length; multIdx1++) {
			for (int multIdx2 = 0; multIdx2 < multipliers2.length; multIdx2++) {
				int row = multIdx1*multipliers2.length+multIdx2;
				for (int itvIdx = 0; itvIdx < intervals.length; itvIdx++) {
					int itv = intervals[itvIdx];
					generator.profitMults = new double[] { multipliers1[multIdx1], multipliers2[multIdx2] };
					SchedulingProblem p = generator.load(itv);
					// p.setResource("ram", null);
					long time = System.currentTimeMillis();
					Scheduler s = new Scheduler();
					p.objective = new Profit(HeuristicsMaker.STRATEGY_HIGHPROFITREMAINRAM);
					s.withTimeLimit(60 * 2 * 1000);// limit search to
					// 5min
					Planning res = s.solve(p);
					long ttime = System.currentTimeMillis() - time;
					values[row][itvIdx] = res != null ? "" + 1.0 * ttime / 1000 + "(" + 100 * res.searchMS / ttime + "%)" : "NaN";
					EvalYumboFirstTrace.printData(values, intervals, colLegend);
				}
			}
		}
	}

}
