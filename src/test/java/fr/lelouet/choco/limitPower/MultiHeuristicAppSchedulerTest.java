/**
 *
 */
package fr.lelouet.choco.limitPower;

import java.util.stream.IntStream;

import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingResult;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class MultiHeuristicAppSchedulerTest {

	@Test
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

		SchedulingResult r = new AppScheduler().solve(pb);
		System.err.println(r);
	}

}
