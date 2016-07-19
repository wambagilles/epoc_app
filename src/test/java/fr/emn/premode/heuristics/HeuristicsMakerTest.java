/**
 *
 */
package fr.emn.premode.heuristics;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.emn.premode.Scheduler;
import fr.emn.premode.center.SchedulingProblem;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class HeuristicsMakerTest {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HeuristicsMakerTest.class);

	@Test
	public void testSortHpcAppsByInterest() {
		// sort the hpc apps wrt their profit/(power*duration)
		SchedulingProblem pb = new SchedulingProblem();
		pb.addHPC("hpc0", 0, 2, 2, 5, -1);// interest is 5/4
		pb.addHPC("hpc1", 0, 2, 1, 5, -1);// one duration instead of 2=> interest is 5/2
		pb.addHPC("hpc2", 0, 2, 2, 2, -1);// profit 2 instead of 5=> interest is 2/4
		Scheduler sc = new Scheduler().withVars(pb);
		int[] indexes = HeuristicsHelper.sortHpcAppsByInterest(sc, n -> pb.getHPC(n).power);
		Assert.assertEquals(indexes[0], sc.app("hpc1"));
		Assert.assertEquals(indexes[1], sc.app("hpc0"));
		Assert.assertEquals(indexes[2], sc.app("hpc2"));
	}
}
