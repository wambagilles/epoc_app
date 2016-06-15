/**
 *
 */
package fr.lelouet.choco.limitPower;

import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingModel;
import fr.lelouet.choco.limitpower.SchedulingResult;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
public class TestBigProblem {

	@Test
	public void firstTest() {
		SchedulingModel model = new SchedulingModel();
		model.nbIntervals = 1;

		// 3 classes of servers, 5 servers each.
		IntStream.rangeClosed(0, 5).forEach(i -> {
			model.server("sa" + i).maxPower = 40;
			model.server("sb" + i).maxPower = 30;
			model.server("sc" + i).maxPower = 20;
		});
		// 3 classes of web app, 50 app each.
		IntStream.rangeClosed(0, 39).forEach(i -> {
			model.addWeb("wa" + i, 1, 1);
			model.addWeb("wb" + i, 1, 1);
			model.addWeb("wc" + i, 1, 1);
		});

		ToIntFunction<String> ram = name -> {
			if (name.startsWith("sa")) {
				return 40;
			}
			if (name.startsWith("sb")) {
				return 30;
			}
			if (name.startsWith("sc")) {
				return 20;
			}
			if (name.startsWith("wa")) {
				return 4;
			}
			if (name.startsWith("wb")) {
				return 3;
			}
			if (name.startsWith("wc")) {
				return 2;
			}
			throw new UnsupportedOperationException();
		};

		model.setResource("RAM", ram);
		AppScheduler as = new AppScheduler();
		// as.withDebug(true);

		SchedulingResult r = as.solve(model);

		Assert.assertNotNull(r);
		System.err.println(r);
	}

}
