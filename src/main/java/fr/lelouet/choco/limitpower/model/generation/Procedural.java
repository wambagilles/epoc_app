package fr.lelouet.choco.limitpower.model.generation;

import java.util.Random;

import fr.lelouet.choco.limitpower.SchedulingModel;

/**
 * Generate data procedurally from a seed
 *
 * @author Guillaume Le Louët
 *
 */

public class Procedural {

	/**
	 * add HPC tasks. if the model has web tasks, adds between 0 and 2 times this
	 * number of tasks. Else adds between 0.5 and 10* the number of servers. The
	 * ram used will be between 0 and 100% of remaining.
	 *
	 * @param model
	 * @param seed
	 */
	public static void addHPC(SchedulingModel model, long seed) {
		Random generator = new Random(seed);
		int minNb, maxNb;
		int nbwebs = model.nbWebs();
		if (nbwebs > 0) {
			minNb = 0;
			maxNb = 2 * nbwebs;
		} else {
			int nbservers = model.nbServers();
			minNb = nbservers / 2;
			maxNb = nbservers * 10;
		}
		int nbHPC = generator.nextInt(maxNb - minNb + 1) + minNb;

	}

}
