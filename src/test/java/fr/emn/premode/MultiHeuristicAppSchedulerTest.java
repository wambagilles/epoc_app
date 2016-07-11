/**
 *
 */
package fr.emn.premode;

import java.util.stream.IntStream;

import fr.emn.premode.Scheduler;
import fr.emn.premode.Planning;
import fr.emn.premode.center.SchedulingProblem;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class MultiHeuristicAppSchedulerTest {

	public void test() {
		SchedulingProblem pb = new SchedulingProblem();
		pb.nbIntervals = 2;
		pb.setMigrateCost(n -> 1);
		int appPerServer = 5;
		int serverPwr = 100;
		int appMaxPwr = serverPwr / appPerServer;
		IntStream.range(0, 5).forEach(si -> {
			pb.server("s" + si).maxPower = serverPwr;
			// also several web apps
			IntStream.range(0, appPerServer).forEach(vi -> {
				pb.addWeb("v" + (si * appPerServer + vi), appMaxPwr, (si + 3) * 10);
				pb.addWeb("v" + (si * appPerServer + vi), appMaxPwr / 2, (si + 3) * 6);
			});
		});

		Planning r = new Scheduler().solve(pb);
		System.err.println(r);
	}

}
